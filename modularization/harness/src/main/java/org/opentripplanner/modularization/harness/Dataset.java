package org.opentripplanner.modularization.harness;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A set of OTP input files, the configuration, and the scenarios to run against the graph built
 * from them. A dataset is a directory {@code datasets/<name>} with a {@code dataset.json} and a
 * {@code scenarios} directory.
 *
 * <p>{@code dataset.json}:
 * <pre>
 * {
 *   "inputs": [
 *     { "from": "path/relative/to/repo/root.osm.pbf" },
 *     { "from": "path/to/a/directory", "zipAs": "netex_x.zip" }
 *   ],
 *   "buildConfig": { ... },
 *   "routerConfig": { ... }
 * }
 * </pre>
 * Input paths are resolved against the repository root of the harness, not against the tree that
 * is tested. This way both trees get exactly the same input, also after test resources move.
 * With {@code zipAs} a directory is zipped, with {@code as} a file is renamed.
 */
record Dataset(String name, Path dir, List<Input> inputs, JsonNode buildConfig, JsonNode routerConfig) {
  record Input(String from, String as, String zipAs) {}

  Path scenarioDir() {
    return dir.resolve("scenarios");
  }

  static List<Dataset> loadAll(Path datasetsDir) {
    try (Stream<Path> dirs = Files.list(datasetsDir)) {
      return dirs
        .filter(d -> Files.exists(d.resolve("dataset.json")))
        .sorted()
        .map(Dataset::load)
        .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static Dataset load(Path dir) {
    try {
      JsonNode n = Json.MAPPER.readTree(dir.resolve("dataset.json").toFile());
      var inputs = new ArrayList<Input>();
      for (JsonNode i : n.path("inputs")) {
        inputs.add(
          new Input(
            i.path("from").asText(),
            i.hasNonNull("as") ? i.get("as").asText() : null,
            i.hasNonNull("zipAs") ? i.get("zipAs").asText() : null
          )
        );
      }
      return new Dataset(
        dir.getFileName().toString(),
        dir,
        inputs,
        n.path("buildConfig"),
        n.path("routerConfig")
      );
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Write the input files and the configuration to the work directory. */
  void materialize(Path repoRoot, Path workDir) throws IOException {
    Files.createDirectories(workDir);
    for (Input in : inputs) {
      Path src = repoRoot.resolve(in.from());
      if (!Files.exists(src)) {
        throw new IllegalStateException("Input of dataset " + name + " not found: " + src);
      }
      if (in.zipAs() != null) {
        zipDirectory(src, workDir.resolve(in.zipAs()));
      } else {
        String target = in.as() != null ? in.as() : src.getFileName().toString();
        Files.copy(src, workDir.resolve(target), StandardCopyOption.REPLACE_EXISTING);
      }
    }
    writeConfig(workDir.resolve("build-config.json"), buildConfig);
    writeConfig(workDir.resolve("router-config.json"), routerConfig);
  }

  private static void writeConfig(Path file, JsonNode config) throws IOException {
    String text = config == null || config.isMissingNode() ? "{}" : Json.MAPPER.writeValueAsString(config);
    Files.writeString(file, text);
  }

  /** Zip a directory with sorted entries and fixed timestamps, so the zip is reproducible. */
  static void zipDirectory(Path dir, Path zip) throws IOException {
    try (
      ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip));
      Stream<Path> files = Files.walk(dir)
    ) {
      for (Path f : files.filter(Files::isRegularFile).sorted().toList()) {
        var entry = new ZipEntry(dir.relativize(f).toString().replace('\\', '/'));
        entry.setTime(0);
        out.putNextEntry(entry);
        Files.copy(f, out);
        out.closeEntry();
      }
    }
  }
}
