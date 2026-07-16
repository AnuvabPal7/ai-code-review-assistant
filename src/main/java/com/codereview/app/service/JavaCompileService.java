package com.codereview.app.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaCompileService {

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\bclass\\s+(\\w+)");

    /**
     * Attempts to compile the given Java source using the real javac compiler.
     * Returns null if compilation succeeds, or the raw compiler error output
     * (with a plain-English note appended) if it fails.
     */
    public String checkCompiles(File sourceFile) throws IOException, InterruptedException {
        String code = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);

        Matcher matcher = CLASS_NAME_PATTERN.matcher(code);
        String className = matcher.find() ? matcher.group(1) : "CompileCheck";

        Path tempDir = Files.createTempDirectory("java-compile-check-");
        try {
            Path tempJavaFile = tempDir.resolve(className + ".java");
            Files.writeString(tempJavaFile, code, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder("javac", tempJavaFile.getFileName().toString());
            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String raw = output.isBlank() ? "Compilation failed with no error details." : output.trim();
                String friendlyNote = friendlyNoteFor(raw);
                return friendlyNote != null ? raw + "\n\n" + friendlyNote : raw;
            }
            return null;

        } finally {
            deleteRecursively(tempDir);
        }
    }

    private String friendlyNoteFor(String compilerOutput) {
        String o = compilerOutput.toLowerCase();

        if (o.contains("class, interface, enum, or record expected")) {
            return "In plain terms: Java expects every file to contain a class, interface, enum, or record definition. "
                    + "The text submitted doesn't look like valid Java source code at all - it might be plain text, "
                    + "a typo, or an incomplete snippet.";
        }
        if (o.contains("cannot find symbol")) {
            return "In plain terms: the compiler found a variable, method, or class name it doesn't recognize - "
                    + "this is often a typo, a missing import, or using something before it's declared.";
        }
        if (o.contains("';' expected")) {
            return "In plain terms: Java statements need to end with a semicolon (;) - one is likely missing near the "
                    + "line mentioned above.";
        }
        if (o.contains("reached end of file while parsing")) {
            return "In plain terms: the code is missing a closing brace (}) somewhere - Java reached the end of the "
                    + "file while still expecting more code.";
        }
        if (o.contains("illegal start of expression")) {
            return "In plain terms: there's a syntax problem at this location - often a stray symbol, misplaced "
                    + "keyword, or missing punctuation nearby.";
        }
        if (o.contains("incompatible types")) {
            return "In plain terms: a value of one type (e.g. text) is being used where a different type "
                    + "(e.g. a number) is expected.";
        }

        return "In plain terms: this code has a syntax error and could not be compiled by Java. "
                + "Double-check the line mentioned above for typos or missing punctuation.";
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