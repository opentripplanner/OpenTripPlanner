package org.opentripplanner.modularization.harness;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Builds a graph and runs a server from an OTP shaded jar. The jar is a black box. */
final class OtpRunner implements AutoCloseable {

  private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);

  private final Path jar;
  private final Path workDir;
  private final Path logDir;
  private final int port;
  private final String xmx;
  // OTP's Grizzly server hangs on the HTTP/2 upgrade request, so use HTTP/1.1 only.
  private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  private Process server;

  OtpRunner(Path jar, Path workDir, Path logDir, int port, String xmx) {
    this.jar = jar.toAbsolutePath();
    this.workDir = workDir.toAbsolutePath();
    this.logDir = logDir.toAbsolutePath();
    this.port = port;
    this.xmx = xmx;
  }

  /** Copy the input data (without any old graph) into the work dir. */
  void prepareData(Path dataDir) throws IOException {
    Files.createDirectories(workDir);
    try (Stream<Path> files = Files.list(dataDir)) {
      for (Path f : files.toList()) {
        if (Files.isRegularFile(f) && !f.getFileName().toString().equals("graph.obj")) {
          Files.copy(f, workDir.resolve(f.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  void buildGraph() throws IOException, InterruptedException {
    Process p = start("build", "--build", "--save", workDir.toString());
    if (p.waitFor() != 0) {
      throw new IllegalStateException("Graph build failed, see " + logDir.resolve("build.log"));
    }
  }

  void startServer() throws IOException, InterruptedException {
    server = start("server", "--load", "--serve", "--port", String.valueOf(port), workDir.toString());
    long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      if (!server.isAlive()) {
        throw new IllegalStateException("OTP exited, see " + logDir.resolve("server.log"));
      }
      try {
        if (get("/otp/").statusCode() == 200) {
          return;
        }
      } catch (IOException ignore) {
        // not up yet
      }
      Thread.sleep(500);
    }
    throw new IllegalStateException("OTP did not start within " + STARTUP_TIMEOUT);
  }

  HttpResponse<String> get(String path) throws IOException, InterruptedException {
    return http.send(
      HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
      HttpResponse.BodyHandlers.ofString()
    );
  }

  HttpResponse<String> postJson(String path, String body) throws IOException, InterruptedException {
    return http.send(
      HttpRequest
        .newBuilder(URI.create("http://localhost:" + port + path))
        .timeout(Duration.ofSeconds(60))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build(),
      HttpResponse.BodyHandlers.ofString()
    );
  }

  private Process start(String logName, String... otpArgs) throws IOException {
    Files.createDirectories(logDir);
    List<String> cmd = new ArrayList<>();
    cmd.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    cmd.add("-Xmx" + xmx);
    cmd.add("-jar");
    cmd.add(jar.toString());
    cmd.addAll(List.of(otpArgs));
    return new ProcessBuilder(cmd)
      .directory(workDir.toFile())
      .redirectErrorStream(true)
      .redirectOutput(logDir.resolve(logName + ".log").toFile())
      .start();
  }

  @Override
  public void close() {
    if (server != null && server.isAlive()) {
      server.destroy();
      try {
        if (!server.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
          server.destroyForcibly();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        server.destroyForcibly();
      }
    }
  }
}
