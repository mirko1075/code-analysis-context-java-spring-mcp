package com.mcp.codeanalysis.lsp;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Bridge to Eclipse JDT Language Server functionality.
 * Provides LSP-like features using Eclipse JDT Core.
 */
public class LspBridge {
    private static final Logger logger = LoggerFactory.getLogger(LspBridge.class);

    private final Path projectRoot;

    public LspBridge(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    /**
     * Get diagnostics (compilation errors/warnings) for a Java file.
     */
    public List<Diagnostic> getDiagnostics(Path javaFile) {
        logger.info("Getting diagnostics for: {}", javaFile);

        try {
            String source = Files.readString(javaFile);
            CompilationUnit cu = parseSource(source, javaFile.toString());

            if (cu == null) {
                return Collections.emptyList();
            }

            IProblem[] problems = cu.getProblems();
            List<Diagnostic> diagnostics = new ArrayList<>();

            for (IProblem problem : problems) {
                diagnostics.add(new Diagnostic(
                    problem.getSourceLineNumber(),
                    problem.getSourceStart(),
                    problem.getSourceEnd(),
                    problem.getMessage(),
                    problem.isError() ? "ERROR" : "WARNING"
                ));
            }

            logger.info("Found {} diagnostics", diagnostics.size());
            return diagnostics;

        } catch (IOException e) {
            logger.error("Error reading file: {}", javaFile, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get hover information for a symbol at a specific position.
     */
    public HoverInfo getHoverInfo(Path javaFile, int line, int column) {
        logger.info("Getting hover info for {}:{}:{}", javaFile, line, column);

        try {
            String source = Files.readString(javaFile);
            CompilationUnit cu = parseSource(source, javaFile.toString());

            if (cu == null) {
                return null;
            }

            int position = getPosition(source, line, column);
            NodeFinder finder = new NodeFinder(cu, position, 1);
            ASTNode node = finder.getCoveredNode();

            if (node == null) {
                return null;
            }

            return extractHoverInfo(node);

        } catch (IOException e) {
            logger.error("Error reading file: {}", javaFile, e);
            return null;
        }
    }

    /**
     * Find definition of a symbol at a specific position.
     */
    public DefinitionInfo findDefinition(Path javaFile, int line, int column) {
        logger.info("Finding definition for {}:{}:{}", javaFile, line, column);

        try {
            String source = Files.readString(javaFile);
            CompilationUnit cu = parseSource(source, javaFile.toString());

            if (cu == null) {
                return null;
            }

            int position = getPosition(source, line, column);
            NodeFinder finder = new NodeFinder(cu, position, 1);
            ASTNode node = finder.getCoveredNode();

            if (node instanceof SimpleName) {
                IBinding binding = ((SimpleName) node).resolveBinding();
                if (binding != null) {
                    return extractDefinitionInfo(binding, cu);
                }
            }

            return null;

        } catch (IOException e) {
            logger.error("Error reading file: {}", javaFile, e);
            return null;
        }
    }

    /**
     * Find all references to a symbol.
     */
    public List<ReferenceInfo> findReferences(Path javaFile, int line, int column) {
        logger.info("Finding references for {}:{}:{}", javaFile, line, column);

        try {
            String source = Files.readString(javaFile);
            CompilationUnit cu = parseSource(source, javaFile.toString());

            if (cu == null) {
                return Collections.emptyList();
            }

            int position = getPosition(source, line, column);
            NodeFinder finder = new NodeFinder(cu, position, 1);
            ASTNode node = finder.getCoveredNode();

            if (!(node instanceof SimpleName)) {
                return Collections.emptyList();
            }

            SimpleName name = (SimpleName) node;
            IBinding binding = name.resolveBinding();

            if (binding == null) {
                return Collections.emptyList();
            }

            // Find all references in the same file
            List<ReferenceInfo> references = new ArrayList<>();
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName visitedName) {
                    IBinding visitedBinding = visitedName.resolveBinding();
                    if (visitedBinding != null && visitedBinding.isEqualTo(binding)) {
                        int lineNumber = cu.getLineNumber(visitedName.getStartPosition());
                        references.add(new ReferenceInfo(
                            javaFile.toString(),
                            lineNumber,
                            visitedName.getStartPosition(),
                            visitedName.getLength(),
                            visitedName.getIdentifier()
                        ));
                    }
                    return true;
                }
            });

            return references;

        } catch (IOException e) {
            logger.error("Error reading file: {}", javaFile, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get code completion suggestions at a specific position.
     */
    public List<CompletionItem> getCompletions(Path javaFile, int line, int column) {
        logger.info("Getting completions for {}:{}:{}", javaFile, line, column);

        try {
            String source = Files.readString(javaFile);
            CompilationUnit cu = parseSource(source, javaFile.toString());

            if (cu == null) {
                return Collections.emptyList();
            }

            int position = getPosition(source, line, column);
            NodeFinder finder = new NodeFinder(cu, position, 0);
            ASTNode node = finder.getCoveringNode();

            List<CompletionItem> completions = new ArrayList<>();

            // Get visible variables and methods in scope
            if (node != null) {
                ASTNode parent = node.getParent();
                while (parent != null) {
                    if (parent instanceof MethodDeclaration) {
                        MethodDeclaration method = (MethodDeclaration) parent;
                        // Add parameters as completions
                        for (Object param : method.parameters()) {
                            if (param instanceof SingleVariableDeclaration) {
                                SingleVariableDeclaration var = (SingleVariableDeclaration) param;
                                completions.add(new CompletionItem(
                                    var.getName().getIdentifier(),
                                    "Parameter",
                                    var.getType().toString()
                                ));
                            }
                        }
                        break;
                    }
                    parent = parent.getParent();
                }
            }

            // Add common Java keywords
            String[] keywords = {"public", "private", "protected", "static", "final",
                                "void", "return", "if", "else", "for", "while",
                                "new", "this", "super", "class", "interface"};
            for (String keyword : keywords) {
                completions.add(new CompletionItem(keyword, "Keyword", null));
            }

            return completions;

        } catch (IOException e) {
            logger.error("Error reading file: {}", javaFile, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get document symbols (outline) for a Java file.
     */
    public List<SymbolInfo> getDocumentSymbols(Path javaFile) {
        logger.info("Getting document symbols for: {}", javaFile);

        try {
            String source = Files.readString(javaFile);
            CompilationUnit cu = parseSource(source, javaFile.toString());

            if (cu == null) {
                return Collections.emptyList();
            }

            List<SymbolInfo> symbols = new ArrayList<>();

            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(TypeDeclaration node) {
                    symbols.add(new SymbolInfo(
                        node.getName().getIdentifier(),
                        node.isInterface() ? "Interface" : "Class",
                        cu.getLineNumber(node.getStartPosition()),
                        node.getStartPosition(),
                        node.getLength()
                    ));
                    return true;
                }

                @Override
                public boolean visit(MethodDeclaration node) {
                    symbols.add(new SymbolInfo(
                        node.getName().getIdentifier(),
                        "Method",
                        cu.getLineNumber(node.getStartPosition()),
                        node.getStartPosition(),
                        node.getLength()
                    ));
                    return true;
                }

                @Override
                public boolean visit(FieldDeclaration node) {
                    for (Object fragment : node.fragments()) {
                        if (fragment instanceof VariableDeclarationFragment) {
                            VariableDeclarationFragment var = (VariableDeclarationFragment) fragment;
                            symbols.add(new SymbolInfo(
                                var.getName().getIdentifier(),
                                "Field",
                                cu.getLineNumber(node.getStartPosition()),
                                node.getStartPosition(),
                                node.getLength()
                            ));
                        }
                    }
                    return true;
                }
            });

            return symbols;

        } catch (IOException e) {
            logger.error("Error reading file: {}", javaFile, e);
            return Collections.emptyList();
        }
    }

    // Helper methods

    private CompilationUnit parseSource(String source, String unitName) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setUnitName(unitName);

        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, "17");
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "17");
        options.put(JavaCore.COMPILER_COMPLIANCE, "17");
        parser.setCompilerOptions(options);

        String[] classpath = getClasspath();
        String[] sourcepath = getSourcepath();
        parser.setEnvironment(classpath, sourcepath, null, true);

        return (CompilationUnit) parser.createAST(null);
    }

    private int getPosition(String source, int line, int column) {
        String[] lines = source.split("\n");
        int position = 0;
        for (int i = 0; i < line - 1 && i < lines.length; i++) {
            position += lines[i].length() + 1; // +1 for newline
        }
        position += column - 1;
        return position;
    }

    private HoverInfo extractHoverInfo(ASTNode node) {
        if (node instanceof SimpleName) {
            SimpleName name = (SimpleName) node;
            IBinding binding = name.resolveBinding();

            if (binding instanceof IVariableBinding) {
                IVariableBinding varBinding = (IVariableBinding) binding;
                return new HoverInfo(
                    name.getIdentifier(),
                    varBinding.getType().getName(),
                    varBinding.isField() ? "Field" : "Variable"
                );
            } else if (binding instanceof IMethodBinding) {
                IMethodBinding methodBinding = (IMethodBinding) binding;
                return new HoverInfo(
                    name.getIdentifier(),
                    methodBinding.getReturnType().getName(),
                    "Method"
                );
            } else if (binding instanceof ITypeBinding) {
                ITypeBinding typeBinding = (ITypeBinding) binding;
                return new HoverInfo(
                    name.getIdentifier(),
                    typeBinding.getQualifiedName(),
                    typeBinding.isInterface() ? "Interface" : "Class"
                );
            }
        }
        return null;
    }

    private DefinitionInfo extractDefinitionInfo(IBinding binding, CompilationUnit cu) {
        if (binding instanceof IVariableBinding) {
            IVariableBinding varBinding = (IVariableBinding) binding;
            return new DefinitionInfo(
                varBinding.getName(),
                varBinding.getType().getName(),
                "Variable",
                null,
                0
            );
        } else if (binding instanceof IMethodBinding) {
            IMethodBinding methodBinding = (IMethodBinding) binding;
            return new DefinitionInfo(
                methodBinding.getName(),
                methodBinding.getReturnType().getName(),
                "Method",
                null,
                0
            );
        } else if (binding instanceof ITypeBinding) {
            ITypeBinding typeBinding = (ITypeBinding) binding;
            return new DefinitionInfo(
                typeBinding.getName(),
                typeBinding.getQualifiedName(),
                typeBinding.isInterface() ? "Interface" : "Class",
                null,
                0
            );
        }
        return null;
    }

    private String[] getClasspath() {
        // Basic classpath - could be enhanced to read from pom.xml
        return new String[0];
    }

    private String[] getSourcepath() {
        // Return source directories
        Path srcMain = projectRoot.resolve("src/main/java");
        if (Files.exists(srcMain)) {
            return new String[]{srcMain.toString()};
        }
        return new String[0];
    }

    // Data classes for LSP responses

    public static record Diagnostic(
        int line,
        int startOffset,
        int endOffset,
        String message,
        String severity
    ) {}

    public static record HoverInfo(
        String name,
        String type,
        String kind
    ) {}

    public static record DefinitionInfo(
        String name,
        String type,
        String kind,
        String filePath,
        int line
    ) {}

    public static record ReferenceInfo(
        String filePath,
        int line,
        int startOffset,
        int length,
        String text
    ) {}

    public static record CompletionItem(
        String label,
        String kind,
        String detail
    ) {}

    public static record SymbolInfo(
        String name,
        String kind,
        int line,
        int startOffset,
        int length
    ) {}
}
