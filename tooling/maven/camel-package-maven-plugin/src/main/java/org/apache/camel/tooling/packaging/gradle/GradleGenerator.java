package org.apache.camel.tooling.packaging.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.camel.tooling.packaging.Generator;
import org.apache.camel.tooling.packaging.helpers.IOHelper;
import org.apache.camel.tooling.packaging.helpers.JSonSchemaHelper;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;

import static org.apache.camel.tooling.packaging.helpers.PackageHelper.loadText;

public class GradleGenerator extends Generator {

    Map<String, Object> properties = new HashMap<>();

    @Override
    public List<String> getClasspath() {
        return (List) properties.get("project.classpath");
    }

    @Override
    protected List<String> getCamelCoreLocations() {
        if ("camel-core".equals(getArtifactId())) {
            return getResources();
        } else {
            return getClasspath();
        }
    }

    @Override
    protected List<String> getResources() {
        return (List) properties.get("project.resources");
    }

    @Override
    protected List<String> getCompileSourceRoots() {
        return (List) properties.get("project.sources");
    }

    @Override
    protected String getOutputDirectory() {
        return getProperty("project.outputDirectory");
    }

    @Override
    protected String getGroupId() {
        return getProperty("project.groupId");
    }

    @Override
    protected String getArtifactId() {
        return getProperty("project.artifactId");
    }

    @Override
    protected String getVersion() {
        return getProperty("project.version");
    }

    @Override
    protected String getName() {
        return getProperty("project.name");
    }

    @Override
    protected String getDescription() {
        return getProperty("project.description");
    }

    @Override
    protected String getProperty(String key) {
        return properties.getOrDefault(key, "").toString();
    }

    @Override
    protected void addResourceDirectory(Path resourceDirectory) {

    }

    @Override
    protected void addCompileSourceRoot(String dir) {

    }

    @Override
    protected void refresh(Path file) {

    }

    @Override
    protected boolean isDebugEnabled() {
        return false;
    }

    @Override
    protected void debug(String message) {

    }

    @Override
    protected void info(String message) {
        System.out.println("INFO: " + message);
    }

    @Override
    protected void warn(String message) {
        System.out.println("WARN: " + message);
    }

    public Path getBasedir() {
        return ((File) properties.get("project.basedir")).toPath();
    }

    public void setBasedir(Path basedir) {
        properties.put("project.basedir", basedir);
    }

    public void setRootDir(Path rootdir) {
        properties.put("project.rootdir", rootdir);
    }

    public void setGroupId(String groupId) {
        properties.put("project.groupId", groupId);
    }

    public void setArtifactId(String artifactId) {
        properties.put("project.artifactId", artifactId);
    }

    public void setVersion(String version) {
        properties.put("project.version", version);
    }

    public void setClasspath(List<String> classpath) {
        properties.put("project.classpath", classpath);
    }

    public void setOutputDirectory(String dir) {
        properties.put("project.outputDirectory", dir);
    }

    public void setSources(List<String> sources) {
        properties.put("project.sources", sources);
    }

    public void setResources(List<String> resources) {
        properties.put("project.resources", resources);
    }

    public void processCamelCore(File sources, File resources) {
        System.setProperty("rootBuildDir", properties.get("project.rootdir") + "/build/");
        Path srcPath = sources.toPath();
        Path resPath = resources.toPath();
        prepareLegal(resPath);
        prepareServices(resPath);
        prepareModel(resPath);
        prepareComponent(resPath);
        processModelDoc(resPath);
        prepareDataFormat(resPath, resPath);
        prepareLanguage(resPath, resPath);
        processEndpoints(resPath);
        processJaxb(resPath);
        createConverter(srcPath);
        IOHelper.closeJarFileSystems();
    }

    public void processComponent(File resources) {
        System.setProperty("rootBuildDir", properties.get("project.rootdir") + "/build/");
        Path resPath = resources.toPath();
        prepareLegal(resPath);
        prepareServices(resPath);
        prepareComponent(resPath);
        prepareDataFormat(resPath, resPath);
        prepareLanguage(resPath, resPath);
        prepareOthers(resPath, resPath);
        processEndpoints(resPath);
        IOHelper.closeJarFileSystems();
    }

    private static final Pattern LABEL_PATTERN = Pattern.compile("\\\"label\\\":\\s\\\"([\\w,]+)\\\"");

    private static final int UNUSED_LABELS_WARN = 15;

