package org.opentripplanner.modularization.harness;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Builds a graph from the given jar and data, starts a server, sends every scenario and writes
 * the normalized responses to the output directory, one {@code <scenario>.json} file each.
 */
final class Capture {

  private Capture() {}

  static void run(
    Path jar,
    Path dataDir,
    Path scenarioDir,
    Path outDir,
    int port,
    String xmx
  ) throws IOException, InterruptedException {
    List<Scenario> scenarios = Scenario.loadAll(scenarioDir);
    Files.createDirectories(outDir);
    Path work = outDir.resolve("_work");
    try (var otp = new OtpRunner(jar, work, outDir.resolve("_logs"), port, xmx)) {
      otp.prepareData(dataDir);
      otp.buildGraph();
      otp.startServer();
      for (Scenario s : scenarios) {
        JsonNode response = send(otp, s);
        JsonNode normalized = Json.normalize(response, s.ignore(), s.sortArraysBy(), s.unordered());
        Files.writeString(
          outDir.resolve(s.name() + ".json"),
          Json.MAPPER.writeValueAsString(normalized) + "\n"
        );
        System.out.println("captured " + s.name());
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
