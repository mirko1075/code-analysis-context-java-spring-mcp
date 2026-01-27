# Code Analysis & Context Engineering MCP - Java/Spring Edition

A sophisticated Model Context Protocol (MCP) server that provides deep codebase understanding for Java projects, with support for both Spring Framework and Spring Boot applications, as well as enterprise frameworks.

## 🎯 Overview

This MCP server is specifically designed for Java projects, supporting both traditional Spring Framework applications and modern Spring Boot applications. It provides architectural analysis, pattern detection, dependency mapping, test coverage analysis, and AI-optimized context generation for enterprise Java development.

## ✨ Features

- **🏗️ Architecture Analysis**: Comprehensive architectural overview with package relationships, class hierarchies, and Spring component dependencies (both XML and annotation-based configurations)
- **🔍 Pattern Detection**: Identify Spring Framework patterns (traditional and Boot), REST API design, dependency injection, and antipatterns
- **📊 Dependency Mapping**: Visualize Maven/Gradle dependencies, detect circular dependencies, and analyze coupling
- **🧪 Coverage Analysis**: Find untested code with actionable test suggestions based on complexity (JUnit/TestNG)
- **✅ Convention Validation**: Validate adherence to Java conventions, Spring Framework best practices, and coding standards
- **🤖 Context Generation**: Build optimal AI context packs respecting token limits and maximizing relevance

## ☕ Supported Frameworks & Libraries

### Spring Framework (Traditional & Boot)
- ✅ **Spring Core** - IoC container, dependency injection, bean lifecycle, ApplicationContext
- ✅ **Spring XML Configuration** - XML-based bean definitions, namespaces, property placeholders
- ✅ **Spring JavaConfig** - @Configuration, @Bean, component scanning, profiles
- ✅ **Spring MVC** - Controllers, REST APIs, request mappings, exception handling, view resolvers
- ✅ **Spring AOP** - Aspect-oriented programming, proxies, advice, pointcuts
- ✅ **Spring JDBC** - JdbcTemplate, transaction management, DataSource configuration
- ✅ **Spring ORM** - Hibernate integration, JPA support, transaction synchronization
- ✅ **Spring Boot** - Auto-configuration, starters, application properties, embedded servers
- ✅ **Spring Data JPA** - Repositories, entities, query methods, specifications
- ✅ **Spring Security** - Authentication, authorization, security configurations (XML & Java config)
- ✅ **Spring Web Services** - SOAP services, XML marshalling, WS-Security
- ✅ **Spring Batch** - Batch processing, jobs, steps, item readers/writers
- ✅ **Spring Integration** - Messaging, channels, adapters, enterprise integration patterns
- ✅ **Spring Cloud** - Microservices patterns, service discovery, config server

### Data & Persistence
- ✅ **Hibernate/JPA** - Entity mappings, relationships, lazy loading, caching
- ✅ **MyBatis** - XML/annotation mappers, SQL queries
- ✅ **Flyway/Liquibase** - Database migrations, versioning

### Testing Frameworks
- ✅ **JUnit 5** - Test classes, assertions, lifecycle, parametrized tests
- ✅ **Mockito** - Mocking, stubbing, argument captors
- ✅ **Spring Test** - Integration tests, MockMvc, test slices

## 📦 Installation

### Prerequisites

- Java 17 or higher (LTS)
- Maven 3.8+ or Gradle 7.0+

### Install from source

```bash
git clone https://github.com/yourusername/code-analysis-context-java-spring-mcp.git
cd code-analysis-context-java-spring-mcp
mvn clean install
```

### Build executable JAR

```bash
mvn package
# Creates target/code-analysis-context-java-spring-mcp-1.0.0.jar
```

## 🚀 Usage

### As an MCP Server

Add to your MCP client configuration:

```json
{
  "mcpServers": {
    "code-analysis-java": {
      "command": "java",
      "args": ["-jar", "/path/to/code-analysis-context-java-spring-mcp-1.0.0.jar"]
    }
  }
}
```

Or if installed globally:

```json
{
  "mcpServers": {
    "code-analysis-java": {
      "command": "code-analysis-java-mcp"
    }
  }
}
```

## 💬 Example Usage

Once configured as an MCP server in your LLM client, you can use natural language prompts:

### Analyzing a Spring Project

*"Analyze the architecture of my Spring project and show me the complexity metrics"*

