# LSP Tool - Quick Start Guide

## 🚀 5-Minute Quick Start

### Prerequisites
✅ Java 17+
✅ MCP server built: `mvn package`
✅ Claude Code with MCP configuration

### Configuration

Add to `.claude/mcp.json`:
```json
{
  "mcpServers": {
    "java-spring-analyzer": {
      "command": "java",
      "args": ["-jar", "./target/code-analysis-context-java-spring-mcp-1.0.0.jar"],
      "description": "Java/Spring code analysis with LSP support"
    }
  }
}
```

---

## 📖 Common Operations

### 1️⃣ Get File Structure (Most Useful!)

**When:** You need to understand a file's structure quickly

**Claude Code Prompt:**
```
Use the 'lsp' tool to analyze:
- path: /path/to/project
- file: src/main/java/com/example/UserService.java
- operation: symbols

Show me all classes, methods, and fields
```

**What You Get:**
```
✅ All classes in the file
✅ All methods with line numbers
✅ All fields
✅ Perfect for code navigation
```

---

### 2️⃣ Find All Compilation Errors (CI/CD)

**When:** Before committing, deploying, or in CI pipeline

**Claude Code Prompt:**
```
Use the 'lsp' tool to:
- path: /path/to/project
- file: src/main/java/AnyFile.java
- operation: all-diagnostics

Are there any compilation errors in the entire project?
```

**What You Get:**
```
✅ All files with errors
✅ Error counts per file
✅ Line numbers and messages
✅ ~3 seconds for 45 files
```

---

### 3️⃣ Check Single File Errors

**When:** Quick validation of one file

**Claude Code Prompt:**
```
Use the 'lsp' tool to:
- path: /path/to/project
- file: src/main/java/com/example/Controller.java
- operation: diagnostics

Does this file have any syntax errors?
```

**What You Get:**
```
✅ Errors and warnings in that file
✅ Line numbers
✅ Error messages
✅ <1 second response
```

---

### 4️⃣ Code Completion Suggestions

**When:** Exploring what's available in scope

**Claude Code Prompt:**
```
Use the 'lsp' tool to:
- path: /path/to/project
- file: src/main/java/Service.java
- operation: completions
- line: 42
- column: 10

What can I use here?
```

**What You Get:**
```
✅ Variables in scope
✅ Method parameters
✅ Java keywords
✅ Helpful for code exploration
```

---

## 🎯 Real-World Scenarios

### Scenario A: Code Review

**Task:** Review a PR with 5 changed files

```bash
# For each file:
"Use lsp tool operation: symbols
Show me the structure of [filename]"

# Then check for errors:
"Use lsp tool operation: diagnostics
Any issues in [filename]?"
```

**Time Saved:** 5-10 minutes per review

---

### Scenario B: Pre-Commit Hook

**Task:** Ensure no compilation errors before commit

```bash
"Use lsp tool operation: all-diagnostics
Scan the entire project for errors"
```

**Benefits:**
- ✅ Catch errors before CI
- ✅ ~3 seconds scan time
- ✅ No need to run full build

---

### Scenario C: Exploring New Codebase

**Task:** Understand a file you've never seen

```bash
# Step 1: Get structure
"Use lsp tool operation: symbols on [file]"

# Step 2: Check for issues
"Use lsp tool operation: diagnostics on [file]"

# Step 3: Explore context
"Use lsp tool operation: completions at line X"
```

**Benefit:** Quick orientation without running the app

---

### Scenario D: Refactoring Planning

**Task:** Want to rename a method/field

```bash
# Step 1: Find definition
"Use lsp tool operation: definition at line X column Y"

# Step 2: Find all usages
"Use lsp tool operation: references at line X column Y"
```

**Note:** Works best with full classpath configured

---

## 🎨 Claude Code Examples

### Example 1: Validate Before Commit

```
I'm about to commit changes to UserService.java and OrderController.java.
Use the 'lsp' tool to:
1. Check UserService.java for errors (operation: diagnostics)
2. Check OrderController.java for errors (operation: diagnostics)
3. If no errors, tell me it's safe to commit
```

### Example 2: Understand Complex File

