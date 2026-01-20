package com.mcp.codeanalysis.analyzers;

import com.mcp.codeanalysis.parsers.JavaSourceParser;
import com.mcp.codeanalysis.types.JavaFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Analyzes JPA/Hibernate entity patterns and relationships.
 */
public class JpaAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(JpaAnalyzer.class);

    private final JavaSourceParser javaParser;

    // JPA entity annotations
    private static final Set<String> ENTITY_ANNOTATIONS = Set.of("Entity", "MappedSuperclass", "Embeddable");

    // JPA relationship annotations
    private static final Set<String> RELATIONSHIP_ANNOTATIONS = Set.of(
            "OneToOne", "OneToMany", "ManyToOne", "ManyToMany"
    );

    // Fetch type patterns
    private static final String LAZY_FETCH = "FetchType.LAZY";
    private static final String EAGER_FETCH = "FetchType.EAGER";

    public JpaAnalyzer() {
        this.javaParser = new JavaSourceParser();
    }

    /**
     * Analyze JPA entities and relationships.
     *
     * @param javaFiles List of Java source files
     * @return Analysis result
     */
    public JpaAnalysisResult analyze(List<Path> javaFiles) {
        JpaAnalysisResult result = new JpaAnalysisResult();

        for (Path javaFile : javaFiles) {
            analyzeJavaFile(javaFile, result);
        }

        return result;
    }

    /**
     * Analyze a Java file for JPA patterns.
     */
    private void analyzeJavaFile(Path javaFile, JpaAnalysisResult result) {
        JavaFileInfo fileInfo = javaParser.parseFile(javaFile);
        if (fileInfo == null) {
            return;
        }

        for (JavaFileInfo.ClassInfo classInfo : fileInfo.getClasses()) {
            // Check if class is a JPA entity
            if (isEntity(classInfo)) {
                String fullClassName = fileInfo.getPackageName() + "." + classInfo.getName();
                EntityInfo entityInfo = new EntityInfo(fullClassName, classInfo.getName());

                // Analyze relationships
                analyzeRelationships(classInfo, entityInfo);

                // Check for common issues
                checkForIssues(classInfo, entityInfo);

                result.addEntity(entityInfo);
            }
        }
    }

    /**
     * Check if class is a JPA entity.
     */
    private boolean isEntity(JavaFileInfo.ClassInfo classInfo) {
        for (String annotation : classInfo.getAnnotations()) {
            if (ENTITY_ANNOTATIONS.contains(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Analyze relationships in an entity.
     */
    private void analyzeRelationships(JavaFileInfo.ClassInfo classInfo, EntityInfo entityInfo) {
        for (JavaFileInfo.FieldInfo field : classInfo.getFields()) {
            for (String annotation : field.getAnnotations()) {
                if (RELATIONSHIP_ANNOTATIONS.contains(annotation)) {
                    RelationshipInfo rel = new RelationshipInfo(
                            field.getName(),
                            field.getType(),
                            annotation
                    );

                    // Detect fetch type from field or method
                    String fetchType = detectFetchType(classInfo, field.getName());
                    rel.setFetchType(fetchType);

                    // Check if bidirectional
                    if (hasMappedBy(classInfo, field.getName())) {
                        rel.setBidirectional(true);
                    }

                    entityInfo.addRelationship(rel);

                    // Track potential N+1 queries
                    if ("EAGER".equals(fetchType) && ("OneToMany".equals(annotation) || "ManyToMany".equals(annotation))) {
                        entityInfo.addPotentialNPlusOneQuery(field.getName());
                    }
                }
            }
        }
    }

    /**
     * Detect fetch type for a field.
     */
    private String detectFetchType(JavaFileInfo.ClassInfo classInfo, String fieldName) {
        // Check field annotations for fetch type parameter
        for (JavaFileInfo.FieldInfo field : classInfo.getFields()) {
            if (field.getName().equals(fieldName)) {
                // Check annotation details for fetch parameter
                for (String annotationDetail : field.getAnnotationDetails().values()) {
                    if (annotationDetail.contains("FetchType.LAZY") || annotationDetail.contains("fetch = LAZY")) {
                        return "LAZY";
                    } else if (annotationDetail.contains("FetchType.EAGER") || annotationDetail.contains("fetch = EAGER")) {
                        return "EAGER";
                    }
                }
            }
        }

        // Default fetch types based on relationship
        return "DEFAULT";
    }

    /**
     * Check if relationship has mappedBy attribute.
     */
    private boolean hasMappedBy(JavaFileInfo.ClassInfo classInfo, String fieldName) {
        for (JavaFileInfo.FieldInfo field : classInfo.getFields()) {
            if (field.getName().equals(fieldName)) {
                // Check annotation details for mappedBy parameter
                for (String annotationDetail : field.getAnnotationDetails().values()) {
                    if (annotationDetail.contains("mappedBy")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check for common JPA issues.
     */
    private void checkForIssues(JavaFileInfo.ClassInfo classInfo, EntityInfo entityInfo) {
        // Check for missing @Id
        boolean hasId = classInfo.getFields().stream()
                .anyMatch(f -> f.getAnnotations().contains("Id"));
        if (!hasId) {
            entityInfo.addIssue("Missing @Id annotation");
        }

        // Check for missing no-arg constructor
        boolean hasNoArgConstructor = hasNoArgConstructor(classInfo);
        if (!hasNoArgConstructor) {
            entityInfo.addIssue("Missing no-argument constructor (required by JPA)");
        }

        // Check for equals/hashCode
        boolean hasEquals = classInfo.getMethods().stream()
                .anyMatch(m -> m.getName().equals("equals"));
        boolean hasHashCode = classInfo.getMethods().stream()
                .anyMatch(m -> m.getName().equals("hashCode"));

        if (!hasEquals || !hasHashCode) {
            entityInfo.addIssue("Missing equals() or hashCode() implementation");
        }
    }

    /**
     * Check if class has no-arg constructor.
     */
    private boolean hasNoArgConstructor(JavaFileInfo.ClassInfo classInfo) {
        // If no explicit constructors, Java provides default no-arg constructor
        boolean hasExplicitConstructor = classInfo.getMethods().stream()
                .anyMatch(m -> m.getName().equals(classInfo.getName()));

        if (!hasExplicitConstructor) {
            return true; // Default constructor exists
        }

        // Check if there's an explicit no-arg constructor
        return classInfo.getMethods().stream()
                .anyMatch(m -> m.getName().equals(classInfo.getName()) && m.getParameters().isEmpty());
    }

    /**
     * JPA analysis result.
     */
    public static class JpaAnalysisResult {
        private final List<EntityInfo> entities = new ArrayList<>();

        public void addEntity(EntityInfo entity) {
            entities.add(entity);
        }

        public List<EntityInfo> getEntities() {
            return new ArrayList<>(entities);
        }

        public int getEntityCount() {
            return entities.size();
        }

        public int getRelationshipCount() {
            return entities.stream()
                    .mapToInt(e -> e.getRelationships().size())
                    .sum();
        }

        public List<EntityInfo> getEntitiesWithIssues() {
            return entities.stream()
                    .filter(e -> !e.getIssues().isEmpty())
                    .toList();
        }

        public List<String> getAllPotentialNPlusOneQueries() {
            List<String> queries = new ArrayList<>();
            for (EntityInfo entity : entities) {
                for (String field : entity.getPotentialNPlusOneQueries()) {
                    queries.add(entity.getClassName() + "." + field);
                }
            }
            return queries;
        }

        @Override
        public String toString() {
            return "JpaAnalysisResult{" +
                    "entities=" + entities.size() +
                    ", relationships=" + getRelationshipCount() +
                    ", entitiesWithIssues=" + getEntitiesWithIssues().size() +
                    '}';
        }
    }

    /**
     * Information about a JPA entity.
     */
    public static class EntityInfo {
        private final String fullClassName;
        private final String className;
        private final List<RelationshipInfo> relationships = new ArrayList<>();
        private final List<String> issues = new ArrayList<>();
        private final List<String> potentialNPlusOneQueries = new ArrayList<>();

        public EntityInfo(String fullClassName, String className) {
            this.fullClassName = fullClassName;
            this.className = className;
        }

        public void addRelationship(RelationshipInfo relationship) {
            relationships.add(relationship);
        }

        public void addIssue(String issue) {
            issues.add(issue);
        }

        public void addPotentialNPlusOneQuery(String field) {
            potentialNPlusOneQueries.add(field);
        }

        public String getFullClassName() {
            return fullClassName;
        }

        public String getClassName() {
            return className;
        }

        public List<RelationshipInfo> getRelationships() {
            return new ArrayList<>(relationships);
        }

        public List<String> getIssues() {
            return new ArrayList<>(issues);
        }

        public List<String> getPotentialNPlusOneQueries() {
            return new ArrayList<>(potentialNPlusOneQueries);
        }

        @Override
        public String toString() {
            return "EntityInfo{" +
                    "className='" + className + '\'' +
                    ", relationships=" + relationships.size() +
                    ", issues=" + issues.size() +
                    '}';
        }
    }

    /**
     * Information about a JPA relationship.
     */
    public static class RelationshipInfo {
        private final String fieldName;
        private final String targetType;
        private final String relationshipType;
        private String fetchType;
        private boolean bidirectional;

        public RelationshipInfo(String fieldName, String targetType, String relationshipType) {
            this.fieldName = fieldName;
            this.targetType = targetType;
            this.relationshipType = relationshipType;
        }

        public void setFetchType(String fetchType) {
            this.fetchType = fetchType;
        }

        public void setBidirectional(boolean bidirectional) {
            this.bidirectional = bidirectional;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getTargetType() {
            return targetType;
        }

        public String getRelationshipType() {
            return relationshipType;
        }

        public String getFetchType() {
            return fetchType;
        }

        public boolean isBidirectional() {
            return bidirectional;
        }

        @Override
        public String toString() {
            return "RelationshipInfo{" +
                    "fieldName='" + fieldName + '\'' +
                    ", type=" + relationshipType +
                    ", fetch=" + fetchType +
                    ", bidirectional=" + bidirectional +
                    '}';
        }
    }
}