This will invoke the `arch` tool with appropriate parameters to:
- Detect Spring Framework usage (Boot or traditional)
- Calculate complexity for all Java files
- Show package structure and class hierarchies
- Identify bean definitions (XML, @Configuration, @Component)
- Identify high-complexity methods needing refactoring

*"Find all controllers in my project and check if they follow best practices"*

This will use the `patterns` tool to:
- Detect @Controller, @RestController, @RequestMapping annotations
- Identify Spring MVC configuration (DispatcherServlet, view resolvers)
- Check request/response DTOs and validation
- Compare against Spring Framework best practices
- Suggest improvements (e.g., proper exception handling, RESTful design)

*"Analyze my Spring XML configuration and show bean dependencies"*

The `arch` and `deps` tools will:
- Parse applicationContext.xml, servlet-context.xml files
- Extract bean definitions and dependencies
- Detect property placeholders and profiles
- Show bean dependency graph
- Identify circular bean dependencies

### Finding Code Issues

*"Check my project for circular dependencies and show me the dependency graph"*

Uses the `deps` tool to:
- Build dependency graph from Maven/Gradle and package imports
- Detect any circular dependency cycles
- Calculate coupling/cohesion metrics
- Generate Mermaid diagram of dependencies

*"Which files in my project have no test coverage and are most critical to test?"*

The `coverage` tool will:
- Parse JaCoCo coverage reports (XML/CSV)
- Prioritize gaps based on complexity and file location
- Generate JUnit test scaffolds for critical classes
- Show untested methods with complexity scores

### Code Quality

*"Validate my project follows Java conventions and show naming violations"*

The `conventions` tool will:
- Check camelCase, PascalCase, UPPER_SNAKE_CASE naming
- Validate package structure and import organization
- Check for missing JavaDoc and proper annotations
- Report consistency score by category

### AI-Assisted Development

*"Generate a context pack for adding input validation to my Spring REST API"*

The `context` tool will:
- Extract keywords ("input validation", "Spring", "REST API")
- Score files by relevance to the task
- Select most relevant controllers, DTOs, and services within token budget
- Include relevant XML configuration if applicable
- Format as markdown with code snippets and suggestions

*"Help me migrate from XML configuration to JavaConfig in my Spring application"*

The `context` tool will:
- Analyze existing XML bean definitions
- Identify @Configuration and @Bean patterns in the codebase
- Provide context for migration strategy
- Include both current XML and target JavaConfig examples

## 🛠️ Available Tools

### 1. `arch` - Architecture Analysis

Analyze Java/Spring project architecture and structure (supports both traditional Spring and Spring Boot).

```json
{
  "path": "/path/to/project",
  "depth": "d",
  "types": ["pkg", "class", "method", "api", "xml", "beans"],
  "diagrams": true,
  "metrics": true,
  "details": true,
  "minCx": 10,
  "maxFiles": 50,
  "includeXml": true
}
```

**Parameters:**
- `path`: Project root directory
- `depth`: Analysis depth - "o" (overview), "d" (detailed), "x" (deep)
- `types`: Analysis types - pkg, class, method, api, entity, repo, service, controller, config, xml, beans, aspect
- `diagrams`: Generate Mermaid diagrams
- `metrics`: Include code metrics (cyclomatic complexity, lines of code)
- `details`: Per-file detailed metrics
- `minCx`: Minimum complexity threshold for filtering
- `maxFiles`: Maximum number of files in detailed output
- `memSuggest`: Generate memory suggestions for llm-memory MCP
- `fw`: Force framework detection - spring, spring-boot, spring-mvc, spring-data, spring-security, spring-aop, hibernate
- `includeXml`: Parse XML configuration files (applicationContext.xml, servlet-context.xml, etc.)

**Detects:**

**Spring Framework (Traditional):**
- XML bean definitions (applicationContext.xml, beans.xml, etc.)
- Bean dependencies and wiring in XML
- Property placeholder configuration
- Spring profiles and conditional beans
- DispatcherServlet and web.xml configuration
- View resolvers and handler mappings
- DataSource and transaction manager configuration

**Spring Framework (Annotation-based & Boot):**
- Package structure and organization
- Class hierarchies and inheritance patterns
- Method definitions and annotations
- Component scanning (@Component, @Service, @Repository, @Controller)
- Configuration classes (@Configuration, @Bean)
- REST API endpoints (@Controller, @RestController, @RequestMapping)
- JPA entities and relationships
- Repository interfaces (Spring Data)
- Service layer components
- Aspect-oriented programming (@Aspect, @Before, @After, @Around)
- Transaction boundaries (@Transactional)

