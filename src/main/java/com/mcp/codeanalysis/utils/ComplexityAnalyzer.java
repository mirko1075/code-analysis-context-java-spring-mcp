package com.mcp.codeanalysis.utils;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzer for calculating cyclomatic complexity of Java methods.
 *
 * Cyclomatic complexity is a metric that measures the number of linearly independent
 * paths through a program's source code. It was developed by Thomas J. McCabe in 1976.
 *
 * Formula: M = E - N + 2P
 * Where:
 * - E = number of edges in the control flow graph
 * - N = number of nodes in the control flow graph
 * - P = number of connected components (usually 1 for a single method)
 *
 * Simplified calculation:
 * - Start with complexity = 1 (base path)
 * - Add 1 for each decision point: if, for, while, do-while, case, catch, ?, &&, ||, ternary
 */
public class ComplexityAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ComplexityAnalyzer.class);

    /**
     * Calculate cyclomatic complexity for a method.
     *
     * @param method The method declaration to analyze
     * @return The cyclomatic complexity value
     */
    public int calculateComplexity(MethodDeclaration method) {
        if (method == null) {
            logger.warn("Cannot calculate complexity for null method");
            return 0;
        }

        ComplexityVisitor visitor = new ComplexityVisitor();
        method.accept(visitor, null);
        int complexity = visitor.getComplexity();

        logger.debug("Method {} has cyclomatic complexity: {}",
                    method.getNameAsString(), complexity);

        return complexity;
    }

    /**
     * Visitor that counts decision points in a method to calculate complexity.
     */
    private static class ComplexityVisitor extends VoidVisitorAdapter<Void> {
        private int complexity = 1; // Start with 1 (base path)

        public int getComplexity() {
            return complexity;
        }

        // If statement
        @Override
        public void visit(IfStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // For loop
        @Override
        public void visit(ForStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // Enhanced for loop (for-each)
        @Override
        public void visit(ForEachStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // While loop
        @Override
        public void visit(WhileStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // Do-while loop
        @Override
        public void visit(DoStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // Switch statement - count each case (excluding default)
        @Override
        public void visit(SwitchStmt n, Void arg) {
            // Each case adds a decision point
            complexity += n.getEntries().size();
            super.visit(n, arg);
        }

        // Switch expression (Java 14+)
        @Override
        public void visit(SwitchEntry n, Void arg) {
            // Already counted in SwitchStmt
            super.visit(n, arg);
        }

        // Catch clause (exception handling)
        @Override
        public void visit(CatchClause n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // Ternary operator (conditional expression)
        @Override
        public void visit(com.github.javaparser.ast.expr.ConditionalExpr n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // Logical AND (short-circuit evaluation)
        @Override
        public void visit(com.github.javaparser.ast.expr.BinaryExpr n, Void arg) {
            switch (n.getOperator()) {
                case AND:           // &&
                case OR:            // ||
                    complexity++;
                    break;
                default:
                    // Other binary operators don't add complexity
                    break;
            }
            super.visit(n, arg);
        }
    }

    /**
     * Get complexity level description based on complexity value.
     *
     * @param complexity The complexity value
     * @return Description of complexity level
     */
    public String getComplexityLevel(int complexity) {
        if (complexity <= 5) {
            return "low";
        } else if (complexity <= 10) {
            return "moderate";
        } else if (complexity <= 20) {
            return "high";
        } else {
            return "very-high";
        }
    }

    /**
     * Get risk assessment based on complexity value.
     *
     * @param complexity The complexity value
     * @return Risk description
     */
    public String getRiskAssessment(int complexity) {
        if (complexity <= 5) {
            return "Low risk - Simple method, easy to test and maintain";
        } else if (complexity <= 10) {
            return "Moderate risk - Reasonably complex, manageable";
        } else if (complexity <= 20) {
            return "High risk - Complex method, consider refactoring";
        } else {
            return "Very high risk - Very complex, difficult to test and maintain, refactoring recommended";
        }
    }

    /**
     * Calculate maintainability index based on complexity and lines of code.
     *
     * Simplified formula: MI = max(0, (171 - 5.2 * ln(V) - 0.23 * G - 16.2 * ln(LOC)) * 100 / 171)
     * Where:
     * - V = Halstead Volume (approximated by LOC for simplicity)
     * - G = Cyclomatic Complexity
     * - LOC = Lines of Code
     *
     * For simplicity, we use: MI = 100 - (complexity * 5) - (LOC / 10)
     *
     * @param complexity Cyclomatic complexity
     * @param linesOfCode Lines of code in the method
     * @return Maintainability index (0-100, higher is better)
     */
    public int calculateMaintainabilityIndex(int complexity, int linesOfCode) {
        if (linesOfCode <= 0) {
            return 100;
        }

        // Simplified maintainability index
        int mi = 100 - (complexity * 5) - (linesOfCode / 10);

        // Clamp to 0-100 range
        return Math.max(0, Math.min(100, mi));
    }

    /**
     * Get maintainability level description.
     *
     * @param maintainabilityIndex The maintainability index (0-100)
     * @return Description of maintainability level
     */
    public String getMaintainabilityLevel(int maintainabilityIndex) {
        if (maintainabilityIndex >= 80) {
            return "excellent";
        } else if (maintainabilityIndex >= 60) {
            return "good";
        } else if (maintainabilityIndex >= 40) {
            return "moderate";
        } else if (maintainabilityIndex >= 20) {
            return "poor";
        } else {
            return "critical";
        }
    }
}
