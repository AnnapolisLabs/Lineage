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

def fetch_duplications():
    """Fetch code duplications from SonarQube"""
    url = f"{SONARQUBE_URL}/api/duplications/show"

    # Create Basic Auth header
    auth_string = f"{SONARQUBE_TOKEN}:"
    auth_header = b64encode(auth_string.encode()).decode()
    headers = {
        "Authorization": f"Basic {auth_header}"
    }

    # Get all files in the project
    measures_url = f"{SONARQUBE_URL}/api/measures/component_tree"
    measures_params = {
        "component": PROJECT_KEY,
        "metricKeys": "duplicated_lines_density",
        "ps": 500
    }

    all_duplications = []
    page = 1

    while True:
        measures_params['p'] = page
        response = requests.get(measures_url, params=measures_params, headers=headers)
        response.raise_for_status()
        data = response.json()

        components = data.get('components', [])

        for component in components:
            # Only process files with duplications
            measures = component.get('measures', [])
            if not measures or float(measures[0].get('value', '0')) == 0:
                continue

            file_key = component['key']
            dup_response = requests.get(url, params={"key": file_key}, headers=headers)

            if dup_response.status_code == 200:
                dup_data = dup_response.json()
                if 'duplications' in dup_data and dup_data['duplications']:
                    all_duplications.append({
                        'file': component.get('path', file_key),
                        'duplications': dup_data['duplications'],
                        'files': dup_data.get('files', {}),
                        'density': float(measures[0].get('value', '0'))
                    })

        # Check if we have more pages
        paging = data.get('paging', {})
        total = paging.get('total', 0)
        if page * measures_params['ps'] >= total:
            break
        page += 1

    return all_duplications

def fetch_coverage():
    """Fetch code coverage from SonarQube"""
    url = f"{SONARQUBE_URL}/api/measures/component_tree"

    # Create Basic Auth header
    auth_string = f"{SONARQUBE_TOKEN}:"
    auth_header = b64encode(auth_string.encode()).decode()
    headers = {
        "Authorization": f"Basic {auth_header}"
    }

    params = {
        "component": PROJECT_KEY,
        "metricKeys": "coverage,line_coverage,branch_coverage,uncovered_lines,uncovered_conditions",
        "ps": 500
    }

    all_coverage = []
    page = 1

    while True:
        params['p'] = page
        response = requests.get(url, params=params, headers=headers)
        response.raise_for_status()
        data = response.json()

        components = data.get('components', [])

        for component in components:
            measures = component.get('measures', [])
            if not measures:
                continue

            # Parse measures into a dict
            metrics = {}
            for measure in measures:
                metrics[measure['metric']] = float(measure.get('value', '0'))

            # Only include files with less than 100% coverage or no coverage
            coverage = metrics.get('coverage', 0)
            if coverage < 100:
                all_coverage.append({
                    'file': component.get('path', component['key']),
                    'coverage': coverage,
                    'line_coverage': metrics.get('line_coverage', 0),
                    'branch_coverage': metrics.get('branch_coverage', 0),
                    'uncovered_lines': int(metrics.get('uncovered_lines', 0)),
                    'uncovered_conditions': int(metrics.get('uncovered_conditions', 0))
                })

        # Check if we have more pages
        paging = data.get('paging', {})
        total = paging.get('total', 0)
        if page * params['ps'] >= total:
            break
        page += 1

    return all_coverage

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

def print_claude_format(issues, duplications, coverage):
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
            print(f"**Line {issue['line']}** [{issue['severity']}] ({issue['rule']})")
            print(f"   {issue['issue']}")
            print()

    # Print duplications section
    if duplications:
        print("\n" + "=" * 80)
        print(f"\n## Code Duplications ({len(duplications)} files with duplicates)\n")
        print("Please review these duplicated code blocks and determine if they are:")
        print("- Genuine duplicates that should be refactored")
        print("- False positives that can be marked as such\n")

        for dup_info in sorted(duplications, key=lambda x: x['density'], reverse=True):
            file_path = dup_info['file']
            density = dup_info['density']
            print(f"\n### {file_path}")
            print(f"Duplication Density: {density:.1f}%\n")

            for dup in dup_info['duplications']:
                from_line = dup['blocks'][0]['from']
                size = dup['blocks'][0]['size']
                print(f"**Lines {from_line}-{from_line + size - 1}** ({size} lines duplicated)")

                # Show where it's duplicated
                for block in dup['blocks'][1:]:
                    dup_file = dup_info['files'].get(str(block['_ref']), {}).get('name', 'unknown')
                    print(f"   Duplicated in: {dup_file}:{block['from']}-{block['from'] + block['size'] - 1}")
                print()

    # Print coverage section
    if coverage:
        print("\n" + "=" * 80)
        print(f"\n## Code Coverage ({len(coverage)} files with incomplete coverage)\n")
        print("Files needing test coverage improvements:\n")

        for cov_info in sorted(coverage, key=lambda x: x['coverage']):
            file_path = cov_info['file']
            cov = cov_info['coverage']
            line_cov = cov_info['line_coverage']
            branch_cov = cov_info['branch_coverage']
            uncov_lines = cov_info['uncovered_lines']
            uncov_cond = cov_info['uncovered_conditions']

            print(f"\n### {file_path}")
            print(f"Overall Coverage: {cov:.1f}%")
            print(f"Line Coverage: {line_cov:.1f}% ({uncov_lines} uncovered lines)")
            print(f"Branch Coverage: {branch_cov:.1f}% ({uncov_cond} uncovered conditions)")
            print()

def main():
    format_type = sys.argv[1] if len(sys.argv) > 1 else "claude"

    print("Fetching issues from SonarQube...", file=sys.stderr)
    issues = fetch_issues()
    formatted = format_for_claude(issues)

    print("Fetching duplications from SonarQube...", file=sys.stderr)
    duplications = fetch_duplications()

    print("Fetching coverage from SonarQube...", file=sys.stderr)
    coverage = fetch_coverage()

    if format_type == "json":
        output = {
            "issues": formatted,
            "duplications": duplications,
            "coverage": coverage
        }
        print(json.dumps(output, indent=2))
    elif format_type == "readable":
        print_readable(formatted)
    else:
        print_claude_format(formatted, duplications, coverage)

if __name__ == "__main__":
    main()