### 2. `deps` - Dependency Analysis

Analyze package dependencies and imports.

```json
{
  "path": "/path/to/project",
  "circular": true,
  "metrics": true,
  "diagram": true,
  "focus": "com.example.service",
  "depth": 3
}
```

**Parameters:**
- `path`: Project root directory
- `circular`: Detect circular dependencies
- `metrics`: Calculate coupling/cohesion metrics
- `diagram`: Generate Mermaid dependency graph
- `focus`: Focus on specific package
- `depth`: Maximum dependency depth to traverse
- `external`: Include Maven/Gradle external dependencies
- `maven`: Parse Maven pom.xml dependencies
- `gradle`: Parse Gradle build files

**Features:**
- Import graph construction from Java source files
- Maven/Gradle dependency tree analysis
- Circular dependency detection with cycle paths
- Coupling and cohesion metrics
- Dependency hotspots (hubs and bottlenecks)
- Package classification (controller, service, repository, model, etc.)

### 3. `patterns` - Pattern Detection

Detect Spring Framework and enterprise Java patterns (traditional Spring and Spring Boot).

```json
{
  "path": "/path/to/project",
  "types": ["rest", "mvc", "jpa", "security", "di", "transaction", "aop", "xml"],
  "custom": true,
  "best": true,
  "suggest": true,
  "configType": "all"
}
```

**Parameters:**
- `path`: Project root directory
- `types`: Pattern types to detect
  - `mvc`: Spring MVC patterns (@Controller, view resolvers, ModelAndView)
  - `rest`: REST API patterns (@RestController, @RequestMapping, DTOs)
  - `jpa`: JPA/Hibernate entity patterns and relationships
  - `security`: Spring Security patterns (authentication, authorization)
  - `di`: Dependency injection patterns (@Autowired, @Component, @Service, XML beans)
  - `transaction`: Transaction management (@Transactional, XML tx:advice)
  - `config`: Configuration patterns (@Configuration, @Bean, @Value, XML config)
  - `aop`: AOP patterns (@Aspect, XML aop:config, proxies)
  - `xml`: XML-based configuration patterns
  - `validation`: Bean validation patterns (@Valid, @NotNull, etc.)
  - `exception`: Exception handling (@ExceptionHandler, @ControllerAdvice)
  - `async`: Async processing (@Async, CompletableFuture)
  - `cache`: Caching patterns (@Cacheable, @CacheEvict)
  - `jdbc`: Spring JDBC patterns (JdbcTemplate, RowMapper)
- `custom`: detect custom patterns
- `best`: Compare with Spring Framework best practices
- `suggest`: Generate improvement suggestions
- `configType`: Configuration types to analyze - "xml", "java", "both", "all"

**Detected Patterns:**

**Traditional Spring Framework:**
- **XML Configuration**: Bean definitions, property injection, constructor injection
- **XML MVC Configuration**: DispatcherServlet, view resolvers, handler mappings, interceptors
- **XML AOP Configuration**: Aspects, pointcuts, advice, proxy-target-class
- **XML Transaction Management**: PlatformTransactionManager, tx:advice, tx:annotation-driven
- **XML Security**: http, authentication-provider, intercept-url patterns
- **Property Placeholder**: PropertyPlaceholderConfigurer, context:property-placeholder
- **Bean Lifecycle**: InitializingBean, DisposableBean, init-method, destroy-method
- **ApplicationContext**: ClassPathXmlApplicationContext, FileSystemXmlApplicationContext
- **Spring JDBC**: JdbcTemplate, NamedParameterJdbcTemplate, RowMapper, ResultSetExtractor

**Annotation-based & Boot:**
- **MVC Patterns**: @Controller, @RequestMapping, ModelAndView, view resolvers
- **REST API**: Controller design, request/response DTOs, proper HTTP methods
- **JPA/Hibernate**: Entity mappings, lazy/eager loading, N+1 query detection
- **Spring Security**: Authentication flows, authorization rules, security configurations
- **Dependency Injection**: Constructor vs field injection, @Autowired, @Qualifier, circular dependencies
- **AOP**: @Aspect, @Before, @After, @Around, @Pointcut
- **Transactions**: Proper transaction boundaries, @Transactional, isolation levels, propagation
- **Configuration**: @Configuration, @Bean, @ComponentScan, @PropertySource
- **Validation**: Input validation, custom validators
- **Exception Handling**: Global exception handlers, custom exceptions
- **Testing**: JUnit, MockMvc, @ContextConfiguration, @WebMvcTest, @DataJpaTest, Mockito patterns

