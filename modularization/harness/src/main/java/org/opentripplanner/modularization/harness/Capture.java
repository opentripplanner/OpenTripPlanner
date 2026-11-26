package org.opentripplanner.modularization.harness;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * For each dataset: builds a graph from the given jar and the dataset inputs, starts a server,
 * sends every scenario and writes the normalized responses to {@code <out>/<dataset>/<scenario>.json}.
 */
final class Capture {

  private Capture() {}

  /**
   * @param only names of the datasets to run, or empty for all
   */
  static void run(
    Path jar,
    Path repoRoot,
    Path datasetsDir,
    Path outDir,
    Set<String> only,
    int port,
    String xmx
  ) throws IOException, InterruptedException {
    Files.createDirectories(outDir);
    for (Dataset dataset : Dataset.loadAll(datasetsDir)) {
      if (!only.isEmpty() && !only.contains(dataset.name())) {
        continue;
      }
      System.out.println("== dataset " + dataset.name());
      run(jar, repoRoot, dataset, outDir.resolve(dataset.name()), port, xmx);
    }
  }

  /** Build the graph of one dataset and keep the server running until the process is stopped. */
  static void serve(Path jar, Path repoRoot, Path datasetsDir, String name, int port, String xmx)
    throws IOException, InterruptedException {
    Dataset dataset = Dataset
      .loadAll(datasetsDir)
      .stream()
      .filter(d -> d.name().equals(name))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown dataset: " + name));
    Path base = Files.createTempDirectory("harness-serve-");
    try (
      var http = new StaticServer(base.resolve("work/http"));
      var otp = new OtpRunner(jar, base.resolve("work"), base.resolve("logs"), port, xmx)
    ) {
      dataset.materialize(repoRoot, base.resolve("work"), http.baseUrl());
      otp.buildGraph();
      otp.startServer();
      System.out.println("OTP is running on http://localhost:" + port + "/otp/ (logs: " + base.resolve("logs") + ")");
      Thread.currentThread().join();
    }
  }

  private static void run(
    Path jar,
    Path repoRoot,
    Dataset dataset,
    Path outDir,
    int port,
    String xmx
  ) throws IOException, InterruptedException {
    Files.createDirectories(outDir);
    Path work = outDir.resolve("_work");
    try (
      var http = new StaticServer(work.resolve("http"));
      var otp = new OtpRunner(jar, work, outDir.resolve("_logs"), port, xmx)
    ) {
      dataset.materialize(repoRoot, work, http.baseUrl());
      otp.buildGraph();
      otp.startServer();
      runScenarios(otp, dataset.name(), dataset.scenarioDir(), outDir);
      for (Dataset.Step step : dataset.steps()) {
        System.out.println("-- step " + step.name());
        dataset.applyDrops(step, repoRoot, work, http.baseUrl());
        Path phaseOut = outDir.resolve("after-" + step.name());
        Files.createDirectories(phaseOut);
        if (!step.awaitDir().isEmpty()) {
          awaitConsumed(work.resolve(step.awaitDir()));
        }
        Thread.sleep(step.settleMillis());
        if (!step.awaitDir().isEmpty()) {
          // The outcome of the file processing is part of the result
          Files.writeString(
            phaseOut.resolve("updater-files.json"),
            Json.MAPPER.writeValueAsString(fileStatuses(work.resolve(step.awaitDir()))) + "\n"
          );
        }
        runScenarios(
          otp,
          dataset.name() + "/after-" + step.name(),
          dataset.scenarioDir().resolve("after-" + step.name()),
          phaseOut
        );
      }
    }
    deleteRecursively(work);
  }

  private static void runScenarios(OtpRunner otp, String label, Path scenarioDir, Path outDir)
    throws IOException, InterruptedException {
    if (!Files.isDirectory(scenarioDir)) {
      return;
    }
    for (Scenario s : Scenario.loadAll(scenarioDir)) {
      JsonNode response = send(otp, s);
      JsonNode normalized = Json.normalize(response, s.ignore(), s.sortArraysBy(), s.unordered());
      Files.writeString(
        outDir.resolve(s.name() + ".json"),
        Json.MAPPER.writeValueAsString(normalized) + "\n"
      );
      System.out.println("captured " + label + "/" + s.name());
    }
  }

  /** Wait until no {@code .xml} or {@code .xml.inProgress} file is left in the directory. */
  private static void awaitConsumed(Path dir) throws IOException, InterruptedException {
    long deadline = System.nanoTime() + java.time.Duration.ofSeconds(90).toNanos();
    while (System.nanoTime() < deadline) {
      try (var files = Files.list(dir)) {
        boolean pending = files.anyMatch(f -> {
          String n = f.getFileName().toString();
          return n.endsWith(".xml") || n.endsWith(".xml.inProgress");
        });
        if (!pending) {
          return;
        }
      }
      Thread.sleep(200);
    }
    throw new IllegalStateException("The updater did not consume the files in " + dir);
  }

  /** The outcome of the file loader: {@code a.xml.ok -> {"a.xml": "ok"}}. */
  private static JsonNode fileStatuses(Path dir) throws IOException {
    var result = Json.object();
    try (var files = Files.list(dir)) {
      for (Path f : files.sorted().toList()) {
        String n = f.getFileName().toString();
        int i = n.indexOf(".xml.");
        result.put(i < 0 ? n : n.substring(0, i + 4), i < 0 ? "unprocessed" : n.substring(i + 5));
      }
    }
    return result;
  }

  private static JsonNode send(OtpRunner otp, Scenario s) throws IOException, InterruptedException {
    HttpResponse<String> response;
    switch (s.api()) {
      case "info" -> response = otp.get("/otp/");
      case "gtfs" -> response = otp.postJson("/otp/gtfs/v1", body(s));
      case "transmodel" -> response = otp.postJson("/otp/transmodel/v3", body(s));
      default -> throw new IllegalArgumentException(
        "Unknown api '" + s.api() + "' in scenario " + s.name()
      );
    }
    var wrapper = Json.object();
    if (response.statusCode() != 200) {
      wrapper.put("_httpStatus", response.statusCode());
    }
    try {
      JsonNode body = Json.MAPPER.readTree(response.body());
      if (response.statusCode() == 200) {
        return body;
      }
      wrapper.set("body", body);
    } catch (IOException e) {
      wrapper.put("_nonJsonBody", response.body());
    }
    return wrapper;
  }

  private static String body(Scenario s) throws IOException {
    var body = Json.object();
    body.put("query", s.query());
    if (s.variables() != null && !s.variables().isMissingNode()) {
      body.set("variables", s.variables());
    }
    return Json.MAPPER.writeValueAsString(body);
  }

  private static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (var paths = Files.walk(dir)) {
      for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
        Files.delete(p);
      }
    }
  }
}
