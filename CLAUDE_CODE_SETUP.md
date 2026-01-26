# Claude Code - Setup in 3 Minuti ⚡

La guida più veloce per iniziare ad usare questo MCP server con Claude Code.

---

## 🚀 Setup Rapido

### 1. Build (30 secondi)

```bash
cd /path/to/code-analysis-context-java-spring-mcp
mvn clean package
```

✅ Crea: `target/code-analysis-context-java-spring-mcp-1.0.0.jar`

---

### 2. Configura (1 minuto)

Crea `.claude/mcp.json` nella root del tuo workspace:

```json
{
  "mcpServers": {
    "java-spring-analyzer": {
      "command": "java",
      "args": [
        "-jar",
        "/ABSOLUTE/PATH/TO/code-analysis-context-java-spring-mcp/target/code-analysis-context-java-spring-mcp-1.0.0.jar"
      ],
      "description": "Java/Spring analysis: LSP, architecture, patterns, coverage"
    }
  }
}
```

**⚠️ IMPORTANTE:** Sostituisci `/ABSOLUTE/PATH/TO/` con il percorso reale!

**Esempio:**
```json
"/home/msiddi/development/code-analysis-context-java-spring-mcp/target/..."
```

---

### 3. Restart Claude Code (30 secondi)

```bash
# Riavvia Claude Code
claude code restart
```

---

### 4. Verifica (30 secondi)

In Claude Code, chiedi:

```
Quali MCP server sono disponibili?
```

Dovresti vedere: ✅ **java-spring-analyzer**

---

## 💬 Primi Comandi da Provare

### Comando 1: Vedi Struttura File

```
Usa il tool 'lsp' per analizzare:
- path: /path/to/my-spring-project
- file: src/main/java/com/example/UserController.java
- operation: symbols

Mostrami la struttura del file
```

**Ricevi:**
- Tutte le classi
- Tutti i metodi
- Tutti i campi
- Con numeri di linea!

---

### Comando 2: Trova Errori di Compilazione

```
Usa il tool 'lsp' per:
- path: /path/to/my-spring-project
- file: src/main/java/com/example/UserService.java
- operation: diagnostics

Ci sono errori in questo file?
```

**Ricevi:**
- Lista errori con riga e messaggio
- Immediato, senza compilare!

---

### Comando 3: Scan Intero Progetto

```
Usa il tool 'lsp' per:
- path: /path/to/my-spring-project
- file: any.java
- operation: all-diagnostics

Trova tutti gli errori nel progetto
```

**Ricevi:**
- Errori in tutti i file Java
- ~3 secondi per 45 file
- Perfetto per pre-commit check!

---

### Comando 4: Analizza Architettura

```
Usa il tool 'arch' per analizzare:
- path: /path/to/my-spring-project
- depth: detailed
- diagrams: true
- metrics: true

Mostrami l'architettura del progetto
```

**Ricevi:**
- Struttura package
- Complessità ciclomatica
- Diagrammi Mermaid
- Metodi da refactorare

---

### Comando 5: Trova Pattern Spring

```
Usa il tool 'patterns' per:
- path: /path/to/my-spring-project
- types: ["rest", "jpa", "security"]

Quali pattern Spring usa il progetto?
```

**Ricevi:**
- Controller REST rilevati
- Entity JPA trovate
- Configurazione Security
- Best practices check

---

## 🎯 I 7 Tool Disponibili

| Tool | Cosa Fa | Quando Usarlo |
|------|---------|---------------|
| **lsp** | LSP features (errors, structure, etc) | Ogni giorno |
| **arch** | Analisi architettura e complessità | Code review |
| **deps** | Dipendenze e circular deps | Refactoring |
| **patterns** | Pattern Spring (Boot, MVC, JPA) | Best practices |
| **coverage** | Test coverage JaCoCo | Prima di release |
| **conventions** | Validazione naming | Code review |
| **context** | Context pack AI-optimized | Grandi refactor |

---

## 💡 Tips & Tricks