### 4. `coverage` - Test Coverage Analysis

Analyze test coverage and generate test suggestions.

```json
{
  "path": "/path/to/project",
  "report": "target/site/jacoco/jacoco.xml",
  "fw": "junit5",
  "threshold": {
    "lines": 80,
    "methods": 80,
    "branches": 75
  },
  "priority": "high",
  "tests": true,
  "cx": true
}
```

**Parameters:**
- `path`: Project root directory
- `report`: Coverage report path (jacoco.xml, jacoco.csv, cobertura.xml)
- `fw`: Test framework - junit5, junit4, testng
- `threshold`: Coverage thresholds
- `priority`: Filter priority - crit, high, med, low, all
- `tests`: Generate test scaffolds
- `cx`: Analyze complexity for prioritization

**Features:**
- Parse JaCoCo and Cobertura reports
- Identify untested classes and methods
- Complexity-based prioritization
- Test scaffold generation (JUnit 5, Mockito)
- Spring Test patterns (@ContextConfiguration, @SpringBootTest, @WebMvcTest, @DataJpaTest)
- Support for XML-based ApplicationContext loading in tests

### 5. `conventions` - Convention Validation

Validate Java conventions and Spring Framework best practices (traditional Spring and Spring Boot).

```json
{
  "path": "/path/to/project",
  "auto": true,
  "severity": "warn",
  "rules": {
    "naming": {
      "methods": "camelCase",
      "classes": "PascalCase",
      "constants": "UPPER_SNAKE_CASE"
    }
  }
}
```

**Parameters:**
- `path`: Project root directory
- `auto`: Auto-detect project conventions
- `severity`: Minimum severity - err, warn, info
- `rules`: Custom convention rules
- `checkstyle`: Use Checkstyle configuration if available

**Checks:**
- Java naming conventions (camelCase, PascalCase, UPPER_SNAKE_CASE)
- Package structure and naming
- Import ordering and organization
- JavaDoc presence and quality
- Annotation usage (Spring annotations, JPA annotations)
- Exception handling best practices
- Resource management (try-with-resources)

**Spring Framework Specific:**
- Bean naming conventions (XML and annotations)
- XML configuration structure and organization
- Proper use of constructor vs setter injection
- Transaction boundary placement
- Controller/service/repository layering patterns
- AOP pointcut expression quality
- Security configuration best practices
- Proper ApplicationContext hierarchy
- XML namespace usage and versioning

### 6. `context` - Context Pack Generation

Generate AI-optimized context packs.

```json
{
  "task": "Add authentication and authorization to REST API",
  "path": "/path/to/project",
  "tokens": 50000,
  "include": ["files", "arch", "patterns"],
  "focus": ["src/main/java/com/example/controller", "src/main/java/com/example/security"],
  "format": "md",
  "lineNums": true,
  "strategy": "rel"
}
```

**Parameters:**
- `task`: Task description (required)
- `path`: Project root directory
- `tokens`: Token budget (default: 50000)
- `include`: Content types - files, deps, tests, entities, arch, conv, config
- `focus`: Priority packages/directories
- `history`: Include git history
- `format`: Output format - md, json, xml
- `lineNums`: Include line numbers
- `strategy`: Optimization strategy - rel (relevance), wide (breadth), deep (depth)

**Features:**
- Task-based file relevance scoring
- Token budget management
- Multiple output formats
- Architectural context inclusion
- Dependency traversal
- Spring configuration context
  - Spring Boot: application.yml, application.properties
  - Traditional Spring: applicationContext.xml, servlet-context.xml, web.xml
  - Property files and resource bundles

### 7. `lsp` - Language Server Protocol Analyzer

Provides IDE-like features using Eclipse JDT Language Server Protocol implementation.

```json
{
  "path": "/path/to/project",
  "file": "src/main/java/com/example/UserService.java",
  "operation": "diagnostics",
  "line": 42,
  "column": 15
}
```