```
I need to understand how AuthenticationService.java works.
Use the 'lsp' tool with operation: symbols to show me:
- All classes
- All methods
- All fields
Then explain the structure
```

### Example 3: Project Health Check

```
Use the 'lsp' tool with operation: all-diagnostics to scan the entire project.
Then tell me:
- How many files have errors?
- Which files have the most errors?
- Are there any critical issues?
```

---

## 📊 Operation Comparison

| Operation | Speed | Scope | Best For | Requires Position |
|-----------|-------|-------|----------|-------------------|
| **symbols** | ⚡ Fast | 1 file | Structure overview | ❌ No |
| **diagnostics** | ⚡ Fast | 1 file | Error checking | ❌ No |
| **all-diagnostics** | 🐢 Slow | All files | CI/CD validation | ❌ No |
| **completions** | ⚡ Fast | 1 position | Code exploration | ✅ Yes |
| **hover** | ⚡ Fast | 1 position | Type info* | ✅ Yes |
| **definition** | ⚡ Fast | 1 position | Navigate to def* | ✅ Yes |
| **references** | ⚡ Fast | 1 position | Find all uses* | ✅ Yes |

*Requires full classpath for best results

---

## ⚙️ Configuration Tips

### Basic (Works Now)
```java
// No configuration needed!
// Works out of the box for:
// - symbols
// - diagnostics
// - completions
// - all-diagnostics
```

### Advanced (Better Results)
```java
// Configure classpath in LspBridge
// For full type resolution:
// - hover
// - definition
// - references
```

---

## 🐛 Troubleshooting

### "Target file not found"
**Solution:** Use relative path from project root
```
✅ "src/main/java/Service.java"
❌ "/absolute/path/Service.java"
```

### "No hover info found"
**Cause:** Missing classpath configuration
**Workaround:** Use 'symbols' instead for structure

### "Found 0 references"
**Cause:** Type binding needs full classpath
**Workaround:** Use 'grep' or search tools

### Lots of import errors in diagnostics
**Cause:** Expected - classpath not configured
**Impact:** Minimal - syntax errors still detected

---

## 💡 Pro Tips

### Tip 1: Always Start with Symbols
```
Before diving into any file, get the structure first:
operation: symbols
```

### Tip 2: Use All-Diagnostics Sparingly
```
It scans the entire project (~3s for 45 files)
Use it for:
- Pre-commit checks
- CI/CD pipelines
- Periodic health checks
```

### Tip 3: Combine with Other Tools
```
1. Use 'lsp' tool for structure (operation: symbols)
2. Use 'arch' tool for complexity metrics
3. Use 'patterns' tool for Spring patterns
```

### Tip 4: Position is 1-indexed
```
Line 1 = first line
Column 1 = first character
(Not 0-indexed like some editors!)
```

---

## 📈 Performance Expectations

| Files | Operation | Time |
|-------|-----------|------|
| 1 | symbols | <1s |
| 1 | diagnostics | <1s |
| 1 | completions | <1s |
| 45 | all-diagnostics | ~3s |
| 100+ | all-diagnostics | ~6-8s |

**Memory Usage:** ~50MB per operation
**Concurrent Operations:** Thread-safe

---

## 🎓 Learning Path

### Beginner
1. ✅ Start with `symbols` - understand file structure
2. ✅ Try `diagnostics` - find errors in one file
3. ✅ Experiment with `completions` - explore code

### Intermediate
4. ✅ Use `all-diagnostics` - scan entire project
5. ✅ Combine with other MCP tools
6. ✅ Integrate into workflow

### Advanced
7. 🔄 Configure classpath for full resolution
8. 🔄 Use `hover`, `definition`, `references`
9. 🔄 Build custom automation scripts

---

## 🎉 You're Ready!

Start with this simple command:

```
Use the 'lsp' tool to show me the symbols in:
src/main/java/com/mcp/codeanalysis/server/McpServer.java
```

Then explore from there! 🚀

---

**Questions?** Check [LSP_DEMO.md](LSP_DEMO.md) for detailed examples
**Issues?** See [LSP_TEST_RESULTS.md](LSP_TEST_RESULTS.md) for test data

**Happy Coding!** 💻✨
