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
 *     { "from": "path/to/a/directory", "zipAs": "netex_x.zip" },
 *     { "from": "path/to/feed.json", "as": "gbfs/net/feed.json", "substitute": true }
 *   ],
 *   "buildConfig": { ... },
 *   "routerConfig": { ... },
 *   "steps": [
 *     { "name": "et-delay", "awaitDir": "siri-et", "settleMillis": 2000,
 *       "drops": [ { "from": "path/relative/to/repo/root.xml", "toDir": "siri-et" } ] }
 *   ]
 * }
 * </pre>
 * Input paths are resolved against the repository root of the harness, not against the tree that
 * is tested. This way both trees get exactly the same input, also after test resources move.
 * With {@code zipAs} a directory is zipped, with {@code as} a file is renamed or moved to a
 * sub-directory of the work directory. With {@code substitute} the text {@code ${workDir}} in the
 * file is replaced, like in the configuration. Update files in the steps can be substituted too.
 *
 * <p>The text {@code ${workDir}} in the configuration is replaced with the absolute path of the
 * work directory, so an updater can read from a directory there, e.g.
 * {@code "url": "file://${workDir}/siri-et"}.
 */
record Dataset(
  String name,
  Path dir,
  List<Input> inputs,
  JsonNode buildConfig,
  JsonNode routerConfig,
  List<Step> steps
) {
  record Input(String from, String as, String zipAs, boolean substitute) {}

  /** A file copied into a directory that an updater reads. */
  record Drop(String from, String toDir, boolean substitute) {}

  /**
   * An update step: the files are dropped into the watched directories, the harness waits until
   * the updater has consumed them ({@code .xml} files are renamed by the file loader; skipped when
   * {@code awaitDir} is empty), waits {@code settleMillis} more for the update to be applied, and
   * then runs the scenarios in {@code scenarios/after-<name>}.
   */
  record Step(String name, List<Drop> drops, String awaitDir, int settleMillis) {}

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
            i.hasNonNull("zipAs") ? i.get("zipAs").asText() : null,
            i.path("substitute").asBoolean(false)
          )
        );
      }
      var steps = new ArrayList<Step>();
      for (JsonNode s : n.path("steps")) {
        var drops = new ArrayList<Drop>();
        for (JsonNode dr : s.path("drops")) {
          drops.add(
            new Drop(
              dr.path("from").asText(),
              dr.path("toDir").asText(),
              dr.path("substitute").asBoolean(false)
            )
          );
        }
        steps.add(
          new Step(
            s.path("name").asText(),
            drops,
            s.path("awaitDir").asText(),
            s.path("settleMillis").asInt(2000)
          )
        );
      }
      return new Dataset(
        dir.getFileName().toString(),
        dir,
        inputs,
        n.path("buildConfig"),
        n.path("routerConfig"),
        steps
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
        copy(src, workDir.resolve(target), in.substitute(), workDir);
      }
    }
    for (Step step : steps) {
      for (Drop drop : step.drops()) {
        Files.createDirectories(workDir.resolve(drop.toDir()));
      }
      if (!step.awaitDir().isEmpty()) {
        Files.createDirectories(workDir.resolve(step.awaitDir()));
      }
    }
    String work = workDir.toAbsolutePath().toString();
    writeConfig(workDir.resolve("build-config.json"), buildConfig, work);
    writeConfig(workDir.resolve("router-config.json"), routerConfig, work);
  }

  /** Copy the files of a step into the directories the updaters read. */
  void applyDrops(Step step, Path repoRoot, Path workDir) throws IOException {
    for (Drop drop : step.drops()) {
      Path src = repoRoot.resolve(drop.from());
      if (!Files.exists(src)) {
        throw new IllegalStateException("Update file of step " + step.name() + " not found: " + src);
      }
      copy(src, workDir.resolve(drop.toDir()).resolve(src.getFileName()), drop.substitute(), workDir);
    }
  }

  private static void copy(Path src, Path target, boolean substitute, Path workDir)
    throws IOException {
    Files.createDirectories(target.getParent());
    if (substitute) {
      String text = Files.readString(src).replace("${workDir}", workDir.toAbsolutePath().toString());
      Files.writeString(target, text);
    } else {
      Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void writeConfig(Path file, JsonNode config, String workDir) throws IOException {
    String text = config == null || config.isMissingNode()
      ? "{}"
      : Json.MAPPER.writeValueAsString(config).replace("${workDir}", workDir);
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