**Parameters:**
- `path`: Project root directory (required)
- `file`: Target Java file path - relative or absolute (required)
- `operation`: LSP operation to perform (required)
  - `diagnostics` - Get compilation errors and warnings for a file
  - `hover` - Get type information at cursor position (requires line/column)
  - `definition` - Find definition of symbol at cursor (requires line/column)
  - `references` - Find all references to symbol (requires line/column)
  - `completions` - Get code completion suggestions (requires line/column)
  - `symbols` - Get document outline (classes, methods, fields)
  - `all-diagnostics` - Get diagnostics for all Java files in project
- `line`: Line number (required for hover, definition, references, completions)
- `column`: Column number (required for hover, definition, references, completions)

**Features:**

**Diagnostics:**
- Compilation errors and warnings
- Syntax errors
- Type checking errors
- Project-wide diagnostics scanning

**Code Intelligence:**
- Hover information (type, kind, documentation)
- Go to definition
- Find all references
- Code completion suggestions
- Document symbols (outline view)

**Use Cases:**
- Pre-commit validation (find all errors before commit)
- Code navigation and exploration
- Type information lookup
- Symbol refactoring planning
- Documentation generation from symbols

**Example Operations:**

```bash
# Get all compilation errors in a file
{
  "path": "/project",
  "file": "src/main/java/UserController.java",
  "operation": "diagnostics"
}

# Get type info at cursor position
{
  "path": "/project",
  "file": "src/main/java/UserService.java",
  "operation": "hover",
  "line": 25,
  "column": 10
}

# Find where a variable/method is defined
{
  "path": "/project",
  "file": "src/main/java/UserService.java",
  "operation": "definition",
  "line": 30,
  "column": 15
}

# Find all usages of a symbol
{
  "path": "/project",
  "file": "src/main/java/User.java",
  "operation": "references",
  "line": 10,
  "column": 20
}

# Get code completions at cursor
{
  "path": "/project",
  "file": "src/main/java/UserService.java",
  "operation": "completions",
  "line": 40,
  "column": 8
}

# Get document structure (outline)
{
  "path": "/project",
  "file": "src/main/java/UserController.java",
  "operation": "symbols"
}

# Scan all files for errors
{
  "path": "/project",
  "file": "any-file.java",
  "operation": "all-diagnostics"
}
```

**Integration with Eclipse JDT:**
- Uses Eclipse JDT Core for accurate Java parsing
- Supports Java 17+ language features (LTS)
- Resolves types and bindings
- Provides IDE-quality analysis

## 📝 Configuration

Create a `.code-analysis.json` file in your project root:

### For Spring Boot Projects

```json
{
  "project": {
    "name": "MySpringBootApp",
    "type": "spring-boot"
  },
  "analysis": {
    "includeGlobs": ["src/main/java/**/*.java", "src/main/resources/**/*.{yml,properties,xml}"],
    "excludeGlobs": ["**/target/**", "**/build/**", "**/*Test.java", "**/*IT.java"]
  },
  "conventions": {
    "naming": {
      "methods": "camelCase",
      "classes": "PascalCase",
      "constants": "UPPER_SNAKE_CASE",
      "packages": "lowercase"
    },
    "imports": {
      "order": ["java", "javax", "org", "com"],
      "grouping": true
    },
    "spring": {
      "preferConstructorInjection": true,
      "requireTransactional": true,
      "controllerNaming": ".*Controller$"
    }
  },
  "coverage": {
    "threshold": {
      "lines": 80,
      "methods": 80,
      "branches": 75
    },
    "reportPath": "target/site/jacoco/jacoco.xml"
  }
}
```

### For Traditional Spring Projects

