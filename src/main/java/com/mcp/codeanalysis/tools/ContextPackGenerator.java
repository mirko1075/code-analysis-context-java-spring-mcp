package com.mcp.codeanalysis.tools;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import com.mcp.codeanalysis.utils.FileScanner;
import com.mcp.codeanalysis.utils.FrameworkDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Context pack generation tool for MCP.
 * Generates AI-optimized context packs with token budget management.
 */
public class ContextPackGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ContextPackGenerator.class);

    // Approximate tokens per character (GPT-4 tokenization: ~4 chars/token)
    private static final double CHARS_PER_TOKEN = 4.0;

    /**
     * Generate context pack for a task.
     *
     * @param task Task description
     * @param projectPath Path to project root
     * @param options Generation options
     * @return Context pack result
     */
    public ContextPackResult generate(String task, String projectPath, ContextPackOptions options) {
        Path projectRoot = Paths.get(projectPath);
        ContextPackResult result = new ContextPackResult();

        try {
            // Extract keywords from task
            List<String> keywords = extractKeywords(task);
            result.setKeywords(keywords);

            // Scan for Java files
            FileScanner fileScanner = new FileScanner(projectRoot);
            List<Path> javaFiles = fileScanner.scanJavaFiles();

            // Score files by relevance
            Map<Path, Double> fileScores = scoreFiles(javaFiles, keywords, projectRoot, options);

            // Select files within token budget
            List<FileSelection> selectedFiles = selectFiles(fileScores, options.tokenBudget);
            result.setSelectedFiles(selectedFiles);

            // Add architectural context if requested
            if (options.includeTypes.contains("arch")) {
                addArchitecturalContext(projectRoot, result, options);
            }

            // Generate output in requested format
            String output = formatOutput(result, task, projectRoot, options);
            result.setOutput(output);

            // Calculate actual token usage
            int tokenCount = estimateTokens(output);
            result.setTokenCount(tokenCount);

        } catch (Exception e) {
            logger.error("Error generating context pack: {}", projectPath, e);
        }

        return result;
    }

    /**
     * Extract keywords from task description.
     */
    private List<String> extractKeywords(String task) {
        // Simple keyword extraction - split on whitespace and filter common words
        Set<String> stopWords = Set.of(
                "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
                "of", "with", "by", "from", "as", "is", "was", "are", "were", "be",
                "have", "has", "had", "do", "does", "did", "will", "would", "should",
                "could", "may", "might", "can", "this", "that", "these", "those"
        );

        return Arrays.stream(task.toLowerCase().split("\\s+"))
                .map(word -> word.replaceAll("[^a-z0-9]", ""))
                .filter(word -> word.length() > 2 && !stopWords.contains(word))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Score files by relevance to keywords.
     */
    private Map<Path, Double> scoreFiles(List<Path> javaFiles, List<String> keywords,
                                         Path projectRoot, ContextPackOptions options) {
        Map<Path, Double> scores = new HashMap<>();
        JavaSourceParser parser = new JavaSourceParser();

        for (Path javaFile : javaFiles) {
            double score = 0.0;

            try {
                // Score based on file path
                String pathStr = javaFile.toString().toLowerCase();
                for (String keyword : keywords) {
                    if (pathStr.contains(keyword)) {
                        score += 2.0; // Path match is significant
                    }
                }

                // Score based on file content
                JavaFileInfo fileInfo = parser.parseFile(javaFile);
                if (fileInfo != null) {
                    String content = Files.readString(javaFile).toLowerCase();

                    for (String keyword : keywords) {
                        // Count keyword occurrences
                        int count = countOccurrences(content, keyword);
                        score += count * 0.5;
                    }

                    // Boost for focus packages
                    if (options.focusPackages != null) {
                        for (String focusPkg : options.focusPackages) {
                            if (fileInfo.getPackageName() != null &&
                                fileInfo.getPackageName().startsWith(focusPkg)) {
                                score += 5.0;
                            }
                        }
                    }

                    // Boost for specific types
                    for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
                        if (classInfo.getAnnotations().contains("Controller") ||
                            classInfo.getAnnotations().contains("RestController")) {
                            score += 1.0;
                        }
                        if (classInfo.getAnnotations().contains("Service")) {
                            score += 1.5;
                        }
                        if (classInfo.getAnnotations().contains("Repository")) {
                            score += 1.0;
                        }
                    }
                }

                scores.put(javaFile, score);

            } catch (IOException e) {
                logger.warn("Error scoring file: {}", javaFile, e);
            }
        }

        return scores;
    }

    /**
     * Count keyword occurrences in text.
     */
    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    /**
     * Select files within token budget.
     */
    private List<FileSelection> selectFiles(Map<Path, Double> fileScores, int tokenBudget) {
        List<FileSelection> selections = new ArrayList<>();

        // Sort files by score descending
        List<Map.Entry<Path, Double>> sortedFiles = fileScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

        int tokensUsed = 0;

        for (Map.Entry<Path, Double> entry : sortedFiles) {
            Path file = entry.getKey();
            double score = entry.getValue();

            try {
                String content = Files.readString(file);
                int fileTokens = estimateTokens(content);

                // Check if adding this file would exceed budget
                if (tokensUsed + fileTokens <= tokenBudget) {
                    FileSelection selection = new FileSelection();
                    selection.setFilePath(file.toString());
                    selection.setScore(score);
                    selection.setTokenCount(fileTokens);
                    selection.setContent(content);

                    selections.add(selection);
                    tokensUsed += fileTokens;
                } else {
                    // Budget exhausted
                    break;
                }

            } catch (IOException e) {
                logger.warn("Error reading file: {}", file, e);
            }
        }

        return selections;
    }

    /**
     * Add architectural context.
     */
    private void addArchitecturalContext(Path projectRoot, ContextPackResult result,
                                        ContextPackOptions options) {
        FrameworkDetector detector = new FrameworkDetector(projectRoot);
        FrameworkDetector.FrameworkInfo frameworkInfo = detector.detect();

        StringBuilder archContext = new StringBuilder();
        archContext.append("# Framework Detection\n\n");
        archContext.append("- Spring Boot: ").append(frameworkInfo.isSpringBoot()).append("\n");
        archContext.append("- Spring MVC: ").append(frameworkInfo.isSpringMvc()).append("\n");
        archContext.append("- Spring Data: ").append(frameworkInfo.isSpringData()).append("\n");
        archContext.append("- Spring Security: ").append(frameworkInfo.isSpringSecurity()).append("\n");

        result.setArchitecturalContext(archContext.toString());
    }

    /**
     * Format output based on requested format.
     */
    private String formatOutput(ContextPackResult result, String task, Path projectRoot,
                                ContextPackOptions options) {
        return switch (options.format) {
            case "json" -> formatAsJson(result, task, options);
            case "xml" -> formatAsXml(result, task, options);
            default -> formatAsMarkdown(result, task, options);
        };
    }

    /**
     * Format output as Markdown.
     */
    private String formatAsMarkdown(ContextPackResult result, String task, ContextPackOptions options) {
        StringBuilder md = new StringBuilder();

        md.append("# AI Context Pack\n\n");
        md.append("## Task\n\n");
        md.append(task).append("\n\n");

        md.append("## Keywords\n\n");
        md.append(String.join(", ", result.getKeywords())).append("\n\n");

        if (result.getArchitecturalContext() != null) {
            md.append(result.getArchitecturalContext()).append("\n\n");
        }

        md.append("## Selected Files\n\n");
        md.append("Total: ").append(result.getSelectedFiles().size()).append(" files\n");
        md.append("Token count: ").append(result.getTokenCount()).append(" / ")
          .append(options.tokenBudget).append("\n\n");

        for (FileSelection file : result.getSelectedFiles()) {
            md.append("### ").append(file.getFilePath()).append("\n\n");
            md.append("Relevance score: ").append(String.format("%.2f", file.getScore())).append("\n");
            md.append("Tokens: ").append(file.getTokenCount()).append("\n\n");

            if (options.includeLineNumbers) {
                md.append("```java\n");
                String[] lines = file.getContent().split("\n");
                for (int i = 0; i < lines.length; i++) {
                    md.append(String.format("%4d: %s\n", i + 1, lines[i]));
                }
                md.append("```\n\n");
            } else {
                md.append("```java\n");
                md.append(file.getContent());
                md.append("\n```\n\n");
            }
        }

        return md.toString();
    }

    /**
     * Format output as JSON.
     */
    private String formatAsJson(ContextPackResult result, String task, ContextPackOptions options) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"task\": \"").append(escapeJson(task)).append("\",\n");
        json.append("  \"keywords\": [");
        json.append(result.getKeywords().stream()
                .map(k -> "\"" + escapeJson(k) + "\"")
                .collect(Collectors.joining(", ")));
        json.append("],\n");
        json.append("  \"tokenCount\": ").append(result.getTokenCount()).append(",\n");
        json.append("  \"files\": [\n");

        List<String> fileJsons = new ArrayList<>();
        for (FileSelection file : result.getSelectedFiles()) {
            StringBuilder fileJson = new StringBuilder();
            fileJson.append("    {\n");
            fileJson.append("      \"path\": \"").append(escapeJson(file.getFilePath())).append("\",\n");
            fileJson.append("      \"score\": ").append(file.getScore()).append(",\n");
            fileJson.append("      \"tokens\": ").append(file.getTokenCount()).append(",\n");
            fileJson.append("      \"content\": \"").append(escapeJson(file.getContent())).append("\"\n");
            fileJson.append("    }");
            fileJsons.add(fileJson.toString());
        }

        json.append(String.join(",\n", fileJsons));
        json.append("\n  ]\n");
        json.append("}\n");

        return json.toString();
    }

    /**
     * Format output as XML.
     */
    private String formatAsXml(ContextPackResult result, String task, ContextPackOptions options) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<contextPack>\n");
        xml.append("  <task>").append(escapeXml(task)).append("</task>\n");
        xml.append("  <keywords>\n");
        for (String keyword : result.getKeywords()) {
            xml.append("    <keyword>").append(escapeXml(keyword)).append("</keyword>\n");
        }
        xml.append("  </keywords>\n");
        xml.append("  <tokenCount>").append(result.getTokenCount()).append("</tokenCount>\n");
        xml.append("  <files>\n");

        for (FileSelection file : result.getSelectedFiles()) {
            xml.append("    <file>\n");
            xml.append("      <path>").append(escapeXml(file.getFilePath())).append("</path>\n");
            xml.append("      <score>").append(file.getScore()).append("</score>\n");
            xml.append("      <tokens>").append(file.getTokenCount()).append("</tokens>\n");
            xml.append("      <content><![CDATA[").append(file.getContent()).append("]]></content>\n");
            xml.append("    </file>\n");
        }

        xml.append("  </files>\n");
        xml.append("</contextPack>\n");

        return xml.toString();
    }

    /**
     * Estimate token count from text.
     */
    private int estimateTokens(String text) {
        return (int) (text.length() / CHARS_PER_TOKEN);
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String escapeXml(String str) {
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }

    // DTOs

    public static class ContextPackOptions {
        public int tokenBudget = 50000;
        public List<String> includeTypes = new ArrayList<>(); // files, arch, deps, config
        public List<String> focusPackages = new ArrayList<>();
        public String format = "md"; // md, json, xml
        public boolean includeLineNumbers = true;
        public String strategy = "rel"; // rel (relevance), wide (breadth), deep (depth)

        public ContextPackOptions() {
            includeTypes.add("files");
        }
    }

    public static class ContextPackResult {
        private List<String> keywords = new ArrayList<>();
        private List<FileSelection> selectedFiles = new ArrayList<>();
        private String architecturalContext;
        private String output;
        private int tokenCount;

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public void setSelectedFiles(List<FileSelection> files) {
            this.selectedFiles = files;
        }

        public void setArchitecturalContext(String context) {
            this.architecturalContext = context;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public void setTokenCount(int count) {
            this.tokenCount = count;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public List<FileSelection> getSelectedFiles() {
            return selectedFiles;
        }

        public String getArchitecturalContext() {
            return architecturalContext;
        }

        public String getOutput() {
            return output;
        }

        public int getTokenCount() {
            return tokenCount;
        }

        public Map<String, Object> getSummary() {
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalFiles", selectedFiles.size());
            summary.put("tokenCount", tokenCount);
            summary.put("keywords", keywords);
            return summary;
        }
    }

    public static class FileSelection {
        private String filePath;
        private double score;
        private int tokenCount;
        private String content;

        public void setFilePath(String path) {
            this.filePath = path;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public void setTokenCount(int count) {
            this.tokenCount = count;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getFilePath() {
            return filePath;
        }

        public double getScore() {
            return score;
        }

        public int getTokenCount() {
            return tokenCount;
        }

        public String getContent() {
            return content;
        }
    }
}
