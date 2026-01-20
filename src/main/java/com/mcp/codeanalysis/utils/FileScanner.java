package com.mcp.codeanalysis.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * Utility for scanning and filtering files in a project directory.
 * Supports glob patterns and default exclusions.
 */
public class FileScanner {
    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);

    // Default exclusion patterns (directories to skip)
    private static final Set<String> DEFAULT_EXCLUSIONS = Set.of(
            "target",           // Maven build output
            "build",            // Gradle build output
            ".git",             // Git directory
            ".svn",             // SVN directory
            ".hg",              // Mercurial directory
            ".idea",            // IntelliJ IDEA directory
            ".vscode",          // VSCode directory
            ".eclipse",         // Eclipse directory
            "node_modules",     // Node.js modules
            ".gradle",          // Gradle cache
            ".m2",              // Maven local repository
            "bin",              // Compiled binaries
            "dist",             // Distribution output
            "out",              // Output directory
            "classes",          // Compiled classes
            "test-classes"      // Test compiled classes
    );

    // Default file extensions to scan
    private static final Set<String> DEFAULT_EXTENSIONS = Set.of(
            ".java",
            ".xml",
            ".yml",
            ".yaml",
            ".properties"
    );

    private final Path rootPath;
    private final Set<String> includeGlobs;
    private final Set<String> excludeGlobs;
    private final Set<String> excludedDirs;
    private final Set<String> includedExtensions;

    /**
     * Create a FileScanner with default configuration.
     *
     * @param rootPath Root directory to scan
     */
    public FileScanner(Path rootPath) {
        this(rootPath, new HashSet<>(), new HashSet<>());
    }

    /**
     * Create a FileScanner with custom include/exclude globs.
     *
     * @param rootPath     Root directory to scan
     * @param includeGlobs Include glob patterns (e.g., "**\/*.java")
     * @param excludeGlobs Exclude glob patterns (e.g., "**\/test/**")
     */
    public FileScanner(Path rootPath, Set<String> includeGlobs, Set<String> excludeGlobs) {
        this.rootPath = rootPath;
        this.includeGlobs = includeGlobs != null ? includeGlobs : new HashSet<>();
        this.excludeGlobs = excludeGlobs != null ? excludeGlobs : new HashSet<>();
        this.excludedDirs = new HashSet<>(DEFAULT_EXCLUSIONS);
        this.includedExtensions = new HashSet<>(DEFAULT_EXTENSIONS);
    }

    /**
     * Add custom excluded directory.
     *
     * @param dirName Directory name to exclude
     */
    public void addExcludedDir(String dirName) {
        this.excludedDirs.add(dirName);
    }

    /**
     * Add custom file extension to scan.
     *
     * @param extension File extension (e.g., ".groovy")
     */
    public void addIncludedExtension(String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        this.includedExtensions.add(extension);
    }

    /**
     * Clear default excluded directories.
     */
    public void clearExcludedDirs() {
        this.excludedDirs.clear();
    }

    /**
     * Clear default included extensions.
     */
    public void clearIncludedExtensions() {
        this.includedExtensions.clear();
    }

    /**
     * Scan directory for files matching criteria.
     *
     * @return List of file paths
     */
    public List<Path> scan() throws IOException {
        if (!Files.exists(rootPath)) {
            logger.error("Root path does not exist: {}", rootPath);
            return Collections.emptyList();
        }

        if (!Files.isDirectory(rootPath)) {
            logger.error("Root path is not a directory: {}", rootPath);
            return Collections.emptyList();
        }

        List<Path> results = new ArrayList<>();

        Files.walkFileTree(rootPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();

                // Skip excluded directories
                if (excludedDirs.contains(dirName)) {
                    logger.debug("Skipping excluded directory: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                // Check exclude globs
                if (matchesExcludeGlob(dir)) {
                    logger.debug("Directory matches exclude glob: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (shouldIncludeFile(file)) {
                    results.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("Failed to visit file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        logger.info("Scanned {} - found {} files", rootPath, results.size());
        return results;
    }

    /**
     * Scan for files with specific extension.
     *
     * @param extension File extension (e.g., ".java")
     * @return List of file paths with the given extension
     */
    public List<Path> scanForExtension(String extension) throws IOException {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        final String finalExtension = extension;
        List<Path> allFiles = scan();

        return allFiles.stream()
                .filter(path -> path.toString().endsWith(finalExtension))
                .toList();
    }

    /**
     * Scan for Java source files.
     *
     * @return List of .java file paths
     */
    public List<Path> scanJavaFiles() throws IOException {
        return scanForExtension(".java");
    }

    /**
     * Scan for XML configuration files.
     *
     * @return List of .xml file paths
     */
    public List<Path> scanXmlFiles() throws IOException {
        return scanForExtension(".xml");
    }

    /**
     * Scan for YAML configuration files.
     *
     * @return List of .yml and .yaml file paths
     */
    public List<Path> scanYamlFiles() throws IOException {
        List<Path> results = new ArrayList<>();
        results.addAll(scanForExtension(".yml"));
        results.addAll(scanForExtension(".yaml"));
        return results;
    }

    /**
     * Scan for properties files.
     *
     * @return List of .properties file paths
     */
    public List<Path> scanPropertiesFiles() throws IOException {
        return scanForExtension(".properties");
    }

    /**
     * Find file by name pattern (scans all files, ignoring extension filters).
     *
     * @param pattern File name pattern (e.g., "pom.xml", "application.*", "build.gradle")
     * @return List of matching file paths
     */
    public List<Path> findFilesByName(String pattern) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        // Scan all files ignoring extension filters for name-based search
        List<Path> allFiles = scanAllFiles();

        return allFiles.stream()
                .filter(path -> matcher.matches(path.getFileName()))
                .toList();
    }

    /**
     * Scan all files in directory tree, ignoring extension filters.
     * Only applies directory exclusions and glob patterns.
     *
     * @return List of all file paths
     */
    private List<Path> scanAllFiles() throws IOException {
        if (!Files.exists(rootPath)) {
            logger.error("Root path does not exist: {}", rootPath);
            return Collections.emptyList();
        }

        if (!Files.isDirectory(rootPath)) {
            logger.error("Root path is not a directory: {}", rootPath);
            return Collections.emptyList();
        }

        List<Path> results = new ArrayList<>();

        Files.walkFileTree(rootPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();

                // Skip excluded directories
                if (excludedDirs.contains(dirName)) {
                    logger.debug("Skipping excluded directory: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                // Check exclude globs
                if (matchesExcludeGlob(dir)) {
                    logger.debug("Directory matches exclude glob: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // Check exclude globs
                if (!matchesExcludeGlob(file)) {
                    // If include globs are specified, file must match
                    if (includeGlobs.isEmpty() || matchesIncludeGlob(file)) {
                        results.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("Failed to visit file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return results;
    }

    /**
     * Find Maven pom.xml files.
     *
     * @return List of pom.xml file paths
     */
    public List<Path> findPomFiles() throws IOException {
        return findFilesByName("pom.xml");
    }

    /**
     * Find Gradle build files.
     *
     * @return List of build.gradle file paths
     */
    public List<Path> findGradleFiles() throws IOException {
        List<Path> results = new ArrayList<>();
        results.addAll(findFilesByName("build.gradle"));
        results.addAll(findFilesByName("build.gradle.kts"));
        return results;
    }

    /**
     * Find application configuration files.
     *
     * @return List of application.properties, application.yml, application.yaml
     */
    public List<Path> findApplicationConfigFiles() throws IOException {
        List<Path> results = new ArrayList<>();
        results.addAll(findFilesByName("application.properties"));
        results.addAll(findFilesByName("application.yml"));
        results.addAll(findFilesByName("application.yaml"));
        results.addAll(findFilesByName("application-*.properties"));
        results.addAll(findFilesByName("application-*.yml"));
        results.addAll(findFilesByName("application-*.yaml"));
        return results;
    }

    /**
     * Check if file should be included based on extension and globs.
     */
    private boolean shouldIncludeFile(Path file) {
        String fileName = file.getFileName().toString();

        // Check exclude globs first
        if (matchesExcludeGlob(file)) {
            return false;
        }

        // If include globs are specified, file must match at least one
        if (!includeGlobs.isEmpty()) {
            if (!matchesIncludeGlob(file)) {
                return false;
            }
        }

        // Check file extension
        boolean hasValidExtension = includedExtensions.stream()
                .anyMatch(fileName::endsWith);

        return hasValidExtension;
    }

    /**
     * Check if path matches include glob patterns.
     */
    private boolean matchesIncludeGlob(Path path) {
        for (String glob : includeGlobs) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
            if (matcher.matches(rootPath.relativize(path))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if path matches exclude glob patterns.
     */
    private boolean matchesExcludeGlob(Path path) {
        for (String glob : excludeGlobs) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
            if (matcher.matches(rootPath.relativize(path))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get scan statistics.
     *
     * @return Map of extension to file count
     */
    public Map<String, Integer> getScanStatistics() throws IOException {
        List<Path> allFiles = scan();
        Map<String, Integer> stats = new HashMap<>();

        for (Path file : allFiles) {
            String fileName = file.getFileName().toString();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                String extension = fileName.substring(dotIndex);
                stats.put(extension, stats.getOrDefault(extension, 0) + 1);
            }
        }

        return stats;
    }
}