```json
{
  "project": {
    "name": "MySpringApp",
    "type": "spring"
  },
  "analysis": {
    "includeGlobs": [
      "src/main/java/**/*.java",
      "src/main/resources/**/*.xml",
      "src/main/webapp/WEB-INF/**/*.xml",
      "src/main/resources/**/*.properties"
    ],
    "excludeGlobs": ["**/target/**", "**/build/**", "**/*Test.java"],
    "xmlConfig": {
      "contextFiles": [
        "src/main/resources/applicationContext.xml",
        "src/main/webapp/WEB-INF/spring/servlet-context.xml"
      ],
      "parseNamespaces": ["beans", "context", "tx", "aop", "mvc", "security"]
    }
  },
  "conventions": {
    "naming": {
      "methods": "camelCase",
      "classes": "PascalCase",
      "constants": "UPPER_SNAKE_CASE",
      "packages": "lowercase",
      "beans": "camelCase"
    },
    "imports": {
      "order": ["java", "javax", "org.springframework", "org", "com"],
      "grouping": true
    },
    "spring": {
      "preferConstructorInjection": true,
      "requireTransactional": true,
      "controllerNaming": ".*Controller$",
      "xmlConfig": {
        "requireSchemaLocation": true,
        "validateNamespaces": true,
        "preferAnnotationConfig": false
      },
      "beanDefinitions": {
        "requireId": true,
        "preferSingletons": true
      }
    }
  },
  "coverage": {
    "threshold": {
      "lines": 75,
      "methods": 75,
      "branches": 70
    },
    "reportPath": "target/site/jacoco/jacoco.xml"
  }
}
```

## 🔧 Development

### Project Structure

```
code-analysis-context-java-spring-mcp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/mcp/codeanalysis/
│   │   │       ├── server/
│   │   │       │   ├── McpServer.java           # MCP server entry point
│   │   │       │   ├── JsonRpcHandler.java      # JSON-RPC protocol handler
│   │   │       │   └── ToolRegistry.java        # Tool registration
│   │   │       ├── tools/                       # Tool implementations
│   │   │       │   ├── ArchitectureAnalyzer.java
│   │   │       │   ├── PatternDetector.java
│   │   │       │   ├── DependencyMapper.java
│   │   │       │   ├── CoverageAnalyzer.java
│   │   │       │   ├── ConventionValidator.java
│   │   │       │   └── ContextPackGenerator.java
│   │   │       ├── analyzers/                   # Framework-specific analyzers
│   │   │       │   ├── SpringFrameworkAnalyzer.java
│   │   │       │   ├── SpringBootAnalyzer.java
│   │   │       │   ├── JpaAnalyzer.java
│   │   │       │   ├── SecurityAnalyzer.java
│   │   │       │   └── AopAnalyzer.java
│   │   │       ├── utils/                       # Utilities
│   │   │       │   ├── JavaParser.java
│   │   │       │   ├── XmlConfigParser.java
│   │   │       │   ├── ComplexityAnalyzer.java
│   │   │       │   ├── MavenParser.java
│   │   │       │   └── DiagramGenerator.java
│   │   │       └── types/                       # DTOs and types
│   │   └── resources/
│   │       └── logback.xml
│   └── test/
│       └── java/
│           └── com/mcp/codeanalysis/
├── pom.xml
└── README.md
```

### Running Tests

```bash
mvn test
mvn test jacoco:report
```

### Code Quality

```bash
# Format code
mvn spotless:apply

# Lint
mvn checkstyle:check

# Static analysis
mvn spotbugs:check
mvn pmd:check
```

## 🎯 Implementation Status

### ✅ Ready for Development!

**Core Tools (Planned)**
- 🔄 **Architecture Analyzer** - JavaParser AST parsing, XML config parsing, complexity metrics, Spring framework detection, Mermaid diagrams
- 🔄 **Pattern Detector** - Spring Framework patterns (XML & annotations), Spring Boot patterns, REST API design, JPA/Hibernate patterns, AOP patterns, best practices
- 🔄 **Dependency Mapper** - Maven/Gradle dependency trees, package import graphs, bean dependency graphs (XML), circular detection, coupling metrics
- 🔄 **Coverage Analyzer** - JaCoCo integration, test scaffolds, complexity-based prioritization
- 🔄 **Convention Validator** - Java conventions, Spring Framework best practices (XML & JavaConfig), naming standards, auto-detection
- 🔄 **Context Pack Generator** - Task-based relevance, token budgets, multiple formats, AI optimization

**Utilities (Planned)**
- 🔄 JavaParser Integration - Classes, methods, annotations, imports, complexity
- 🔄 XML Config Parser - Spring XML bean definitions, namespaces, property placeholders, profiles
- 🔄 Complexity Analyzer - Cyclomatic complexity, maintainability index
- 🔄 Maven/Gradle Parser - Dependency extraction, version management
- 🔄 Framework Detector - Spring Framework, Spring Boot, Spring MVC, Spring Data, Spring Security, Spring AOP
- 🔄 Diagram Generator - Mermaid architecture & dependency graphs, bean dependency graphs

