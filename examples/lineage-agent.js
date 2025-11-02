#!/usr/bin/env node

/**
 * Lineage AI Agent - Full conversational agent with MCP tool access
 * Handles multi-step requirement management tasks
 */

const WebSocket = require('ws');
const readline = require('readline');

// Configuration
const CONFIG = {
    MCP_URL: process.env.LINEAGE_MCP_URL || 'ws://localhost:8080/mcp',
    JWT_TOKEN: process.env.LINEAGE_JWT_TOKEN,
    LLM_API: process.env.LLM_API || 'http://127.0.0.1:1234/v1/chat/completions',
    LLM_MODEL: process.env.LLM_MODEL || 'local-model',
    PROJECT_ID: process.env.LINEAGE_PROJECT_ID
};

// Agent system prompt
const AGENT_SYSTEM_PROMPT = `You are an AI agent for Lineage Requirements Management System. You have access to MCP tools to manage requirements.

# Available Tools
You can call these MCP tools by responding with JSON in this format:
{
  "action": "tool_call",
  "tool": "tool_name",
  "arguments": { ... }
}

When you need to respond to the user (not call a tool), use:
{
  "action": "respond",
  "message": "Your response to the user"
}

# Tool Descriptions
- list_projects: List all projects (no arguments)
- list_requirements: List requirements in a project (projectId)
- create_requirement: Create a new requirement (projectId, title, description, priority, status, parentId)
- update_requirement: Update a requirement (requirementId, title?, description?, priority?, status?, parentId?)
- delete_requirement: Delete a requirement (requirementId)
- create_link: Create a link between requirements (fromRequirementId, toRequirementId)
- parse_requirements: Parse text into requirements (text, context)

# Guidelines
1. For multi-step tasks, execute one tool at a time
2. Always list requirements first before modifying them
3. Use fuzzy matching to find requirements by reqId or title
4. Confirm destructive operations (delete) with the user
5. Handle errors gracefully and explain what went wrong
6. Be concise but informative in responses

# Examples
User: "Delete REQ-010"
You: {"action": "tool_call", "tool": "list_requirements", "arguments": {"projectId": "..."}}
[after getting results with REQ-010's UUID]
You: {"action": "tool_call", "tool": "delete_requirement", "arguments": {"requirementId": "uuid-here"}}
You: {"action": "respond", "message": "Deleted REQ-010 successfully"}

User: "Move REQ-010 under MCP Interfacing"
You: {"action": "tool_call", "tool": "list_requirements", "arguments": {"projectId": "..."}}
[find REQ-010 and MCP Interfacing UUIDs]
You: {"action": "tool_call", "tool": "update_requirement", "arguments": {"requirementId": "req-010-uuid", "parentId": "mcp-interfacing-uuid"}}
You: {"action": "respond", "message": "Moved REQ-010 to be a child of MCP Interfacing"}

Output ONLY valid JSON. No extra text, no markdown blocks.`;

class LineageAgent {
    constructor() {
        this.ws = null;
        this.requestId = 0;
        this.pendingRequests = new Map();
        this.conversationHistory = [];
        this.availableTools = [];
        this.currentProjectId = null;
    }

    async connect() {
        const url = `${CONFIG.MCP_URL}?token=${CONFIG.JWT_TOKEN}`;

        return new Promise((resolve, reject) => {
            this.ws = new WebSocket(url);

            this.ws.on('open', () => {
                console.log('✓ Connected to Lineage MCP server');
                resolve();
            });

            this.ws.on('message', (data) => {
                const response = JSON.parse(data.toString());

                if (response.method === 'server/info') {
                    console.log('✓ Server:', response.params.name);
                    return;
                }

                if (response.id && this.pendingRequests.has(response.id)) {
                    const { resolve, reject } = this.pendingRequests.get(response.id);
                    this.pendingRequests.delete(response.id);

                    if (response.error) {
                        reject(new Error(response.error.message));
                    } else {
                        resolve(response.result);
                    }
                }
            });

            this.ws.on('error', (error) => {
                console.error('WebSocket error:', error.message);
                reject(error);
            });

            this.ws.on('close', () => {
                console.log('✗ Disconnected from MCP server');
            });
        });
    }

    async callMcpTool(toolName, args) {
        const id = ++this.requestId;

        const request = {
            jsonrpc: '2.0',
            id,
            method: 'tools/call',
            params: {
                name: toolName,
                arguments: args
            }
        };

        return new Promise((resolve, reject) => {
            this.pendingRequests.set(id, { resolve, reject });
            this.ws.send(JSON.stringify(request));

            setTimeout(() => {
                if (this.pendingRequests.has(id)) {
                    this.pendingRequests.delete(id);
                    reject(new Error('Request timeout'));
                }
            }, 30000);
        });
    }

    async listTools() {
        const id = ++this.requestId;

        const request = {
            jsonrpc: '2.0',
            id,
            method: 'tools/list',
            params: {}
        };

        return new Promise((resolve, reject) => {
            this.pendingRequests.set(id, { resolve, reject });
            this.ws.send(JSON.stringify(request));

            setTimeout(() => {
                if (this.pendingRequests.has(id)) {
                    this.pendingRequests.delete(id);
                    reject(new Error('Request timeout'));
                }
            }, 30000);
        });
    }

    async callLLM(userMessage) {
        // Add user message to history
        this.conversationHistory.push({ role: 'user', content: userMessage });

        const response = await fetch(CONFIG.LLM_API, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                model: CONFIG.LLM_MODEL,
                messages: [
                    { role: 'system', content: AGENT_SYSTEM_PROMPT },
                    ...this.conversationHistory
                ],
                temperature: 0.3,
                max_tokens: 2000
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`LLM API error: ${response.statusText} - ${errorText}`);
        }

