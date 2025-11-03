# Lineage MCP Examples

Examples and tools for using the Lineage MCP (Model Context Protocol) server.

## Prerequisites

1. **Lineage Server Running**
   ```bash
   cd ..
   ./gradlew bootRun
   ```

2. **LM Studio Running** (for AI-powered parsing)
   - Download from: https://lmstudio.ai/
   - Load a model (recommended: qwen3-thinking-2507 or qwen2.5-14b-instruct)
   - Start the local server (default: http://127.0.0.1:1234)

3. **Node.js Installed** (v18 or higher)

## Installation

```bash
npm install
```

## Getting Your JWT Token

### Linux/Mac:
```bash
chmod +x get-token.sh
./get-token.sh
```

### Windows:
```cmd
get-token.bat
```

Or manually:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@lineage.local", "password": "admin123"}'
```

Copy the token from the response.

## Usage

### 1. Basic MCP Client (No AI)

Test the MCP connection and manually create requirements:

```bash
export LINEAGE_JWT_TOKEN="your-token-here"
node mcp-client-example.js
```

### 2. AI-Powered Requirements Parser

#### Interactive Mode

Paste requirements text and let AI parse them:

```bash
export LINEAGE_JWT_TOKEN="your-token-here"
node lineage-mcp-ai-client.js
```

Then paste your requirements:
```
The system must authenticate users. High priority.
Should support OAuth2 integration.
Password reset via email is needed.
```

Press Ctrl+D (Mac/Linux) or Ctrl+Z (Windows) when done.

#### File Mode

Parse requirements from a file:

```bash
export LINEAGE_JWT_TOKEN="your-token-here"
node lineage-mcp-ai-client.js sample-requirements.txt
```

### 3. Batch Import

Import many requirements at once:

```bash
export LINEAGE_JWT_TOKEN="your-token-here"
python mcp-batch-import.py your-token-here project-id requirements-file.txt
```

## Configuration

### Environment Variables

- `LINEAGE_JWT_TOKEN` - Your authentication token (required)
- `LINEAGE_MCP_URL` - MCP WebSocket URL (default: ws://localhost:8080/mcp)
- `LINEAGE_PROJECT_ID` - Target project UUID (uses first project if not set)
- `LLM_API` - LM Studio API URL (default: http://127.0.0.1:1234/v1/chat/completions)
- `LLM_MODEL` - Model name in LM Studio (default: local-model)

### Example with All Options

```bash
export LINEAGE_JWT_TOKEN="eyJhbGc..."
export LINEAGE_PROJECT_ID="550e8400-e29b-41d4-a716-446655440000"
export LLM_MODEL="qwen3-thinking-2507"
node lineage-mcp-ai-client.js customer-interview.txt
```

## Examples

### Example 1: Customer Interview

Create `customer-interview.txt`:
```
From customer call on 2025-01-15:

They need secure user authentication with 2FA.
High priority: Must integrate with their existing LDAP directory.
Password complexity requirements are critical for compliance.
Users should be able to reset passwords without calling helpdesk.
Need activity logging for all login attempts.
```

Run:
```bash
node lineage-mcp-ai-client.js customer-interview.txt
```

### Example 2: Audio Transcript

After transcribing an audio meeting:
```
Meeting transcript 2025-01-15:

John: We absolutely need dark mode support.
Sarah: Agreed, and it should remember the user's preference.
Mike: What about accessibility? Screen reader support is critical.
Sarah: Good point. WCAG 2.1 AA compliance is required.
John: Can we also add keyboard shortcuts for power users?
```

The AI will parse this into structured requirements with appropriate priorities.

## Troubleshooting

### "LINEAGE_JWT_TOKEN environment variable not set"

Run `get-token.sh` or `get-token.bat` to get your token, then:
```bash
export LINEAGE_JWT_TOKEN="your-token"  # Mac/Linux
set LINEAGE_JWT_TOKEN=your-token       # Windows
```

### "WebSocket error: Unexpected server response: 403"

Your JWT token expired or is invalid. Get a new one with `get-token.sh`.

### "LLM API error: fetch failed"

Make sure LM Studio is running and the server is started. Check the URL at http://127.0.0.1:1234.

### "LLM did not return valid JSON"

The model might need a better prompt or different temperature setting. Try:
- A different model (qwen models work best)
- Lower temperature (0.3-0.5)
- Check the system prompt in the client code

### "No projects found"

Create a project first via the web UI at http://localhost:5173 or via API:
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $LINEAGE_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "My Project", "projectKey": "MYPROJ", "description": "Test project"}'
```

## Available MCP Tools

The MCP server provides these tools:

1. **parse_requirements** - Parse text into structured requirements
2. **list_projects** - List all accessible projects
3. **list_requirements** - List requirements in a project
4. **create_requirement** - Create a new requirement
5. **create_link** - Create a link between requirements

See the test files for usage examples of each tool.

## Development

To add new features or modify the clients:

1. Edit the client files
2. Test with: `npm test`
3. Check the MCP server logs in the main Lineage application

For more information, see the main MCP integration documentation: `../MCP_INTEGRATION.md`