**Features**
- 🔄 Circular dependency detection with cycle paths (both package-level and bean-level)
- 🔄 LLM memory integration for persistent context
- 🔄 Test scaffold generation (JUnit 5, Mockito, Spring Test)
- 🔄 Multi-format output (JSON, Markdown, XML)
- 🔄 Token budget management for AI tools
- 🔄 Complexity-based prioritization
- 🔄 Mermaid diagram generation
- 🔄 Spring configuration analysis
  - Traditional Spring: XML bean definitions, web.xml, context configurations
  - Spring Boot: application.yml, application.properties
- 🔄 Bean dependency graph visualization from XML and annotations
- 🔄 AOP aspect detection and pointcut analysis
- 🔄 Transaction boundary analysis (XML and @Transactional)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

MIT

## 👨‍💻 Author

Adapted for Java/Spring Boot by [Your Name]
Based on the Python version by Andrea Salvatore (@andreahaku)

## 🔗 Related Projects

- [llm-memory-mcp](https://github.com/andreahaku/llm_memory_mcp) - Persistent memory for LLM tools
- [code-analysis-context-mcp](https://github.com/andreahaku/code-analysis-context-mcp) - TypeScript/JavaScript version
- [code-analysis-context-python-mcp](https://github.com/andreahaku/code-analysis-context-python-mcp) - Python version

## 🧪 Testing

Test all tools on a sample Spring project:

```bash
# Test with a Spring Boot project
java -jar target/code-analysis-context-java-spring-mcp-1.0.0.jar --test --project-type=spring-boot

# Test with a traditional Spring project
java -jar target/code-analysis-context-java-spring-mcp-1.0.0.jar --test --project-type=spring

# Run all integration tests
mvn test
```

This will run all 6 tools and display:
- Project architecture and complexity metrics
- Pattern detection (Controllers, JPA entities, Spring Security, AOP aspects)
- Dependency graph and coupling metrics (package and bean dependencies)
- XML configuration analysis (for traditional Spring)
- Coverage gaps with priorities
- Convention violations and consistency scores
- AI context pack generation

## 📊 Example Output

### For Spring Boot Projects

Running the tools on a typical Spring Boot project shows:

- **Package structure**, **@Component/@Service/@Repository classes**, **REST endpoints**, **Lines of code**
- **Patterns detected** (REST APIs, JPA relationships, dependency injection, transaction boundaries, auto-configuration)
- **Circular dependencies** detection (package-level)
- **Test coverage** analysis with JaCoCo integration
- **Complexity metrics** - identifies refactoring targets
- **Naming consistency** - follows Java conventions
- **Configuration analysis** - application.yml, application.properties

### For Traditional Spring Projects

Running the tools on a traditional Spring project shows:

- **Package structure**, **Bean definitions (XML and annotations)**, **MVC Controllers**, **Lines of code**
- **XML Configuration analysis**:
  - Bean definitions and dependencies from applicationContext.xml
  - DispatcherServlet configuration from servlet-context.xml
  - Security configuration from spring-security.xml
  - Transaction management configuration
  - AOP aspect configurations
- **Patterns detected**:
  - Spring MVC patterns (Controllers, ViewResolvers, HandlerMappings)
  - Dependency injection (constructor vs setter, XML vs annotations)
  - Transaction boundaries (XML tx:advice and @Transactional)
  - AOP aspects (XML aop:config and @Aspect)
  - Security patterns (authentication, authorization)
- **Bean dependency graph** - visualizes wiring from XML and annotations
- **Circular bean dependencies** detection
- **Test coverage** analysis with JaCoCo integration
- **Complexity metrics** - identifies refactoring targets
- **Convention validation** - XML structure, naming consistency, best practices

---

**Status**: 🔄 **In Development** - Specification complete, ready for implementation

**Java Version**: 17+ (LTS)

**Target Frameworks**:
- Spring Framework 5.x/6.x (Traditional)
- Spring Boot 2.x/3.x
- Spring MVC, Spring Data, Spring Security, Spring AOP

**Build Tool**: Maven 3.8+ (Gradle support planned)

**Test Framework**: JUnit 5

**Configuration Support**:
- XML-based configuration (applicationContext.xml, servlet-context.xml, etc.)
- Java-based configuration (@Configuration, @Bean)
- Annotation-based configuration (@Component, @Service, @Repository)
- Spring Boot auto-configuration
- Mixed configurations (XML + annotations)
