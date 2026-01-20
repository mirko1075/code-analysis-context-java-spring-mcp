package com.mcp.codeanalysis.parsers;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parser for Maven pom.xml files.
 * Extracts project information and dependencies.
 */
public class MavenParser {
    private static final Logger logger = LoggerFactory.getLogger(MavenParser.class);

    /**
     * Parse a pom.xml file and extract Maven project information.
     *
     * @param pomFile Path to pom.xml file
     * @return MavenProject containing parsed information, or null if parsing fails
     */
    public MavenProject parsePom(Path pomFile) {
        try (FileReader reader = new FileReader(pomFile.toFile())) {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            Model model = mavenReader.read(reader);

            MavenProject project = new MavenProject();

            // Set groupId (from model or parent)
            if (model.getGroupId() != null) {
                project.setGroupId(model.getGroupId());
            } else if (model.getParent() != null) {
                project.setGroupId(model.getParent().getGroupId());
            }

            project.setArtifactId(model.getArtifactId());

            // Set version (from model or parent)
            if (model.getVersion() != null) {
                project.setVersion(model.getVersion());
            } else if (model.getParent() != null) {
                project.setVersion(model.getParent().getVersion());
            }

            project.setPackaging(model.getPackaging() != null ? model.getPackaging() : "jar");
            project.setName(model.getName());
            project.setDescription(model.getDescription());

            // Extract dependencies
            if (model.getDependencies() != null) {
                for (Dependency dep : model.getDependencies()) {
                    MavenDependency mavenDep = new MavenDependency();
                    mavenDep.setGroupId(dep.getGroupId());
                    mavenDep.setArtifactId(dep.getArtifactId());
                    mavenDep.setVersion(dep.getVersion());
                    mavenDep.setScope(dep.getScope() != null ? dep.getScope() : "compile");
                    mavenDep.setType(dep.getType() != null ? dep.getType() : "jar");
                    mavenDep.setOptional(dep.isOptional());
                    project.addDependency(mavenDep);
                }
            }

            // Extract parent information
            if (model.getParent() != null) {
                project.setParentGroupId(model.getParent().getGroupId());
                project.setParentArtifactId(model.getParent().getArtifactId());
                project.setParentVersion(model.getParent().getVersion());
            }

            logger.debug("Successfully parsed pom.xml: {} dependencies", project.getDependencies().size());
            return project;

        } catch (IOException e) {
            logger.error("Error reading pom.xml: {}", pomFile, e);
            return null;
        } catch (XmlPullParserException e) {
            logger.error("Error parsing pom.xml: {}", pomFile, e);
            return null;
        }
    }

    /**
     * Represents a Maven project parsed from pom.xml.
     */
    public static class MavenProject {
        private String groupId;
        private String artifactId;
        private String version;
        private String packaging;
        private String name;
        private String description;
        private String parentGroupId;
        private String parentArtifactId;
        private String parentVersion;
        private List<MavenDependency> dependencies;

        public MavenProject() {
            this.dependencies = new ArrayList<>();
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getPackaging() {
            return packaging;
        }

        public void setPackaging(String packaging) {
            this.packaging = packaging;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getParentGroupId() {
            return parentGroupId;
        }

        public void setParentGroupId(String parentGroupId) {
            this.parentGroupId = parentGroupId;
        }

        public String getParentArtifactId() {
            return parentArtifactId;
        }

        public void setParentArtifactId(String parentArtifactId) {
            this.parentArtifactId = parentArtifactId;
        }

        public String getParentVersion() {
            return parentVersion;
        }

        public void setParentVersion(String parentVersion) {
            this.parentVersion = parentVersion;
        }

        public List<MavenDependency> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<MavenDependency> dependencies) {
            this.dependencies = dependencies;
        }

        public void addDependency(MavenDependency dependency) {
            this.dependencies.add(dependency);
        }

        /**
         * Get dependencies by scope.
         *
         * @param scope Dependency scope (compile, test, provided, runtime)
         * @return List of dependencies with the given scope
         */
        public List<MavenDependency> getDependenciesByScope(String scope) {
            return dependencies.stream()
                    .filter(dep -> scope.equals(dep.getScope()))
                    .collect(Collectors.toList());
        }

        /**
         * Get dependencies by group ID pattern.
         *
         * @param groupIdPattern Group ID pattern (e.g., "org.springframework.*")
         * @return List of matching dependencies
         */
        public List<MavenDependency> getDependenciesByGroupId(String groupIdPattern) {
            String regex = groupIdPattern.replace(".", "\\.").replace("*", ".*");
            return dependencies.stream()
                    .filter(dep -> dep.getGroupId().matches(regex))
                    .collect(Collectors.toList());
        }

        /**
         * Check if project has a specific dependency.
         *
         * @param groupId    Group ID
         * @param artifactId Artifact ID
         * @return true if dependency exists
         */
        public boolean hasDependency(String groupId, String artifactId) {
            return dependencies.stream()
                    .anyMatch(dep -> groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId()));
        }

        /**
         * Get full Maven coordinates.
         *
         * @return groupId:artifactId:version
         */
        public String getCoordinates() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }

    /**
     * Represents a Maven dependency.
     */
    public static class MavenDependency {
        private String groupId;
        private String artifactId;
        private String version;
        private String scope;
        private String type;
        private boolean optional;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isOptional() {
            return optional;
        }

        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        /**
         * Get full Maven coordinates.
         *
         * @return groupId:artifactId:version[:scope]
         */
        public String getCoordinates() {
            String coords = groupId + ":" + artifactId + ":" + version;
            if (scope != null && !"compile".equals(scope)) {
                coords += ":" + scope;
            }
            return coords;
        }

        @Override
        public String toString() {
            return getCoordinates();
        }
    }
}