    public void processCatalog(List<Path> resourceFolders, File resources) {
        Path resPath = resources.toPath();

        executeModel(resourceFolders, resPath);
        Set<String> components = executeComponents(resourceFolders, resPath);
        Set<String> dataformats = executeDataFormats(resourceFolders, resPath);
        Set<String> languages = executeLanguages(resourceFolders, resPath);
        Set<String> others = executeOthers(resourceFolders, resPath);
        executeDocuments(components, dataformats, languages, others);
        executeArchetypes();
        executeXmlSchemas();
    }

    public void executeModel(List<Path> resourceFolders, Path resPath) {

        // List models
        Set<Path> jsonFiles = resourceFolders.stream()
                .flatMap(p ->
                    Stream.of("org/apache/camel/model", "org/apache/camel/spring", "org/apache/camel/core/xml")
                        .map(p::resolve))
                .filter(Files::isDirectory)
                .flatMap(IOHelper::walk)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toSet());
        System.out.println("Found " + jsonFiles.size() + " model json files");

        Set<Path> missingLabels = new TreeSet<>();
        Set<Path> missingJavaDoc = new TreeSet<>();
        Set<Path> duplicateJsonFiles = new TreeSet<>();
        Map<String, Set<String>> usedLabels = new TreeMap<>();

        // Copy models
        copyTo(resPath, jsonFiles, duplicateJsonFiles, "model");

        for (Path file : jsonFiles) {
             String text = loadJson(file);

            // just do a basic label check
            if (text.contains("\"label\": \"\"")) {
                missingLabels.add(file);
            } else {
                getUsedLabels(usedLabels, text, asComponentName(file));
            }

            // check all the properties if they have description
            List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("properties", text, true);
            for (Map<String, String> row : rows) {
                String name = row.get("name");
                // skip checking these as they have no documentation
                if ("outputs".equals(name) || "transforms".equals(name)) {
                    continue;
                }

                String doc = row.get("description");
                if (doc == null || doc.isEmpty()) {
                    missingJavaDoc.add(file);
                    break;
                }
            }
        }

        String names = jsonFiles.stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .map(s -> s.substring(0, s.length() - ".json".length()))
                .sorted()
                .collect(Collectors.joining(NL));
        updateResource(resPath.resolve("org/apache/camel/models.properties"), "# " + GENERATED_MSG + NL + names);

