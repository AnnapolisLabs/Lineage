#!/usr/bin/env node

/**
 * Example MCP client for Lineage
 * Demonstrates how to connect and use the MCP tools
 * 
 * Usage:
 *   node mcp-client-example.js <jwt-token>
 */

const WebSocket = require('ws');

if (process.argv.length < 3) {
    console.error('Usage: node mcp-client-example.js <jwt-token>');
    process.exit(1);
}

const JWT_TOKEN = process.argv[2];
const MCP_URL = `ws://localhost:8080/mcp?token=${JWT_TOKEN}`;

class LineageMcpClient {
    constructor(url) {
        this.url = url;
        this.requestId = 0;
        this.pendingRequests = new Map();
    }

    connect() {
        return new Promise((resolve, reject) => {
            this.ws = new WebSocket(this.url);
            
            this.ws.on('open', () => {
                console.log('✓ Connected to Lineage MCP server');
                resolve();
            });

            this.ws.on('message', (data) => {
                const response = JSON.parse(data.toString());
                
                if (response.method === 'server/info') {
                    console.log('✓ Server info received:', response.params);
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

            this.ws.on('error', reject);
        });
    }

    async callTool(toolName, args) {
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
        });
    }

    async listTools() {
        const id = ++this.requestId;
        
        const request = {
            jsonrpc: '2.0',
            id,
            method: 'tools/list'
        };

        return new Promise((resolve, reject) => {
            this.pendingRequests.set(id, { resolve, reject });
            this.ws.send(JSON.stringify(request));
        });
    }

    close() {
        this.ws.close();
    }
}

async function main() {
    const client = new LineageMcpClient(MCP_URL);
    
    try {
        // Connect
        await client.connect();

        // List available tools
        console.log('\n📋 Listing available tools...');
        const toolsResult = await client.listTools();
        console.log(`✓ Found ${toolsResult.tools.length} tools:`);
        toolsResult.tools.forEach(tool => {
            console.log(`  - ${tool.name}: ${tool.description}`);
        });

        // Example 1: List projects
        console.log('\n🏗️  Listing projects...');
        const projectsResponse = await client.callTool('list_projects', {});
        const projectsData = JSON.parse(projectsResponse.content[0].text);
        console.log(`✓ Found ${projectsData.count} projects`);
        projectsData.projects.forEach(p => {
            console.log(`  - ${p.name} (${p.id}) - Role: ${p.role}`);
        });

        if (projectsData.projects.length === 0) {
            console.log('⚠️  No projects found. Please create a project first.');
            return;
        }

        const projectId = projectsData.projects[0].id;

        // Example 2: Parse requirements from text
        console.log('\n📝 Parsing requirements from sample text...');
        const sampleText = `
Customer Requirements from Interview 2025-01-15:

1. The system must authenticate users with email and password. High priority.
2. Users should be able to reset their passwords via email.
3. The application must log all authentication attempts for security audit.
4. Critical: System must lock accounts after 5 failed login attempts.
5. Support for multi-factor authentication should be added in the future.
        `;

        const parseResponse = await client.callTool('parse_requirements', {
            text: sampleText,
            context: 'Authentication requirements from customer interview'
        });
        const parsedData = JSON.parse(parseResponse.content[0].text);
        console.log(`✓ Parsed ${parsedData.count} requirements:`);
        parsedData.requirements.forEach((req, idx) => {
            console.log(`  ${idx + 1}. [${req.priority}] ${req.title}`);
        });

        // Example 3: Create one of the parsed requirements
        console.log('\n✍️  Creating first requirement in project...');
        const firstReq = parsedData.requirements[0];
        const createResponse = await client.callTool('create_requirement', {
            projectId: projectId,
            title: firstReq.title,
            description: firstReq.description,
            priority: firstReq.priority,
            status: firstReq.status
        });
        const createData = JSON.parse(createResponse.content[0].text);
        console.log(`✓ ${createData.message}`);
        console.log(`  ID: ${createData.requirementId}`);

        // Example 4: List requirements in project
        console.log('\n📄 Listing requirements in project...');
        const reqsResponse = await client.callTool('list_requirements', {
            projectId: projectId
        });
        const reqsData = JSON.parse(reqsResponse.content[0].text);
        console.log(`✓ Found ${reqsData.count} requirements:`);
        reqsData.requirements.forEach(req => {
            console.log(`  - ${req.reqId}: ${req.title} [${req.status}]`);
        });

        console.log('\n✅ All examples completed successfully!');

    } catch (error) {
        console.error('❌ Error:', error.message);
        process.exit(1);
    } finally {
        client.close();
    }
}

main();
