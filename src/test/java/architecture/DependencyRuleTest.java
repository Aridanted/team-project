package architecture;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the Clean Architecture dependency rule by scanning the import statements of every
 * main source file. Source dependencies may only point inward:
 * {@code app -> view / data_access -> interface_adapter -> use_case -> entity}.
 *
 * <p>Fully qualified names used without an import are not detected; reviewers still check those.
 */
class DependencyRuleTest {

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+)");

    /** Libraries that belong to the outer "frameworks and drivers" ring. */
    private static final List<String> FRAMEWORKS = List.of(
            "javax.swing", "java.awt", "java.io", "java.nio.file", "java.net", "java.sql",
            "javax.xml", "org.w3c", "org.xml", "org.json");

    private static final List<String> UI = List.of("javax.swing", "java.awt");

    record LayerRule(String layer, List<String> forbidden) {
        @Override
        public String toString() {
            return layer;
        }
    }

    static Stream<LayerRule> rules() {
        return Stream.of(
                new LayerRule("entity", concat(
                        List.of("use_case", "interface_adapter", "view", "data_access", "app"), FRAMEWORKS)),
                new LayerRule("use_case", concat(
                        List.of("interface_adapter", "view", "data_access", "app"), FRAMEWORKS)),
                new LayerRule("interface_adapter", concat(
                        List.of("view", "data_access", "app"), UI)),
                new LayerRule("view",
                        List.of("entity", "use_case", "data_access", "app")),
                new LayerRule("data_access", concat(
                        List.of("interface_adapter", "view", "app"), UI)));
    }

    @ParameterizedTest(name = "{0} respects the dependency rule")
    @MethodSource("rules")
    void layerImportsOnlyWhatItIsAllowedTo(LayerRule rule) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFilesIn(MAIN_SOURCES.resolve(rule.layer()))) {
            for (String imported : importsOf(file)) {
                if (rule.forbidden().stream().anyMatch(prefix -> isInPackage(imported, prefix))) {
                    violations.add(MAIN_SOURCES.relativize(file) + " imports " + imported);
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "Dependency rule violated:\n  " + String.join("\n  ", violations));
    }

    private static List<Path> javaFilesIn(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static List<String> importsOf(Path file) throws IOException {
        List<String> imports = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            Matcher matcher = IMPORT.matcher(line);
            if (matcher.find()) {
                imports.add(stripTrailingDot(matcher.group(1)));
            }
        }
        return imports;
    }

    private static boolean isInPackage(String imported, String prefix) {
        return imported.equals(prefix) || imported.startsWith(prefix + ".");
    }

    private static String stripTrailingDot(String name) {
        return name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }
}
