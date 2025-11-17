#!/usr/bin/env node

/**
 * AI-Powered MCP Client for Lineage
 * Connects LM Studio LLM to Lineage MCP server for intelligent requirement parsing
 */

const WebSocket = require('ws');
const readline = require('readline');
const fs = require('fs');

// Configuration
const CONFIG = {
    MCP_URL: process.env.LINEAGE_MCP_URL || 'ws://localhost:8080/mcp',
    JWT_TOKEN: process.env.LINEAGE_JWT_TOKEN,
    LLM_API: process.env.LLM_API || 'http://127.0.0.1:1234/v1/chat/completions',
    LLM_MODEL: process.env.LLM_MODEL || 'local-model',
    PROJECT_ID: process.env.LINEAGE_PROJECT_ID
};

// System prompt for the LLM
const SYSTEM_PROMPT = `You are a requirements parser. Extract structured requirements from text and output ONLY valid JSON (no markdown, no explanations).

# Output Format
{
  "requirements": [
    {
      "title": "Brief requirement statement (50-100 chars)",
      "description": "Detailed explanation",
      "priority": "CRITICAL|HIGH|MEDIUM|LOW",
      "status": "DRAFT"
    }
  ]
}

# Priority Rules
- CRITICAL: Security, legal/compliance, system failures
- HIGH: Core functionality, "must" statements
- MEDIUM: Important features, "should" statements
- LOW: Nice-to-have, future enhancements

# Extraction Rules
1. One requirement per item - split compound statements
2. Remove priority keywords from descriptions
3. Default: status=DRAFT, priority=MEDIUM
4. Modal verbs: "must"→HIGH, "should"→MEDIUM, "critical"→CRITICAL
5. Be specific, measurable, actionable

# Quality Standards
- Specific: Clear, unambiguous
- Measurable: Testable outcome
- Actionable: Can be implemented
- Singular: One requirement per item
- Traceable: Note relationships

# Example
Input: "The system must authenticate users. High priority. We should support OAuth2 and password reset via email."

Output:
{
  "requirements": [
    {
      "title": "User Authentication",
      "description": "The system must provide secure user authentication functionality.",
      "priority": "HIGH",
      "status": "DRAFT"
    },
    {
      "title": "OAuth2 Authentication Support",
      "description": "Support OAuth2 authentication protocol for third-party login providers.",
      "priority": "MEDIUM",
      "status": "DRAFT"
    },
    {
      "title": "Password Reset via Email",
      "description": "Enable password reset through email verification.",
      "priority": "MEDIUM",
      "status": "DRAFT"
    }
  ]
}

Output ONLY the JSON object. No markdown blocks, no extra text.`;

