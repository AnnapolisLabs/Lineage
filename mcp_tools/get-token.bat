@echo off
REM Get JWT token from Lineage (Windows)

echo Getting JWT token from Lineage...

curl -s -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\": \"admin@lineage.local\", \"password\": \"admin123\"}" > token_response.txt

REM Extract token (requires jq or manual copy)
echo.
echo Token saved to token_response.txt
echo.
echo Set the token manually:
echo set LINEAGE_JWT_TOKEN=your-token-here
echo.
type token_response.txt