        const data = await response.json();
        const content = data.choices[0].message.content;

        // Add assistant response to history
        this.conversationHistory.push({ role: 'assistant', content });

        return this.parseAgentResponse(content);
    }

    parseAgentResponse(content) {
        // Remove thinking/system-reminder tags
        let cleanContent = content.trim();
        cleanContent = cleanContent.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
        cleanContent = cleanContent.replace(/<system-reminder>[\s\S]*?<\/system-reminder>/g, '').trim();

        // Remove markdown code blocks
        if (cleanContent.startsWith('```json')) {
            cleanContent = cleanContent.replace(/```json\n?/g, '').replace(/```\n?$/g, '').trim();
        } else if (cleanContent.startsWith('```')) {
            cleanContent = cleanContent.replace(/```\n?/g, '').trim();
        }

        // Try to extract JSON object
        const jsonMatch = cleanContent.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
            cleanContent = jsonMatch[0];
        }

        try {
            return JSON.parse(cleanContent);
        } catch (e) {
            console.error('Failed to parse agent response:', cleanContent);
            throw new Error('Agent did not return valid JSON');
        }
    }

    async executeAgentAction(action) {
        if (action.action === 'respond') {
            console.log('\n💬 Agent:', action.message, '\n');
            return { type: 'response', done: false };
        }

        if (action.action === 'tool_call') {
            console.log(`\n🔧 Calling tool: ${action.tool}`);
            console.log(`   Arguments:`, JSON.stringify(action.arguments, null, 2));

            try {
                const result = await this.callMcpTool(action.tool, action.arguments);
                const resultData = JSON.parse(result.content[0].text);

                console.log(`   ✓ Result:`, JSON.stringify(resultData, null, 2));

                // Add tool result to conversation history
                const toolResultMessage = `Tool ${action.tool} returned: ${JSON.stringify(resultData)}`;
                this.conversationHistory.push({ role: 'user', content: toolResultMessage });

                return { type: 'tool_call', result: resultData, done: false };
            } catch (error) {
                console.error(`   ✗ Error:`, error.message);

                // Add error to conversation history
                const errorMessage = `Tool ${action.tool} failed with error: ${error.message}`;
                this.conversationHistory.push({ role: 'user', content: errorMessage });

                return { type: 'error', error: error.message, done: false };
            }
        }

        throw new Error('Unknown action type: ' + action.action);
    }

    async processUserInput(input) {
        console.log('\n🤖 Agent is thinking...\n');

        let iterationCount = 0;
        const maxIterations = 10; // Prevent infinite loops

        while (iterationCount < maxIterations) {
            const agentResponse = await this.callLLM(input);
            const result = await this.executeAgentAction(agentResponse);

            // If agent responded to user, we're done
            if (result.type === 'response') {
                break;
            }

            // For tool calls, let the agent process the result
            input = ''; // Clear input for subsequent iterations
            iterationCount++;
        }

        if (iterationCount >= maxIterations) {
            console.log('\n⚠ Agent reached maximum iteration limit\n');
        }
    }

    close() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

async function main() {
    // Validate configuration
    if (!CONFIG.JWT_TOKEN) {
        console.error('Error: LINEAGE_JWT_TOKEN environment variable not set');
        console.error('\nGet your token by running:');
        console.error('  ./get-token.bat (Windows) or ./get-token.sh (Linux/Mac)');
        process.exit(1);
    }

    const agent = new LineageAgent();

    try {
        await agent.connect();

        // List available tools
        const toolsList = await agent.listTools();
        const tools = toolsList.tools || (toolsList.result && toolsList.result.tools);
        if (tools) {
            agent.availableTools = tools;
            console.log('\n🔧 Available tools:', tools.map(t => t.name).join(', '));
        }

        // Get project
        const result = await agent.callMcpTool('list_projects', {});
        const projects = JSON.parse(result.content[0].text);

        if (projects.projects.length === 0) {
            console.error('\nNo projects found. Please create a project first.');
            process.exit(1);
        }

        agent.currentProjectId = CONFIG.PROJECT_ID || projects.projects[0].id;
        console.log(`\n📦 Using project: ${agent.currentProjectId}`);

        // Interactive conversation
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });

        console.log('\n' + '━'.repeat(60));
        console.log('Lineage AI Agent - Conversational Requirements Management');
        console.log('Type your request and press Enter. Type "exit" to quit.');
        console.log('━'.repeat(60));
        console.log();

        const askQuestion = () => {
            rl.question('You: ', async (input) => {
                const trimmed = input.trim();

                if (trimmed.toLowerCase() === 'exit') {
                    console.log('\nGoodbye!');
                    rl.close();
                    agent.close();
                    process.exit(0);
                    return;
                }

                if (!trimmed) {
                    askQuestion();
                    return;
                }

                try {
                    // Inject project ID into conversation context
                    const contextualInput = `[Current project ID: ${agent.currentProjectId}]\n\n${trimmed}`;
                    await agent.processUserInput(contextualInput);
                } catch (error) {
                    console.error('\n❌ Error:', error.message, '\n');
                }

                askQuestion();
            });
        };

        askQuestion();

    } catch (error) {
        console.error('❌ Fatal error:', error.message);
        agent.close();
        process.exit(1);
    }
}

// Run
if (require.main === module) {
    main().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = { LineageAgent };
