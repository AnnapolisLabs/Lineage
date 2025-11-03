#!/usr/bin/env python3
"""
Export SonarQube issues in a format optimized for JetBrains Claude Agent to fix.
"""

import requests
import json
import sys
import os
from base64 import b64encode
from pathlib import Path

# Load .env file from project root
try:
    from dotenv import load_dotenv
    env_path = Path(__file__).parent.parent / '.env'
    load_dotenv(env_path)
except ImportError:
    print("Warning: python-dotenv not installed. Install with: pip install python-dotenv", file=sys.stderr)

# Configuration
SONARQUBE_URL = os.getenv("SONARQUBE_URL", "http://192.168.2.200:9000")
PROJECT_KEY = os.getenv("SONARQUBE_PROJECT_KEY", "lineage-sonar")
SONARQUBE_TOKEN = os.getenv("SONARQUBE_TOKEN")

if not SONARQUBE_TOKEN:
    raise ValueError("SONARQUBE_TOKEN environment variable must be set")

def fetch_issues():
    """Fetch all open issues from SonarQube"""
    url = f"{SONARQUBE_URL}/api/issues/search"
    params = {
        "componentKeys": PROJECT_KEY,
        "statuses": "OPEN",
        "ps": 500  # page size
    }

    # Create Basic Auth header (SonarQube tokens use token as username, empty password)
    auth_string = f"{SONARQUBE_TOKEN}:"
    auth_header = b64encode(auth_string.encode()).decode()
    headers = {
        "Authorization": f"Basic {auth_header}"
    }

    all_issues = []
    page = 1

    while True:
        params['p'] = page
        response = requests.get(url, params=params, headers=headers)
        response.raise_for_status()
        data = response.json()

        all_issues.extend(data['issues'])

        # Check if we have more pages
        total = data['paging']['total']
        if page * params['ps'] >= total:
            break
        page += 1

    return all_issues

def format_for_claude(issues):
    """Format issues for Claude Agent to understand and fix"""
    formatted = []

    for issue in issues:
        # Extract file path (remove project key prefix)
        file_path = issue['component'].split(':', 1)[1] if ':' in issue['component'] else issue['component']

        # Get line number
        line = issue.get('line', 'N/A')

        # Format the issue
        formatted_issue = {
            "file": file_path,
            "line": line,
            "severity": issue['severity'],
            "type": issue['type'],
            "rule": issue['rule'],
            "issue": issue['message'],
            "scope": issue.get('scope', 'MAIN')
        }

        formatted.append(formatted_issue)

    return formatted

def print_readable(issues):
    """Print in a human-readable format"""
    # Group by severity
    by_severity = {}
    for issue in issues:
        severity = issue['severity']
        by_severity.setdefault(severity, []).append(issue)

    # Print summary
    print(f"Total Issues: {len(issues)}\n")
    print("=" * 80)

    # Print in severity order
    for severity in ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']:
        if severity not in by_severity:
            continue

        print(f"\n{severity} ({len(by_severity[severity])} issues)")
        print("-" * 80)

        for issue in by_severity[severity]:
            print(f"\nFile: {issue['file']}")
            print(f"Line: {issue['line']}")
            print(f"Rule: {issue['rule']}")
            print(f"Issue: {issue['issue']}")

def print_claude_format(issues):
    """Print in a format optimized for Claude Agent"""
    print("# SonarQube Issues for Fixing\n")
    print("Below are code quality issues that need to be fixed. Each issue includes:")
    print("- File path and line number")
    print("- Severity and type")
    print("- Description of what needs to be fixed\n")
    print("=" * 80)

    # Filter out test issues by default (Claude should focus on main code)
    main_issues = [i for i in issues if i.get('scope') == 'MAIN']

    print(f"\n## Main Code Issues ({len(main_issues)} issues)\n")

    # Group by file for easier fixing
    by_file = {}
    for issue in main_issues:
        file_path = issue['file']
        by_file.setdefault(file_path, []).append(issue)

    for file_path in sorted(by_file.keys()):
        print(f"\n### {file_path}")
        print()

        for issue in sorted(by_file[file_path], key=lambda x: x['line'] if isinstance(x['line'], int) else 0):
            severity_emoji = {
                'BLOCKER': '🚨',
                'CRITICAL': '🔴',
                'MAJOR': '🟠',
                'MINOR': '🟡',
                'INFO': 'ℹ️'
            }.get(issue['severity'], '•')

            print(f"{severity_emoji} **Line {issue['line']}** [{issue['severity']}] ({issue['rule']})")
            print(f"   {issue['issue']}")
            print()

def main():
    format_type = sys.argv[1] if len(sys.argv) > 1 else "claude"

    print("Fetching issues from SonarQube...", file=sys.stderr)
    issues = fetch_issues()
    formatted = format_for_claude(issues)

    if format_type == "json":
        print(json.dumps(formatted, indent=2))
    elif format_type == "readable":
        print_readable(formatted)
    else:
        print_claude_format(formatted)

if __name__ == "__main__":
    main()