### Tip 1: Non Serve Ricordare i Parametri!

❌ **NON fare:**
```
Chiama il tool lsp con arguments.path=/project e
arguments.operation=symbols e arguments.file=...
```

✅ **FAI:**
```
Mostrami la struttura di UserController.java
```

Claude Code capisce da solo quale tool usare!

---

### Tip 2: Combina i Tool

```
Prima usa 'arch' per trovare metodi complessi,
poi usa 'coverage' per vedere quali non sono testati,
poi usa 'lsp' symbols per capire la struttura.

Dammi una lista prioritizzata di cosa refactorare.
```

Claude esegue tutto e ti dà un report completo!

---

### Tip 3: Code Review Automatico

```
Sto per fare code review di questi 3 file:
- UserController.java
- OrderService.java
- PaymentGateway.java

Per ognuno:
1. Mostra struttura (lsp symbols)
2. Trova errori (lsp diagnostics)
3. Calcola complessità (arch)

Poi dammi un summary della review
```

---

### Tip 4: Pre-Commit Hook

```
Prima di committare, usa 'lsp' all-diagnostics
per scannerizzare tutto il progetto.

Ci sono errori che dovrei fixare?
```

Eviti di pushare codice broken!

---

## ⚙️ Configurazione Avanzata (Opzionale)

### Aggiungi Variabili d'Ambiente

```json
{
  "mcpServers": {
    "java-spring-analyzer": {
      "command": "java",
      "args": ["-jar", "/path/to/server.jar"],
      "env": {
        "LOG_LEVEL": "DEBUG",
        "JAVA_OPTS": "-Xmx512m"
      }
    }
  }
}
```

### Configurazione Per-Progetto

Crea `.code-analysis.json` nella root del progetto:

```json
{
  "analysis": {
    "includeGlobs": ["src/main/java/**/*.java"],
    "excludeGlobs": ["**/target/**", "**/*Test.java"]
  },
  "conventions": {
    "naming": {
      "methods": "camelCase",
      "classes": "PascalCase",
      "constants": "UPPER_SNAKE_CASE"
    }
  }
}
```

---

## 🐛 Problemi Comuni

### "Server not responding"

```bash
# Test manualmente
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | \
  java -jar target/code-analysis-context-java-spring-mcp-1.0.0.jar

# Se non risponde, rebuilda
mvn clean package
```

### "Tool lsp not found"

```bash
# Riavvia Claude Code
claude code restart

# Verifica configurazione
cat .claude/mcp.json
```

### "Target file not found"

Usa path relativi al progetto:

✅ `src/main/java/UserService.java`
❌ `/absolute/path/to/UserService.java`

---

## 📚 Documentazione Completa

- **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)** - Guida completa Claude Code + Copilot
- **[LSP_QUICK_START.md](LSP_QUICK_START.md)** - Guida tool LSP
- **[LSP_DEMO.md](LSP_DEMO.md)** - Demo ed esempi
- **[LSP_TEST_RESULTS.md](LSP_TEST_RESULTS.md)** - Risultati test

---

## 🎉 Fatto! Sei Pronto

Hai completato il setup in **3 minuti**!

Ora prova il primo comando:

```
Usa il tool 'lsp' per mostrare i simboli di:
src/main/java/com/mcp/codeanalysis/server/McpServer.java

(questo è un file di questo stesso progetto!)
```

---

## 🚀 Next Level

### Workflow Avanzati

1. **Code Review Completo**
   - arch + patterns + lsp diagnostics

2. **Refactoring Planning**
   - arch (find complexity) + coverage (find untested)

3. **Pre-Release Check**
   - all-diagnostics + coverage + conventions

### Automazione

Integra in CI/CD:

```bash
# Jenkins, GitHub Actions, etc
./run-analysis.sh && \
  check-results.sh && \
  deploy-if-ok.sh
```

---

**Happy Coding!** 🎊

**Domande?** Leggi [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)

**Issues?** https://github.com/your-repo/issues
