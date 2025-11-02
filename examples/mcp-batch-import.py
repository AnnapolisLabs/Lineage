#!/usr/bin/env python3

"""
Batch import requirements from a text file using Lineage MCP

This script demonstrates how to:
1. Read a requirements document
2. Parse it into structured requirements
3. Create all requirements in a Lineage project
4. Optionally create links between related requirements

Usage:
    python mcp-batch-import.py <jwt-token> <project-id> <requirements-file>

Example:
    python mcp-batch-import.py eyJhbGc... 550e8400-e29b-41d4... requirements.txt
"""

import sys
import json
import websocket
import uuid

def connect_mcp(token):
    """Connect to Lineage MCP server with authentication"""
    url = f'ws://localhost:8080/mcp?token={token}'
    ws = websocket.create_connection(url)
    
    # Read server info message
    welcome = ws.recv()
    print(f"✓ Connected to Lineage MCP server")
    
    return ws

def call_tool(ws, tool_name, arguments):
    """Call an MCP tool and return the result"""
    request = {
        "jsonrpc": "2.0",
        "id": str(uuid.uuid4()),
        "method": "tools/call",
        "params": {
            "name": tool_name,
            "arguments": arguments
        }
    }
    
    ws.send(json.dumps(request))
    response = json.loads(ws.recv())
    
    if 'error' in response:
        raise Exception(f"Tool error: {response['error']['message']}")
    
    # Extract result from MCP response format
    result_text = response['result']['content'][0]['text']
    return json.loads(result_text)

def parse_requirements_file(ws, file_path):
    """Parse a requirements file into structured requirements"""
    print(f"\n📖 Reading requirements from {file_path}...")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        text = f.read()
    
    print(f"✓ Read {len(text)} characters")
    print(f"\n🔍 Parsing requirements...")
    
    result = call_tool(ws, 'parse_requirements', {
        'text': text,
        'context': f'Requirements from {file_path}'
    })
    
    print(f"✓ Parsed {result['count']} requirements")
    
    return result['requirements']

def create_requirements(ws, project_id, requirements):
    """Create all requirements in the project"""
    created = []
    
    print(f"\n✍️  Creating {len(requirements)} requirements in project...")
    
    for idx, req in enumerate(requirements, 1):
        try:
            result = call_tool(ws, 'create_requirement', {
                'projectId': project_id,
                'title': req['title'],
                'description': req['description'],
                'priority': req['priority'],
                'status': req['status']
            })
            
            created.append(result)
            print(f"  {idx}. ✓ Created {result['reqId']}: {req['title'][:60]}")
            
        except Exception as e:
            print(f"  {idx}. ✗ Failed to create requirement: {e}")
    
    return created

def main():
    if len(sys.argv) < 4:
        print("Usage: python mcp-batch-import.py <jwt-token> <project-id> <requirements-file>")
        sys.exit(1)
    
    token = sys.argv[1]
    project_id = sys.argv[2]
    file_path = sys.argv[3]
    
    try:
        # Connect to MCP server
        ws = connect_mcp(token)
        
        # Parse requirements from file
        requirements = parse_requirements_file(ws, file_path)
        
        if not requirements:
            print("⚠️  No requirements found in file")
            return
        
        # Show parsed requirements
        print("\n📋 Parsed requirements:")
        for idx, req in enumerate(requirements, 1):
            print(f"  {idx}. [{req['priority']}] {req['title']}")
        
        # Confirm before creating
        response = input("\n❓ Create these requirements in project? [y/N]: ")
        if response.lower() != 'y':
            print("⚠️  Cancelled")
            return
        
        # Create all requirements
        created = create_requirements(ws, project_id, requirements)
        
        # Summary
        print(f"\n✅ Successfully created {len(created)} requirements!")
        print(f"\nCreated requirement IDs:")
        for req in created:
            print(f"  - {req['reqId']} ({req['requirementId']})")
        
        ws.close()
        
    except FileNotFoundError:
        print(f"❌ Error: File not found: {file_path}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