        printModelsReport(jsonFiles, duplicateJsonFiles, missingLabels, usedLabels, missingJavaDoc);
    }

    protected Set<String> executeComponents(List<Path> resourceFolders, Path resPath) {
        info("Copying all Camel component json descriptors");

        // lets use sorted set/maps
        Set<Path> duplicateJsonFiles = new TreeSet<>();
        Set<Path> missingComponents = new TreeSet<>();
        Map<String, Set<String>> usedComponentLabels = new TreeMap<>();
        Set<String> usedOptionLabels = new TreeSet<>();
        Set<String> unlabeledOptions = new TreeSet<>();
        Set<Path> missingFirstVersions = new TreeSet<>();

        // find all json files in components and camel-core
        Set<Path> jsonFiles = getJsonFiles(resourceFolders, GradleGenerator::isComponentJson);
        Set<Path> componentFiles = getPropertiesFile(resourceFolders, "component.properties");

        // Check for missing components
        for (Path path : componentFiles) {
            Path root = path.getParent().resolve("../../../..");
            if (jsonFiles.stream().noneMatch(p -> p.startsWith(root))) {
                missingComponents.add(root);
            }
        }

        info("Found " + componentFiles.size() + " component.properties files");
        info("Found " + jsonFiles.size() + " component json files");

        // Copy jsons
        copyTo(resPath, jsonFiles, duplicateJsonFiles, "component");

        Set<String> alternativeSchemes = new HashSet<>();

        for (Path file : jsonFiles) {
            String text = loadJson(file);
            String name = asComponentName(file);

            // check if we have a component label as we want the components to include labels
            getUsedLabels(usedComponentLabels, text, asComponentName(file));

            // check all the component options and grab the label(s) they use
            List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("componentProperties", text, true);
            for (Map<String, String> row : rows) {
                String label = row.get("label");
                if (label != null && !label.isEmpty()) {
                    String[] parts = label.split(",");
                    Collections.addAll(usedOptionLabels, parts);
                }
            }

            // check all the endpoint options and grab the label(s) they use
            int unused = 0;
            rows = JSonSchemaHelper.parseJsonSchema("properties", text, true);
            for (Map<String, String> row : rows) {
                String label = row.get("label");
                if (label != null && !label.isEmpty()) {
                    String[] parts = label.split(",");
                    Collections.addAll(usedOptionLabels, parts);
                } else {
                    unused++;
                }
            }

            if (unused >= UNUSED_LABELS_WARN) {
                unlabeledOptions.add(name);
            }

            // remember alternative schemes
            rows = JSonSchemaHelper.parseJsonSchema("component", text, false);
            for (Map<String, String> row : rows) {
                String alternativeScheme = row.get("alternativeSchemes");
                if (alternativeScheme != null && !alternativeScheme.isEmpty()) {
                    String[] parts = alternativeScheme.split(",");
                    alternativeSchemes.addAll(Arrays.asList(parts).subList(1, parts.length));
                }
            }

            // detect missing first version
            if (rows.stream().noneMatch(row -> row.containsKey("firstVersion"))) {
                missingFirstVersions.add(file);
            }
        }

        Set<String> componentNames = getNames(jsonFiles);
        updateResource(resPath.resolve("org/apache/camel/components.properties"), "# " + GENERATED_MSG + NL
                + String.join(NL, componentNames));

        // filter out duplicate component names that are alternative scheme names
        componentNames.removeAll(alternativeSchemes);

        printComponentsReport(jsonFiles, duplicateJsonFiles, missingComponents, usedComponentLabels, usedOptionLabels, unlabeledOptions, missingFirstVersions);

        return componentNames;
    }

    protected Set<String> executeDataFormats(List<Path> resourceFolders, Path resPath) {
        info("Copying all Camel dataformat json descriptors");

        // lets use sorted set/maps
        Set<Path> duplicateJsonFiles = new TreeSet<>();
        Map<String, Set<String>> usedLabels = new TreeMap<>();
        Set<Path> missingFirstVersions = new TreeSet<>();

        // find all data formats from the components directory
        Set<Path> jsonFiles = getJsonFiles(resourceFolders, GradleGenerator::isDataformatJson);
        Set<Path> dataFormatFiles = getPropertiesFile(resourceFolders, "dataformat.properties");

        info("Found " + dataFormatFiles.size() + " dataformat.properties files");
        info("Found " + jsonFiles.size() + " dataformat json files");

        // Copy jsons
        copyTo(resPath, jsonFiles, duplicateJsonFiles, "dataformat");

        for (Path file : jsonFiles) {
            String text = loadJson(file);

            // check if we have a label as we want the data format to include labels
            getUsedLabels(usedLabels, text, asComponentName(file));

            // detect missing first version
            List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("dataformat", text, false);
            if (rows.stream().noneMatch(row -> row.containsKey("firstVersion"))) {
                missingFirstVersions.add(file);
            }
        }

        Set<String> dataformatNames = getNames(jsonFiles);
        updateResource(resPath.resolve("org/apache/camel/dataformats.properties"), "# " + GENERATED_MSG + NL
                + String.join(NL, dataformatNames));

        printDataFormatsReport(jsonFiles, duplicateJsonFiles, usedLabels, missingFirstVersions);

        return dataformatNames;
    }

    protected Set<String> executeLanguages(List<Path> resourceFolders, Path resPath) {
        info("Copying all Camel language json descriptors");

        // lets use sorted set/maps
        Set<Path> duplicateJsonFiles = new TreeSet<>();
        Map<String, Set<String>> usedLabels = new TreeMap<>();
        Set<Path> missingFirstVersions = new TreeSet<>();

        // find all data formats from the components directory
        Set<Path> jsonFiles = getJsonFiles(resourceFolders, GradleGenerator::isLanguageJson);
        Set<Path> languageFiles = getPropertiesFile(resourceFolders, "language.properties");

        info("Found " + languageFiles.size() + " language.properties files");
        info("Found " + jsonFiles.size() + " language json files");

        // Copy jsons
        copyTo(resPath, jsonFiles, duplicateJsonFiles, "language");

        for (Path file : jsonFiles) {
            String text = loadJson(file);

            // check if we have a label as we want the language to include labels
            getUsedLabels(usedLabels, text, asComponentName(file));

            // detect missing first version
            List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("language", text, false);
            if (rows.stream().noneMatch(row -> row.containsKey("firstVersion"))) {
                missingFirstVersions.add(file);
            }
        }

        Set<String> languageNames = getNames(jsonFiles);
        updateResource(resPath.resolve("org/apache/camel/languages.properties"), "# " + GENERATED_MSG + NL
                + String.join(NL, languageNames));

        printLanguagesReport(jsonFiles, duplicateJsonFiles, usedLabels, missingFirstVersions);

        return languageNames;
    }

    static Set<String> EXCLUDED = new TreeSet<>(Arrays.asList(
            "camel-core-osgi",
            "camel-core-xml",
            "camel-box",
            "camel-http-common",
            "camel-jetty",
            "camel-jetty-common",
            "camel-as2",
            "camel-linkedin",
            "camel-olingo2",
            "camel-olingo4",
            "camel-servicenow",
            "camel-salesforce",
            "camel-fhir"
    ));

    private static boolean isExcluded(Path p) {
        for (Path e : p) {
            if (EXCLUDED.contains(e.toString())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> executeOthers(List<Path> resourceFolders, Path resPath) {
        info("Copying all Camel other json descriptors");

        resourceFolders = new ArrayList<>(resourceFolders);
        resourceFolders.removeIf(GradleGenerator::isExcluded);

        // lets use sorted set/maps
        Set<Path> duplicateJsonFiles = new TreeSet<>();
        Map<String, Set<String>> usedLabels = new TreeMap<>();
        Set<Path> missingFirstVersions = new TreeSet<>();

        // find all others from the components directory
        Set<Path> jsonFiles = getJsonFiles(resourceFolders, GradleGenerator::isOtherJson);
        Set<Path> otherFiles = getPropertiesFile(resourceFolders, "other.properties");

        info("Found " + otherFiles.size() + " other.properties files");
        info("Found " + jsonFiles.size() + " other json files");

        // Copy jsons
        copyTo(resPath, jsonFiles, duplicateJsonFiles, "other");

        for (Path file : jsonFiles) {
            String text = loadJson(file);

            // check if we have a component label as we want the components to include labels
            getUsedLabels(usedLabels, text, asComponentName(file));

            // detect missing first version
            List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("other", text, false);
            if (rows.stream().noneMatch(m -> m.containsKey("firstVersion"))) {
                missingFirstVersions.add(file);
            }
        }

        Set<String> otherNames = getNames(jsonFiles);
        updateResource(resPath.resolve("org/apache/camel/others.properties"), "# " + GENERATED_MSG + NL
                + String.join(NL, otherNames));

        printOthersReport(jsonFiles, duplicateJsonFiles, usedLabels, missingFirstVersions);

        return otherNames;
    }

    protected void executeArchetypes() {
        info("Copying Archetype Catalog");

        // find the generate catalog
        File file = new File(archetypesDir, "target/classes/archetype-catalog.xml");

        // make sure to create out dir
        archetypesOutDir.mkdirs();

        if (file.exists() && file.isFile()) {
            File to = new File(archetypesOutDir, file.getName());
            try {
                copyFile(file, to);
            } catch (IOException e) {
                throw new RuntimeException("Cannot copy file from " + file + " -> " + to, e);
            }
        }
    }

    protected void executeXmlSchemas() {
        info("Copying Spring/Blueprint XML schemas");

        schemasOutDir.mkdirs();

        File file = new File(springSchemaDir, "camel-spring.xsd");
        if (file.exists() && file.isFile()) {
            File to = new File(schemasOutDir, file.getName());
            try {
                copyFile(file, to);
            } catch (IOException e) {
                throw new RuntimeException("Cannot copy file from " + file + " -> " + to, e);
            }
        }
        file = new File(blueprintSchemaDir, "camel-blueprint.xsd");
        if (file.exists() && file.isFile()) {
            File to = new File(schemasOutDir, file.getName());
            try {
                copyFile(file, to);
            } catch (IOException e) {
                throw new RuntimeException("Cannot copy file from " + file + " -> " + to, e);
            }
        }
    }

    protected void executeDocuments(Set<String> components, Set<String> dataformats, Set<String> languages, Set<String> others) {
        info("Copying all Camel documents (ascii docs)");

        // lets use sorted set/maps
        Set<Path> adocFiles = new TreeSet<>();
        Set<Path> missingAdocFiles = new TreeSet<>();
        Set<Path> duplicateAdocFiles = new TreeSet<>();

        // find all camel maven modules
        if (componentsDir != null && componentsDir.isDirectory()) {
            File[] componentFiles = componentsDir.listFiles();
            if (componentFiles != null) {
                for (File dir : componentFiles) {
                    if (dir.isDirectory() && !"target".equals(dir.getName()) && !dir.getName().startsWith(".") && !excludeDocumentDir(dir.getName())) {
                        File target = new File(dir, "src/main/docs");

                        // special for these as they are in sub dir
                        if ("camel-as2".equals(dir.getName())) {
                            target = new File(dir, "camel-as2-component/src/main/docs");
                        } else if ("camel-salesforce".equals(dir.getName())) {
                            target = new File(dir, "camel-salesforce-component/src/main/docs");
                        } else if ("camel-linkedin".equals(dir.getName())) {
                            target = new File(dir, "camel-linkedin-component/src/main/docs");
                        } else if ("camel-olingo2".equals(dir.getName())) {
                            target = new File(dir, "camel-olingo2-component/src/main/docs");
                        } else if ("camel-olingo4".equals(dir.getName())) {
                            target = new File(dir, "camel-olingo4-component/src/main/docs");
                        } else if ("camel-box".equals(dir.getName())) {
                            target = new File(dir, "camel-box-component/src/main/docs");
                        } else if ("camel-servicenow".equals(dir.getName())) {
                            target = new File(dir, "camel-servicenow-component/src/main/docs");
                        } else if ("camel-fhir".equals(dir.getName())) {
                            target = new File(dir, "camel-fhir-component/src/main/docs");
                        }

                        int before = adocFiles.size();
                        findAsciiDocFilesRecursive(target, adocFiles, new CamelAsciiDocFileFilter());
                        int after = adocFiles.size();

                        if (before == after) {
                            missingAdocFiles.add(dir);
                        }
                    }
                }
            }
        }
        if (coreDir != null && coreDir.isDirectory()) {
            File target = new File(coreDir, "src/main/docs");
            findAsciiDocFilesRecursive(target, adocFiles, new CamelAsciiDocFileFilter());
        }

        info("Found " + adocFiles.size() + " ascii document files");

        // make sure to create out dir
        documentsOutDir.mkdirs();

        // use ascii doctor to convert the adoc files to html so we have documentation in this format as well
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        int converted = 0;

        for (File file : adocFiles) {
            File to = new File(documentsOutDir, file.getName());
            if (to.exists()) {
                duplicateAdocFiles.add(to);
                warn("Duplicate document name detected: " + to);
            }
            try {
                copyFile(file, to);
            } catch (IOException e) {
                throw new RuntimeException("Cannot copy file from " + file + " -> " + to, e);
            }

            // convert adoc to html as well
            if (file.getName().endsWith(".adoc")) {
                String newName = file.getName().substring(0, file.getName().length() - 5) + ".html";
                File toHtml = new File(documentsOutDir, newName);

                debug("Converting ascii document to html -> " + toHtml);
                asciidoctor.convertFile(file, OptionsBuilder.options().toFile(toHtml));

                converted++;

                try {
                    // now fix the html file because we don't want to include certain lines
                    List<String> lines = FileUtils.readLines(toHtml, StandardCharsets.UTF_8);
                    List<String> output = new ArrayList<>();
                    for (String line : lines) {
                        // skip these lines
                        if (line.contains("% raw %") || line.contains("% endraw %")) {
                            continue;
                        }
                        output.add(line);
                    }
                    if (lines.size() != output.size()) {
                        FileUtils.writeLines(toHtml, output, false);
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        if (converted > 0) {
            info("Converted " + converted + " ascii documents to HTML");
        }

        Set<String> docs = new LinkedHashSet<>();

        File all = new File(documentsOutDir, "../docs.properties");
        try {
            FileOutputStream fos = new FileOutputStream(all, false);

            String[] names = documentsOutDir.list();
            List<String> documents = new ArrayList<>();
            // sort the names
            for (String name : names) {
                if (name.endsWith(".adoc")) {
                    // strip out .adoc from the name
                    String documentName = name.substring(0, name.length() - 5);
                    documents.add(documentName);
                }
            }

            Collections.sort(documents);
            for (String name : documents) {
                fos.write(name.getBytes());
                fos.write("\n".getBytes());

                docs.add(name);
            }

            fos.close();

        } catch (IOException e) {
            throw new RuntimeException("Error writing to file " + all);
        }

        printDocumentsReport(adocFiles, duplicateAdocFiles, missingAdocFiles);

        // find out if we have documents for each component / dataformat / languages / others
        printMissingDocumentsReport(docs, components, dataformats, languages, others);
    }

    private void printMissingDocumentsReport(Set<String> docs, Set<String> components, Set<String> dataformats, Set<String> languages, Set<String> others) {
        info("");
        info("Camel missing documents report");
        info("");

        List<String> missing = new ArrayList<>();
        for (String component : components) {
            // special for mail
            if (component.equals("imap") || component.equals("imaps") || component.equals("pop3") || component.equals("pop3s") || component.equals("smtp") || component.equals("smtps")) {
                component = "mail";
            } else if (component.equals("ftp") || component.equals("sftp") || component.equals("ftps")) {
                component = "ftp";
            }
            String name = component + "-component";
            if (!docs.contains(name) && (!component.equalsIgnoreCase("linkedin") && !component.equalsIgnoreCase("salesforce") && !component.equalsIgnoreCase("servicenow"))) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            info("");
            warn("\tMissing .adoc component documentation  : " + missing.size());
            for (String name : missing) {
                warn("\t\t" + name);
            }
        }
        missing.clear();

        for (String dataformat : dataformats) {
            // special for bindy
            if (dataformat.startsWith("bindy")) {
                dataformat = "bindy";
            }
            String name = dataformat + "-dataformat";
            if (!docs.contains(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            info("");
            warn("\tMissing .adoc dataformat documentation  : " + missing.size());
            for (String name : missing) {
                warn("\t\t" + name);
            }
        }
        missing.clear();

        for (String language : languages) {
            String name = language + "-language";
            if (!docs.contains(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            info("");
            warn("\tMissing .adoc language documentation  : " + missing.size());
            for (String name : missing) {
                warn("\t\t" + name);
            }
        }
        missing.clear();

        for (String other : others) {
            String name = other;
            if (!docs.contains(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            info("");
            warn("\tMissing .adoc other documentation  : " + missing.size());
            for (String name : missing) {
                warn("\t\t" + name);
            }
        }
        missing.clear();

        info("");
        info("================================================================================");
    }

    private void printModelsReport(Set<Path> json, Set<Path> duplicate, Set<Path> missingLabels, Map<String, Set<String>> usedLabels, Set<Path> missingJavaDoc) {
        info("================================================================================");

        info("");
        info("Camel model catalog report");
        info("");
        info("\tModels found: " + json.size());
        for (Path file : json) {
            info("\t\t" + asComponentName(file));
        }
        if (!duplicate.isEmpty()) {
            info("");
            warn("\tDuplicate models detected: " + duplicate.size());
            for (Path file : duplicate) {
                warn("\t\t" + asComponentName(file));
            }
        }
        if (!missingLabels.isEmpty()) {
            info("");
            warn("\tMissing labels detected: " + missingLabels.size());
            for (Path file : missingLabels) {
                warn("\t\t" + asComponentName(file));
            }
        }
        if (!usedLabels.isEmpty()) {
            info("");
            info("\tUsed labels: " + usedLabels.size());
            for (Map.Entry<String, Set<String>> entry : usedLabels.entrySet()) {
                info("\t\t" + entry.getKey() + ":");
                for (String name : entry.getValue()) {
                    info("\t\t\t" + name);
                }
            }
        }
        if (!missingJavaDoc.isEmpty()) {
            info("");
            warn("\tMissing javadoc on models: " + missingJavaDoc.size());
            for (Path file : missingJavaDoc) {
                warn("\t\t" + asComponentName(file));
            }
        }
        info("");
        info("================================================================================");
    }

    private void printComponentsReport(Set<Path> json, Set<Path> duplicate, Set<Path> missing, Map<String,
            Set<String>> usedComponentLabels, Set<String> usedOptionsLabels, Set<String> unusedLabels, Set<Path> missingFirstVersions) {
        info("================================================================================");
        info("");
        info("Camel component catalog report");
        info("");
        info("\tComponents found: " + json.size());
        for (Path file : json) {
            info("\t\t" + asComponentName(file));
        }
        if (!duplicate.isEmpty()) {
            info("");
            warn("\tDuplicate components detected: " + duplicate.size());
            for (Path file : duplicate) {
                warn("\t\t" + asComponentName(file));
            }
        }
        if (!usedComponentLabels.isEmpty()) {
            info("");
            info("\tUsed component labels: " + usedComponentLabels.size());
            for (Map.Entry<String, Set<String>> entry : usedComponentLabels.entrySet()) {
                info("\t\t" + entry.getKey() + ":");
                for (String name : entry.getValue()) {
                    info("\t\t\t" + name);
                }
            }
        }
        if (!usedOptionsLabels.isEmpty()) {
            info("");
            info("\tUsed component/endpoint options labels: " + usedOptionsLabels.size());
            for (String name : usedOptionsLabels) {
                info("\t\t\t" + name);
            }
        }
        if (!unusedLabels.isEmpty()) {
            info("");
            info("\tComponent with more than " + UNUSED_LABELS_WARN + " unlabelled options: " + unusedLabels.size());
            for (String name : unusedLabels) {
                info("\t\t\t" + name);
            }
        }
        if (!missing.isEmpty()) {
            info("");
            warn("\tMissing components detected: " + missing.size());
            for (Path name : missing) {
                warn("\t\t" + name.getFileName());
            }
        }
        if (!missingFirstVersions.isEmpty()) {
            info("");
            warn("\tComponents without firstVersion defined: " + missingFirstVersions.size());
            for (Path name : missingFirstVersions) {
                warn("\t\t" + name.getFileName());
            }
        }
        info("");
        info("================================================================================");
    }

    private void printDataFormatsReport(Set<Path> json, Set<Path> duplicate, Map<String, Set<String>> usedLabels, Set<Path> missingFirstVersions) {
        info("================================================================================");
        info("");
        info("Camel data format catalog report");
        info("");
        info("\tDataFormats found: " + json.size());
        for (Path file : json) {
            info("\t\t" + asComponentName(file));
        }
        if (!duplicate.isEmpty()) {
            info("");
            warn("\tDuplicate dataformat detected: " + duplicate.size());
            for (Path file : duplicate) {
                warn("\t\t" + asComponentName(file));
            }
        }
        if (!usedLabels.isEmpty()) {
            info("");
            info("\tUsed labels: " + usedLabels.size());
            for (Map.Entry<String, Set<String>> entry : usedLabels.entrySet()) {
                info("\t\t" + entry.getKey() + ":");
                for (String name : entry.getValue()) {
                    info("\t\t\t" + name);
                }
            }
        }
        if (!missingFirstVersions.isEmpty()) {
            info("");
            warn("\tDataFormats without firstVersion defined: " + missingFirstVersions.size());
            for (Path name : missingFirstVersions) {
                warn("\t\t" + name.getFileName());
            }
        }
        info("");
        info("================================================================================");
    }

    private void printLanguagesReport(Set<Path> json, Set<Path> duplicate, Map<String, Set<String>> usedLabels, Set<Path> missingFirstVersions) {
        info("================================================================================");
        info("");
        info("Camel language catalog report");
        info("");
        info("\tLanguages found: " + json.size());
        for (Path file : json) {
            info("\t\t" + asComponentName(file));
        }
        if (!duplicate.isEmpty()) {
            info("");
            warn("\tDuplicate language detected: " + duplicate.size());
            for (Path file : duplicate) {
                warn("\t\t" + asComponentName(file));
            }
        }
        if (!usedLabels.isEmpty()) {
            info("");
            info("\tUsed labels: " + usedLabels.size());
            for (Map.Entry<String, Set<String>> entry : usedLabels.entrySet()) {
                info("\t\t" + entry.getKey() + ":");
                for (String name : entry.getValue()) {
                    info("\t\t\t" + name);
                }
            }
        }
        if (!missingFirstVersions.isEmpty()) {
            info("");
            warn("\tLanguages without firstVersion defined: " + missingFirstVersions.size());
            for (Path name : missingFirstVersions) {
                warn("\t\t" + name.getFileName());
            }
        }
        info("");
        info("================================================================================");
    }

    private void printOthersReport(Set<Path> json, Set<Path> duplicate, Map<String, Set<String>> usedLabels, Set<Path> missingFirstVersions) {
        info("================================================================================");
        info("");
        info("Camel other catalog report");
        info("");
        info("\tOthers found: " + json.size());
        for (Path file : json) {
            info("\t\t" + asComponentName(file));
        }
        if (!duplicate.isEmpty()) {
            info("");
            warn("\tDuplicate other detected: " + duplicate.size());
            for (Path file : duplicate) {
                warn("\t\t" + asComponentName(file));
            }
        }
        if (!usedLabels.isEmpty()) {
            info("");
            info("\tUsed labels: " + usedLabels.size());
            for (Map.Entry<String, Set<String>> entry : usedLabels.entrySet()) {
                info("\t\t" + entry.getKey() + ":");
                for (String name : entry.getValue()) {
                    info("\t\t\t" + name);
                }
            }
        }
        if (!missingFirstVersions.isEmpty()) {
            info("");
            warn("\tOthers without firstVersion defined: " + missingFirstVersions.size());
            for (Path name : missingFirstVersions) {
                warn("\t\t" + name.getFileName());
            }
        }
        info("");
        info("================================================================================");
    }

    private void printDocumentsReport(Set<Path> docs, Set<Path> duplicate, Set<Path> missing) {
        info("================================================================================");
        info("");
        info("Camel document catalog report");
        info("");
        info("\tDocuments found: " + docs.size());
        for (Path file : docs) {
            info("\t\t" + asComponentName(file));
        }
        if (!duplicate.isEmpty()) {
            info("");
            warn("\tDuplicate document detected: " + duplicate.size());
            for (Path file : duplicate) {
                warn("\t\t" + asComponentName(file));
            }
        }
        info("");
        if (!missing.isEmpty()) {
            info("");
            warn("\tMissing document detected: " + missing.size());
            for (Path name : missing) {
                warn("\t\t" + name.getFileName());
            }
        }
        info("");
        info("================================================================================");
    }

    private void copyTo(Path resPath, Set<Path> jsonFiles, Set<Path> duplicateJsonFiles, String type) {
        try {
            Path modelsOutDir = resPath.resolve("org/apache/camel/" + type + "s");
            deleteRecursive(modelsOutDir);
            Files.createDirectories(modelsOutDir);
            for (Path p : jsonFiles) {
                Path to = modelsOutDir.resolve(p.getFileName());
                if (Files.exists(to)) {
                    duplicateJsonFiles.add(to);
                    warn("Duplicate " + type + " name detected: " + to);
                }
                Files.copy(p, modelsOutDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to process " + type + "s", e);
        }
    }

    private Set<Path> getPropertiesFile(List<Path> resourceFolders, String name) {
        return resourceFolders.stream()
                .map(p -> p.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL + name))
                .filter(Files::isRegularFile)
                .collect(Collectors.toSet());
    }

    private Set<Path> getJsonFiles(List<Path> resourceFolders, Predicate<Path> filter) {
        return resourceFolders.stream()
                .flatMap(IOHelper::walk)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .filter(filter)
                .collect(Collectors.toSet());
    }

    private Set<String> getNames(Set<Path> jsonFiles) {
        return jsonFiles.stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .map(s -> s.substring(0, s.length() - ".json".length()))
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void getUsedLabels(Map<String, Set<String>> usedLabels, String text, String name) {
        Matcher matcher = LABEL_PATTERN.matcher(text);
        // grab the label, and remember it in the used labels
        if (matcher.find()) {
            String label = matcher.group(1);
            String[] labels = label.split(",");
            for (String s : labels) {
                usedLabels.computeIfAbsent(s, k -> new TreeSet<>()).add(name);
            }
        }
    }

    private String loadJson(Path file) {
        return IOHelper.lines(file)
                .filter(s -> !s.startsWith("//"))
                .collect(Collectors.joining(NL));
    }

    private static boolean isComponentJson(Path path) {
        return IOHelper.lines(path).anyMatch(l -> l.contains("\"kind\": \"component\""));
    }

    private static boolean isDataformatJson(Path path) {
        return IOHelper.lines(path).anyMatch(l -> l.contains("\"kind\": \"dataformat\""));
    }

    private static boolean isLanguageJson(Path path) {
        return IOHelper.lines(path).anyMatch(l -> l.contains("\"kind\": \"language\""));
    }

    private static boolean isOtherJson(Path path) {
        return IOHelper.lines(path).anyMatch(l -> l.contains("\"kind\": \"other\""));
    }

    private static void deleteRecursive(Path dir) throws IOException {
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private static String asComponentName(Path p) {
        String name = p.getFileName().toString();
        if (name.endsWith(".json") || name.endsWith(".adoc")) {
            return name.substring(0, name.length() - 5);
        }
        return name;
    }

}
