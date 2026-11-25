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
    try (var otp = new OtpRunner(jar, base.resolve("work"), base.resolve("logs"), port, xmx)) {
      dataset.materialize(repoRoot, base.resolve("work"));
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
    List<Scenario> scenarios = Scenario.loadAll(dataset.scenarioDir());
    Files.createDirectories(outDir);
    Path work = outDir.resolve("_work");
    try (var otp = new OtpRunner(jar, work, outDir.resolve("_logs"), port, xmx)) {
      dataset.materialize(repoRoot, work);
      otp.buildGraph();
      otp.startServer();
      for (Scenario s : scenarios) {
        JsonNode response = send(otp, s);
        JsonNode normalized = Json.normalize(response, s.ignore(), s.sortArraysBy(), s.unordered());
        Files.writeString(
          outDir.resolve(s.name() + ".json"),
          Json.MAPPER.writeValueAsString(normalized) + "\n"
        );
        System.out.println("captured " + dataset.name() + "/" + s.name());
      }
    }
    deleteRecursively(work);
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
