# MCP Integration for Lineage

This document describes how to use the Model Context Protocol (MCP) integration to allow AI models to interact with your Lineage requirements management system.

## Overview

The MCP server allows AI models to:
- Parse plain text or transcribed audio into structured requirements
- Create requirements in your projects
- List existing projects and requirements
- Create traceability links between requirements

This enables workflows like:
1. Transcribe a customer interview
2. Have an AI model parse it into requirements
3. Automatically create and link requirements in your project

## Architecture

- **WebSocket Endpoint**: `ws://localhost:8080/mcp`
- **Authentication**: JWT token via query parameter or Authorization header
- **Protocol**: Model Context Protocol (MCP) 2024-11-05

## Available Tools

### 1. `parse_requirements`
Parses plain text or transcribed audio into structured requirements.

**Input:**
```json
{
  "text": "The system must authenticate users. High priority. The app should support dark mode.",
  "context": "Mobile app customer requirements"
}
```

**Output:**
```json
{
  "success": true,
  "requirements": [
    {
      "title": "The system must authenticate users",
      "description": "The system must authenticate users.",
      "priority": "HIGH",
      "status": "DRAFT"
    },
    {
      "title": "The app should support dark mode",
      "description": "The app should support dark mode.",
      "priority": "MEDIUM",
      "status": "DRAFT"
    }
  ],
  "count": 2
}
```

### 2. `list_projects`
Lists all projects accessible to the authenticated user.

**Input:** None required

**Output:**
```json
{
  "success": true,
  "projects": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Mobile App",
      "description": "Customer mobile application",
      "role": "EDITOR"
    }
  ],
  "count": 1
}
```

### 3. `list_requirements`
Lists all requirements in a project.

**Input:**
```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Output:**
```json
{
  "success": true,
  "requirements": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440000",
      "reqId": "CR-001",
      "title": "User Authentication",
      "status": "APPROVED",
      "priority": "HIGH",
      "level": 1,
      "parentId": ""
    }
  ],
  "count": 1
}
```

### 4. `create_requirement`
Creates a new requirement in a project.

**Input:**
```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "User Authentication",
  "description": "The system must authenticate users using email and password.",
  "status": "DRAFT",
  "priority": "HIGH",
  "parentId": null
}
```

**Output:**
```json
{
  "success": true,
  "requirementId": "660e8400-e29b-41d4-a716-446655440000",
  "reqId": "CR-001",
  "message": "Requirement created successfully: CR-001"
}
```

### 5. `create_link`
Creates a traceability link between two requirements.

**Input:**
```json
{
  "fromRequirementId": "660e8400-e29b-41d4-a716-446655440000",
  "toRequirementId": "770e8400-e29b-41d4-a716-446655440000"
}
```

**Output:**
```json
{
  "success": true,
  "linkId": "880e8400-e29b-41d4-a716-446655440000",
  "message": "Link created successfully between requirements"
}
```

## Usage Examples

### Example 1: Parse Customer Interview Transcript

```javascript
// 1. Connect to MCP server with JWT token
const ws = new WebSocket('ws://localhost:8080/mcp?token=YOUR_JWT_TOKEN');

// 2. Parse transcript
const parseRequest = {
  jsonrpc: "2.0",
  id: 1,
  method: "tools/call",
  params: {
    name: "parse_requirements",
    arguments: {
      text: `
        Customer Interview Notes:
        - Must have secure login with 2FA
        - Should support biometric authentication
        - Need password reset functionality
        - Critical: Must comply with GDPR
      `,
      context: "Customer requirements from interview 2025-01-15"
    }
  }
};

ws.send(JSON.stringify(parseRequest));
```

### Example 2: Automated Requirement Creation Workflow

```python
import websocket
import json

# Connect with authentication
ws = websocket.create_connection('ws://localhost:8080/mcp?token=YOUR_JWT_TOKEN')

# Step 1: List projects
list_projects = {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
        "name": "list_projects",
        "arguments": {}
    }
}
ws.send(json.dumps(list_projects))
response = json.loads(ws.recv())
project_id = response['result']['content'][0]['text']['projects'][0]['id']

# Step 2: Parse requirements from text
parse_req = {
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
        "name": "parse_requirements",
        "arguments": {
            "text": "The system shall authenticate users. Must support OAuth2.",
            "context": "Authentication requirements"
        }
    }
}
ws.send(json.dumps(parse_req))
response = json.loads(ws.recv())
parsed_reqs = json.loads(response['result']['content'][0]['text'])['requirements']

# Step 3: Create each requirement
for req in parsed_reqs:
    create_req = {
        "jsonrpc": "2.0",
        "id": 3,
        "method": "tools/call",
        "params": {
            "name": "create_requirement",
            "arguments": {
                "projectId": project_id,
                "title": req['title'],
                "description": req['description'],
                "priority": req['priority'],
                "status": req['status']
            }
        }
    }
    ws.send(json.dumps(create_req))
    response = json.loads(ws.recv())
    print(f"Created: {response}")

ws.close()
```

### Example 3: Using with Claude Desktop

Add to your Claude Desktop MCP configuration (`~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "lineage": {
      "command": "node",
      "args": ["/path/to/lineage-mcp-client.js"],
      "env": {
        "LINEAGE_URL": "ws://localhost:8080/mcp",
        "LINEAGE_TOKEN": "your-jwt-token"
      }
    }
  }
}
```

Then in Claude:
> "I have these customer requirements from a call transcript. Can you parse them and add them to my Mobile App project in Lineage?"

## Authentication

### Getting a JWT Token

1. Login via the API:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@lineage.local", "password": "admin123"}'
```

2. Extract the token from the response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "admin@lineage.local"
}
```

3. Use the token in WebSocket connection:
```
ws://localhost:8080/mcp?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Or via header:
```javascript
const ws = new WebSocket('ws://localhost:8080/mcp', {
  headers: {
    'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  }
});
```

## Security Considerations

- **Authentication Required**: All MCP connections must authenticate with a valid JWT token
- **Authorization**: Users can only access projects they have permissions for
- **Token Expiration**: JWT tokens expire based on your security configuration
- **CORS**: WebSocket endpoint allows all origins by default - restrict in production

## Limitations

- WebSocket connections timeout after inactivity (configure via Spring Boot)
- Large transcript parsing may take several seconds
- Maximum message size is limited by Spring Boot WebSocket configuration

## Troubleshooting

### Connection Refused
- Ensure Spring Boot application is running
- Check JWT token is valid and not expired
- Verify WebSocket port is not blocked by firewall

### Authentication Failed
- Verify JWT token format (should start with "eyJ")
- Check token hasn't expired
- Ensure user account is active

### Tool Execution Errors
- Verify project IDs are valid UUIDs
- Check user has appropriate role (EDITOR or ADMIN for creating requirements)
- Ensure requirement IDs exist when creating links

## Development

The MCP integration consists of:

- `McpServer.java` - WebSocket handler and protocol implementation
- `McpTool.java` - Interface for MCP tools
- `tools/` - Individual tool implementations
- `McpWebSocketConfig.java` - WebSocket and authentication configuration
- `McpToolsConfig.java` - Tool registration

To add new tools:
1. Create a class implementing `McpTool` interface
2. Annotate with `@Component("toolName")`
3. Implement `getName()`, `getDescription()`, `getInputSchema()`, and `execute()`
4. Tool will be automatically registered and available

## Future Enhancements

- Batch requirement creation
- Requirement validation and duplicate detection
- Integration with speech-to-text services
- Support for ReqIF import via MCP
- Automatic link suggestion based on content similarity
