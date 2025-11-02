#!/bin/bash

# Get JWT token from Lineage
echo "Getting JWT token from Lineage..."

RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@lineage.local", "password": "admin123"}')

TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ Failed to get token. Is the server running?"
    echo "Response: $RESPONSE"
    exit 1
fi

echo "✓ Token obtained successfully!"
echo ""
echo "Export this token:"
echo "export LINEAGE_JWT_TOKEN=\"$TOKEN\""
echo ""
echo "Or run:"
echo "export LINEAGE_JWT_TOKEN=\"$TOKEN\" && node lineage-mcp-ai-client.js"
