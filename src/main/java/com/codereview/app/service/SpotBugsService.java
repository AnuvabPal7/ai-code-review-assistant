package com.codereview.app.service;

import com.codereview.app.dto.StaticFinding;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SpotBugsService {

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\bclass\\s+(\\w+)");
    private static final String SPOTBUGS_HOME = "D:\\tools\\spotbugs-4.8.6";

    public List<StaticFinding> analyze(File javaFile) {
        List<StaticFinding> findings = new ArrayList<>();
        Path tempDir = null;

        try {
            String code = Files.readString(javaFile.toPath(), StandardCharsets.UTF_8);
            Matcher matcher = CLASS_NAME_PATTERN.matcher(code);
            String className = matcher.find() ? matcher.group(1) : "Analyzed";

            tempDir = Files.createTempDirectory("spotbugs-check-");
            Path tempJavaFile = tempDir.resolve(className + ".java");
            Files.writeString(tempJavaFile, code, StandardCharsets.UTF_8);

            ProcessBuilder compilePb = new ProcessBuilder("javac", tempJavaFile.getFileName().toString());
            compilePb.directory(tempDir.toFile());
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            compileProcess.getInputStream().readAllBytes();
            int compileExit = compileProcess.waitFor();

            if (compileExit != 0) {
                return findings;
            }

            Path reportFile = tempDir.resolve("spotbugs-report.xml");
            String spotbugsJar = SPOTBUGS_HOME + "\\lib\\spotbugs.jar";

            ProcessBuilder sbPb = new ProcessBuilder(
                    "java", "-jar", spotbugsJar,
                    "-textui", "-xml:withMessages",
                    "-output", reportFile.toString(),
                    tempDir.toString()
            );
            sbPb.redirectErrorStream(true);
            Process sbProcess = sbPb.start();
            sbProcess.getInputStream().readAllBytes();
            sbProcess.waitFor();

            if (Files.exists(reportFile)) {
                findings.addAll(parseSpotBugsXml(reportFile));
            }

        } catch (Exception e) {
            findings.add(new StaticFinding("LOW", "SpotBugs analysis failed: " + e.getMessage(), "SPOTBUGS", 0));
        } finally {
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
        }

        return findings;
    }

    private List<StaticFinding> parseSpotBugsXml(Path reportFile) {
        List<StaticFinding> findings = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(reportFile.toFile());

            NodeList bugInstances = doc.getElementsByTagName("BugInstance");
            for (int i = 0; i < bugInstances.getLength(); i++) {
                Element bug = (Element) bugInstances.item(i);
                String priority = bug.getAttribute("priority");
                String severity = mapPriority(priority);

                String message = extractDirectChildText(bug, "LongMessage");
                if (message == null || message.isBlank()) {
                    message = extractDirectChildText(bug, "ShortMessage");
                }
                if (message == null || message.isBlank()) {
                    String bugType = bug.getAttribute("type");
                    message = bugType.isBlank() ? "SpotBugs finding" : "SpotBugs: " + bugType;
                }

                int line = 0;
                NodeList sourceLines = bug.getElementsByTagName("SourceLine");
                if (sourceLines.getLength() > 0) {
                    Element sl = (Element) sourceLines.item(0);
                    String startLine = sl.getAttribute("start");
                    if (!startLine.isBlank()) {
                        try {
                            line = Integer.parseInt(startLine);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                findings.add(new StaticFinding(severity, message, "SPOTBUGS", line));
            }
        } catch (Exception e) {
            findings.add(new StaticFinding("LOW", "Could not parse SpotBugs report: " + e.getMessage(), "SPOTBUGS", 0));
        }
        return findings;
    }

    private String extractDirectChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element el && el.getTagName().equals(tagName)) {
                return el.getTextContent();
            }
        }
        return null;
    }

    private String mapPriority(String priority) {
        return switch (priority) {
            case "1" -> "HIGH";
            case "2" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private void deleteRecursively(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}