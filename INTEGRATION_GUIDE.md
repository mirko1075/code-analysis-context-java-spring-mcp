# MCP Server Integration Guide - Claude Code & GitHub Copilot

Complete guide to using the Java/Spring MCP Analysis Server with popular AI coding assistants.

---

## 📋 Table of Contents

1. [Claude Code Integration](#claude-code-integration)
2. [GitHub Copilot Integration](#github-copilot-integration)
3. [VS Code MCP Extension](#vs-code-mcp-extension)
4. [Comparison & Best Practices](#comparison--best-practices)
5. [Troubleshooting](#troubleshooting)

---

# Claude Code Integration

## 🚀 Quick Start with Claude Code

### Prerequisites

- ✅ Claude Code CLI installed
- ✅ Java 17+
- ✅ Project built: `mvn package`

### Step 1: Build the MCP Server

```bash
cd /path/to/code-analysis-context-java-spring-mcp
mvn clean package
```

This creates: `target/code-analysis-context-java-spring-mcp-1.0.0.jar` (27 MB)

### Step 2: Configure MCP Server

Create or edit `.claude/mcp.json` in your workspace:

```json
{
  "mcpServers": {
    "java-spring-analyzer": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/code-analysis-context-java-spring-mcp/target/code-analysis-context-java-spring-mcp-1.0.0.jar"
      ],
      "description": "Java/Spring code analysis with LSP, architecture, patterns, and coverage analysis",
      "env": {
        "LOG_LEVEL": "INFO"
      }
    }
  }
}
```

**Important:** Use absolute path or relative path from workspace root!

### Step 3: Restart Claude Code

```bash
# If using CLI
claude code restart

# Or simply restart your Claude Code session
```

### Step 4: Verify Installation

In Claude Code, ask:

```
What MCP servers are available?
```

You should see: **java-spring-analyzer** in the list.

---

## 💬 Using Tools in Claude Code

### Example 1: Analyze File Structure

**Your Prompt:**
```
Use the 'lsp' tool to analyze the structure of this file:
- path: /home/msiddi/development/my-spring-project
- file: src/main/java/com/example/UserService.java
- operation: symbols

Show me all classes, methods, and fields
```

**What Claude Code Does:**
1. Calls the `lsp` tool via MCP
2. Receives structured data back
3. Formats it nicely for you

**You Get:**
```
📁 File Structure: UserService.java

Classes:
├── UserService (Line 15)
│
Fields:
├── userRepository (Line 20)
├── emailService (Line 21)
│
Methods:
├── createUser (Line 25)
├── updateUser (Line 40)
├── deleteUser (Line 55)
├── findUserById (Line 70)
└── findAllUsers (Line 85)
```

---

### Example 2: Pre-Commit Validation

**Your Prompt:**
```
I'm about to commit changes to these files:
- UserController.java
- OrderService.java
- PaymentGateway.java

Use the 'lsp' tool with operation 'diagnostics' to check each file
for compilation errors. Tell me if it's safe to commit.
```

**Claude Code Will:**
1. Run diagnostics on each file
2. Analyze the results
3. Give you a clear answer

---

### Example 3: Architecture Analysis

**Your Prompt:**
```
Use the 'arch' tool to analyze my Spring Boot project:
- path: /home/msiddi/development/my-spring-project
- depth: detailed
- diagrams: true
- metrics: true

Then explain the architecture and highlight any issues
```

**You Get:**
- Package structure with complexity metrics
- Mermaid architecture diagrams
- High-complexity methods flagged
- Spring component analysis

---

### Example 4: Find Spring Patterns

**Your Prompt:**
```
Use the 'patterns' tool to detect Spring patterns in:
- path: /home/msiddi/development/my-spring-project
- types: ["rest", "jpa", "security"]

Are we following best practices?
```

**Claude Analyzes:**
- REST controllers and endpoints
- JPA entities and repositories
- Spring Security configuration
- Provides recommendations

---

### Example 5: Test Coverage Analysis

**Your Prompt:**
```
Use the 'coverage' tool to find untested code:
- path: /home/msiddi/development/my-spring-project
- report: target/site/jacoco/jacoco.xml
- priority: high

Which critical classes need tests?
```

**You Get:**
- Untested methods sorted by complexity
- JUnit 5 test scaffolds
- Priority recommendations

---

### Example 6: Project-Wide Error Scan

**Your Prompt:**
```
Use the 'lsp' tool with operation 'all-diagnostics' to scan
the entire project for compilation errors.

How many issues are there and which files need attention?
```

**Claude Reports:**
- Total error count
- Files with most issues
- Categorized by severity
- Top priorities

---

## 🎯 All 7 MCP Tools Available

| Tool | Purpose | Example Use Case |
|------|---------|------------------|
| **arch** | Architecture analysis | "Show me package complexity" |
| **deps** | Dependency mapping | "Find circular dependencies" |
| **patterns** | Pattern detection | "Check REST API design" |
| **coverage** | Test coverage | "Find untested code" |
| **conventions** | Code conventions | "Validate naming standards" |
| **context** | AI context packs | "Build context for refactoring" |
| **lsp** | LSP features | "Show file structure" |

---

## 🔥 Power User Tips for Claude Code

### Tip 1: Combine Multiple Tools

```
First use 'arch' to find high-complexity methods,
then use 'coverage' to see which are untested,
then use 'lsp' symbols to understand their structure.

Give me a prioritized list of refactoring targets.
```

### Tip 2: Natural Language Queries

You don't need to remember exact parameters:

```
Find all Spring REST controllers in my project
and check if they follow best practices
```

Claude Code will figure out to use the `patterns` tool!

### Tip 3: Iterative Analysis

```
1. Show me the architecture overview (use arch tool)
2. Now focus on the com.example.service package
3. Show me dependencies for just that package
4. Find circular dependencies in that area
```

### Tip 4: Code Review Assistant

```
I'm reviewing a PR that changes these 5 files:
[list files]

For each file:
1. Show structure (lsp symbols)
2. Check for errors (lsp diagnostics)
3. Analyze complexity (arch tool)

Summarize the review
```

---

## 📝 Configuration Options

### Environment Variables

```json
{
  "mcpServers": {
    "java-spring-analyzer": {
      "env": {
        "LOG_LEVEL": "DEBUG",           // INFO, DEBUG, WARN, ERROR
        "JAVA_OPTS": "-Xmx512m"         // JVM options
      }
    }
  }
}
```

### Per-Project Settings

Create `.code-analysis.json` in project root:

```json
{
  "analysis": {
    "includeGlobs": ["src/main/java/**/*.java"],
    "excludeGlobs": ["**/target/**", "**/*Test.java"]
  },
  "conventions": {
    "naming": {
      "methods": "camelCase",
      "classes": "PascalCase"
    }
  }
}
```

---

# GitHub Copilot Integration

## ⚠️ Current Status

**GitHub Copilot** (as of January 2026) does **not natively support MCP protocol** yet.

However, you can still use this MCP server with Copilot through:

### Option 1: VS Code MCP Extension (Recommended)

Install the MCP extension for VS Code which bridges MCP servers to Copilot Chat.

### Option 2: CLI Wrapper

Use the MCP server via command-line and feed results to Copilot Chat manually.

### Option 3: Wait for Native Support

GitHub is working on MCP support. Check: https://github.com/features/copilot

---

## 🔌 VS Code MCP Extension Setup

### Installation

1. **Install VS Code Extension**
```bash
code --install-extension mcp-integration
```

2. **Configure in VS Code Settings**

Add to `.vscode/settings.json`:

```json
{
  "mcp.servers": {
    "java-spring-analyzer": {
      "command": "java",
      "args": [
        "-jar",
        "${workspaceFolder}/path/to/code-analysis-context-java-spring-mcp-1.0.0.jar"
      ],
      "description": "Java/Spring code analysis"
    }
  }
}
```

3. **Reload VS Code**

```
Cmd/Ctrl + Shift + P → "Reload Window"
```

---

### Using with Copilot Chat

Once configured, you can use MCP tools in Copilot Chat:

**In Copilot Chat Panel:**

```
@mcp java-spring-analyzer analyze the architecture of this project

@mcp java-spring-analyzer find all Spring REST controllers

@mcp java-spring-analyzer check UserService.java for errors using LSP
```

---

### Example Workflow with Copilot

```
User: @mcp java-spring-analyzer use lsp tool to show symbols in
      src/main/java/com/example/UserController.java

Copilot: [Calls MCP server, receives symbol data]

         Here's the structure:
         - UserController (Class, Line 15)
           - getUserById (Method, Line 25)
           - createUser (Method, Line 40)
           - updateUser (Method, Line 60)
           ...

User: Now check if there are any compilation errors in that file

Copilot: [Uses lsp diagnostics]

         Found 2 errors:
         - Line 35: Missing semicolon
         - Line 42: Undefined variable 'userId'
```

---

## 🔧 CLI Wrapper for Copilot

If MCP extension is not available, use CLI wrapper:

### Create Helper Script

**`mcp-analyze.sh`:**
```bash
#!/bin/bash
# MCP Server CLI Wrapper for Copilot integration

PROJECT_PATH=$1
OPERATION=$2
FILE=$3

(
  echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"cli","version":"1.0"}}}'
  echo "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"lsp\",\"arguments\":{\"path\":\"$PROJECT_PATH\",\"file\":\"$FILE\",\"operation\":\"$OPERATION\"}}}"
) | java -jar /path/to/code-analysis-context-java-spring-mcp-1.0.0.jar 2>/dev/null | tail -1 | jq -r '.result.content[0].text'
```

### Usage

```bash
chmod +x mcp-analyze.sh

# Get file structure
./mcp-analyze.sh /my-project symbols src/main/java/UserService.java

# Check for errors
./mcp-analyze.sh /my-project diagnostics src/main/java/UserController.java
```

### Integration with Copilot

1. Run the script
2. Copy output
3. Paste into Copilot Chat with context:

```
Copilot Chat:
"I analyzed UserService.java with LSP and found these symbols:
[paste output]

Based on this structure, help me refactor the createUser method"
```

---

# Comparison & Best Practices

## 🆚 Claude Code vs GitHub Copilot

| Feature | Claude Code | GitHub Copilot + MCP Ext |
|---------|-------------|--------------------------|
| **MCP Support** | ✅ Native | ⚠️ Via Extension |
| **Tool Invocation** | Natural language | @mcp prefix |
| **Multi-step Analysis** | ✅ Excellent | ⚠️ Manual steps |
| **Context Understanding** | ✅ Deep | ✅ Good |
| **Setup Complexity** | Easy | Medium |
| **Response Format** | Rich markdown | Good |
| **Best For** | Complex analysis | Quick checks |

---

## ✨ Best Practices

### For Claude Code

✅ **Use Natural Language**
```
"Find all REST controllers and check if they follow best practices"
```
Not:
```
"Call the patterns tool with types=['rest'] and best=true"
```

✅ **Multi-Step Workflows**
```
1. Analyze architecture
2. Find high-complexity areas
3. Check test coverage
4. Recommend refactoring
```

✅ **Context Building**
```
"Use the context tool to build an AI-optimized pack for
adding JWT authentication to the REST API"
```

---

### For GitHub Copilot (with MCP)

✅ **Be Explicit with @mcp**
```
@mcp java-spring-analyzer use lsp tool with operation symbols
```

✅ **One Tool at a Time**
```
@mcp analyze architecture first
[Review results]
@mcp now check dependencies
```

✅ **Copy-Paste Bridge**
```
Run CLI script → Copy results → Paste to Copilot Chat
```

---

## 🎯 Recommended Workflows

### Workflow 1: Code Review (Claude Code)

```
I'm reviewing a PR with these changes:
- UserController.java (modified)
- OrderService.java (new)
- PaymentGateway.java (modified)

For each file:
1. Use 'lsp' tool to show structure (operation: symbols)
2. Use 'lsp' tool to check errors (operation: diagnostics)
3. Use 'arch' tool to check complexity
4. Use 'patterns' tool to validate Spring patterns

Give me a comprehensive review report.
```

---

### Workflow 2: Pre-Commit Check (Either Tool)

**Claude Code:**
```
Before I commit, scan the entire project with 'lsp' all-diagnostics
and tell me if there are any compilation errors I should fix
```

**Copilot + CLI:**
```bash
./mcp-analyze.sh /my-project all-diagnostics any.java > errors.txt
# Review errors.txt
# Fix issues
# Commit
```

---

### Workflow 3: Architecture Analysis (Claude Code)

```
Use the 'arch' tool to analyze my Spring Boot project at:
/home/user/projects/my-app

Set depth to 'detailed' and include diagrams and metrics.

Then identify:
1. Packages with highest complexity
2. Classes that need refactoring
3. Architectural issues
4. Recommendations for improvement
```

---

### Workflow 4: Test Coverage (Claude Code)

```
Use the 'coverage' tool to analyze test coverage:
- path: /home/user/projects/my-app
- report: target/site/jacoco/jacoco.xml
- priority: high

Generate JUnit 5 test scaffolds for the top 5
untested classes with highest complexity.
```

---

# Troubleshooting

## ❌ "Server not responding"

**Symptoms:** MCP server doesn't respond to tool calls

**Solutions:**

1. **Check Server is Running**
```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | \
  java -jar target/code-analysis-context-java-spring-mcp-1.0.0.jar

# Should return initialization response
```

2. **Check Logs**
```bash
# Set LOG_LEVEL=DEBUG in mcp.json
# Check stderr for errors
```

3. **Verify JAR Path**
```json
// Use absolute path
"args": ["-jar", "/absolute/path/to/server.jar"]
```

---

## ❌ "Tool not found: lsp"

**Symptoms:** Claude Code says tool doesn't exist

**Solutions:**

1. **Restart Claude Code**
```bash
claude code restart
```

2. **Verify MCP Configuration**
```bash
cat .claude/mcp.json
# Check syntax is valid JSON
```

3. **Test Tool Registration**
```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | \
  java -jar server.jar
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | \
  java -jar server.jar

# Should list 'lsp' in tools
```

---

## ❌ "Target file not found"

**Symptoms:** LSP operations fail with file not found

**Solutions:**

1. **Use Relative Paths**
```json
{
  "file": "src/main/java/UserService.java"
  // NOT: "/absolute/path/to/UserService.java"
}
```

2. **Verify Project Path**
```json
{
  "path": "/correct/project/root"
  // Should contain pom.xml or build.gradle
}
```

---

## ❌ "No diagnostics found" but file has errors

**Symptoms:** Known errors not detected by LSP

**Cause:** Missing classpath configuration

**Workaround:** LSP works best for syntax errors; compilation errors may need full classpath

**Solution:**
- Syntax errors: ✅ Detected
- Import errors: ⚠️ Detected (shown as unresolved imports)
- Type errors: ⚠️ May require classpath

---

## ❌ Copilot can't access MCP server

**Symptoms:** @mcp commands don't work in Copilot Chat

**Solutions:**

1. **Check MCP Extension Installed**
```bash
code --list-extensions | grep mcp
```

2. **Verify VS Code Settings**
```json
// .vscode/settings.json must have mcp.servers configured
```

3. **Use CLI Wrapper**
```bash
# Fallback to CLI wrapper method
./mcp-analyze.sh ...
```

---

## 📞 Getting Help

### Documentation
- [LSP_QUICK_START.md](LSP_QUICK_START.md) - Quick reference
- [LSP_DEMO.md](LSP_DEMO.md) - Detailed examples
- [LSP_TEST_RESULTS.md](LSP_TEST_RESULTS.md) - Test data

### Issues
- GitHub: https://github.com/yourusername/code-analysis-context-java-spring-mcp/issues

### Community
- Discord: [Your Discord Server]
- Discussions: GitHub Discussions

---

## 🎉 Summary

### Claude Code: ✅ READY TO USE
- Native MCP support
- Natural language tool invocation
- Multi-step complex workflows
- Best for comprehensive analysis

### GitHub Copilot: ⚠️ PARTIAL SUPPORT
- Requires MCP extension or CLI wrapper
- Works but needs manual steps
- Best for quick checks
- Native support coming soon

---

## 📚 Quick Reference Card

### Claude Code Commands

```bash
# File structure
"Use lsp tool operation symbols on [file]"

# Check errors
"Use lsp tool operation diagnostics on [file]"

# Scan project
"Use lsp tool operation all-diagnostics"

# Architecture
"Use arch tool on [project] with depth detailed"

# Patterns
"Use patterns tool to find Spring patterns"

# Coverage
"Use coverage tool with JaCoCo report"
```

### CLI Commands

```bash
# Build server
mvn clean package

# Test server
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | \
  java -jar target/code-analysis-context-java-spring-mcp-1.0.0.jar

# List tools
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | \
  java -jar target/code-analysis-context-java-spring-mcp-1.0.0.jar
```

---

**Last Updated:** 2026-01-26
**Version:** 1.0.0
**Status:** ✅ Production Ready