class LineageAIClient {
    constructor() {
        this.ws = null;
        this.requestId = 0;
        this.pendingRequests = new Map();
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
            
            // Timeout after 30 seconds
            setTimeout(() => {
                if (this.pendingRequests.has(id)) {
                    this.pendingRequests.delete(id);
                    reject(new Error('Request timeout'));
                }
            }, 30000);
        });
    }

    async callLLM(userMessage) {
        console.log('\n🤖 Asking LLM to parse requirements...');
        
        const response = await fetch(CONFIG.LLM_API, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                model: CONFIG.LLM_MODEL,
                messages: [
                    { role: 'system', content: SYSTEM_PROMPT },
                    { role: 'user', content: userMessage }
                ],
                temperature: 0.5,
                max_tokens: 10000
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`LLM API error: ${response.statusText} - ${errorText}`);
        }

        const data = await response.json();
        const content = data.choices[0].message.content;

        // Parse JSON response (handle markdown code blocks and thinking tags)
        let jsonText = content.trim();

        // Remove markdown code blocks
        if (jsonText.startsWith('```json')) {
            jsonText = jsonText.replace(/```json\n?/g, '').replace(/```\n?$/g, '').trim();
        } else if (jsonText.startsWith('```')) {
            jsonText = jsonText.replace(/```\n?/g, '').trim();
        }

        // Remove thinking/system-reminder tags
        jsonText = jsonText.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
        jsonText = jsonText.replace(/<system-reminder>[\s\S]*?<\/system-reminder>/g, '').trim();

        // Try to extract JSON object if there's extra text
        const jsonMatch = jsonText.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
            jsonText = jsonMatch[0];
        }

        try {
            return JSON.parse(jsonText);
        } catch (e) {
            console.error('Failed to parse LLM response as JSON:', jsonText);
            throw new Error('LLM did not return valid JSON');
        }
    }

    async parseAndCreateRequirements(text, projectId) {
        // Step 0: Check if user specified a parent requirement
        const parentRef = this.extractParentReference(text);
        let parentId = null;

        if (parentRef) {
            console.log(`\n🔍 Looking for parent requirement: "${parentRef}"`);

            // Fetch existing requirements
            const existingReqs = await this.listRequirements(projectId);
            const requirements = existingReqs.requirements || [];

            if (requirements.length > 0) {
                const parent = this.findParentRequirement(parentRef, requirements);
                if (parent) {
                    parentId = parent.id;
                    console.log(`✓ Found parent: ${parent.title} (${parent.reqId})\n`);
                } else {
                    console.log(`⚠ No match found for "${parentRef}" - creating as top-level requirement\n`);
                }
            } else {
                console.log(`⚠ No existing requirements - creating as top-level requirement\n`);
            }
        }

        // Step 1: Use LLM to parse requirements
        const parsed = await this.callLLM(text);

        if (!parsed.requirements || !Array.isArray(parsed.requirements)) {
            throw new Error('Invalid LLM response format - missing requirements array');
        }

        console.log(`✓ LLM parsed ${parsed.requirements.length} requirements\n`);

        // Step 2: Create each requirement in Lineage
        const created = [];

        for (let i = 0; i < parsed.requirements.length; i++) {
            const req = parsed.requirements[i];

            console.log(`Creating ${i + 1}/${parsed.requirements.length}: ${req.title}`);

            try {
                const createArgs = {
                    projectId: projectId,
                    title: req.title,
                    description: req.description,
                    priority: req.priority || 'MEDIUM',
                    status: req.status || 'DRAFT'
                };

                // Add parentId if we found a parent
                if (parentId) {
                    createArgs.parentId = parentId;
                }

                const result = await this.callMcpTool('create_requirement', createArgs);

                const resultData = JSON.parse(result.content[0].text);
                created.push(resultData);
                console.log(`  ✓ Created: ${resultData.reqId}${parentId ? ' (child)' : ''}`);
            } catch (error) {
                console.error(`  ✗ Failed: ${error.message}`);
            }
        }

        return created;
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

    async listProjects() {
        const result = await this.callMcpTool('list_projects', {});
        return JSON.parse(result.content[0].text);
    }

    async listRequirements(projectId) {
        const result = await this.callMcpTool('list_requirements', { projectId });
        return JSON.parse(result.content[0].text);
    }

    /**
     * Fuzzy match a search term against requirement titles
     * Returns the best matching requirement or null
     */
    findParentRequirement(searchTerm, requirements) {
        if (!searchTerm || !requirements || requirements.length === 0) {
            return null;
        }

        const normalizedSearch = searchTerm.toLowerCase().trim();

        // Exact match first
        let match = requirements.find(req =>
            req.title.toLowerCase() === normalizedSearch
        );
        if (match) return match;

        // Partial match - search term contained in title
        match = requirements.find(req =>
            req.title.toLowerCase().includes(normalizedSearch)
        );
        if (match) return match;

        // Reverse - title contained in search term
        match = requirements.find(req =>
            normalizedSearch.includes(req.title.toLowerCase())
        );
        if (match) return match;

        // Fuzzy match - remove common words and check similarity
        const commonWords = ['the', 'a', 'an', 'for', 'to', 'of', 'in', 'on'];
        const cleanSearch = normalizedSearch.split(/\s+/)
            .filter(word => !commonWords.includes(word))
            .join(' ');

        match = requirements.find(req => {
            const cleanTitle = req.title.toLowerCase().split(/\s+/)
                .filter(word => !commonWords.includes(word))
                .join(' ');

            return cleanTitle.includes(cleanSearch) || cleanSearch.includes(cleanTitle);
        });

        return match || null;
    }

    /**
     * Extract parent reference from user input
     * Patterns: "under X", "child of X", "sub-requirement of X", "L2 under X", "beneath X"
     */
    extractParentReference(text) {
        const patterns = [
            /(?:under|beneath)\s+([^,.;]+)/i,
            /(?:child|sub-requirement)\s+of\s+([^,.;]+)/i,
            /L\d+\s+(?:under|beneath)\s+([^,.;]+)/i,
            /parent:?\s*([^,.;]+)/i
        ];

        for (const pattern of patterns) {
            const match = text.match(pattern);
            if (match) {
                return match[1].trim();
            }
        }

        return null;
    }

    close() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

// Interactive Mode
async function interactiveMode() {
    const client = new LineageAIClient();
    
    try {
        await client.connect();

        // List available tools
        console.log('\n🔧 Listing available tools...');
        const toolsList = await client.listTools();
        const tools = toolsList.tools || (toolsList.result && toolsList.result.tools);
        if (tools) {
            console.log('Available tools:', tools.map(t => t.name).join(', '));
        } else {
            console.error('No tools registered on server!');
            console.error('Response:', JSON.stringify(toolsList, null, 2));
        }

        // Get projects
        console.log('\n📋 Fetching projects...');
        const projects = await client.listProjects();
        
        if (projects.projects.length === 0) {
            console.error('No projects found. Please create a project first.');
            process.exit(1);
        }

        console.log('\nAvailable projects:');
        projects.projects.forEach((p, i) => {
            console.log(`  ${i + 1}. ${p.name} (${p.id})`);
        });

        const projectId = CONFIG.PROJECT_ID || projects.projects[0].id;
        console.log(`\nUsing project: ${projectId}\n`);

        // Interactive input
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });

        console.log('━'.repeat(60));
        console.log('AI-Powered Requirements Parser');
        console.log('Enter requirements text (Ctrl+D or type "exit" when done)');
        console.log('━'.repeat(60));
        console.log();

        let inputText = '';

        rl.on('line', (line) => {
            if (line.trim().toLowerCase() === 'exit') {
                rl.close();
                return;
            }
            inputText += line + '\n';
        });

        rl.on('close', async () => {
            if (inputText.trim()) {
                console.log('\n' + '━'.repeat(60));
                try {
                    const created = await client.parseAndCreateRequirements(inputText, projectId);
                    
                    console.log('\n' + '━'.repeat(60));
                    console.log(`✅ Successfully created ${created.length} requirements!`);
                    console.log('━'.repeat(60));
                    
                    created.forEach((req) => {
                        console.log(`  • ${req.reqId}: ${req.message}`);
                    });
                } catch (error) {
                    console.error('\n❌ Error:', error.message);
                }
            }
            
            client.close();
            process.exit(0);
        });

    } catch (error) {
        console.error('❌ Fatal error:', error.message);
        client.close();
        process.exit(1);
    }
}

// File Mode
async function fileMode(filePath) {
    const client = new LineageAIClient();
    
    try {
        await client.connect();

        const projects = await client.listProjects();
        const projectId = CONFIG.PROJECT_ID || projects.projects[0].id;

        const text = fs.readFileSync(filePath, 'utf-8');
        console.log(`\n📄 Reading from: ${filePath}`);
        console.log(`📦 Target project: ${projectId}\n`);

        const created = await client.parseAndCreateRequirements(text, projectId);
        
        console.log(`\n✅ Successfully created ${created.length} requirements!`);
        created.forEach((req) => {
            console.log(`  • ${req.reqId}`);
        });

        client.close();
    } catch (error) {
        console.error('❌ Error:', error.message);
        client.close();
        process.exit(1);
    }
}

// Main
async function main() {
    // Validate configuration
    if (!CONFIG.JWT_TOKEN) {
        console.error('Error: LINEAGE_JWT_TOKEN environment variable not set');
        console.error('\nGet your token by running:');
        console.error('  ./get-token.sh');
        console.error('\nOr manually:');
        console.error('  curl -X POST http://localhost:8080/api/auth/login \\');
        console.error('    -H "Content-Type: application/json" \\');
        console.error('    -d \'{"email": "your-email", "password": "your-password"}\'');
        process.exit(1);
    }

    const args = process.argv.slice(2);
    
    if (args.length > 0 && args[0] !== '-') {
        // File mode
        await fileMode(args[0]);
    } else {
        // Interactive mode
        await interactiveMode();
    }
}

// Run
if (require.main === module) {
    main().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = { LineageAIClient };
