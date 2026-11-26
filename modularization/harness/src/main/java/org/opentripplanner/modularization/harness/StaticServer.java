package org.opentripplanner.modularization.harness;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves the files of a directory on a local port, for updaters that only read from HTTP. The
 * file is read on every request, so a step can replace it. Any method is answered with the file,
 * because some updaters POST their request. A missing file gives 404.
 */
final class StaticServer implements AutoCloseable {

  private final HttpServer server;

  StaticServer(Path root) throws IOException {
    Files.createDirectories(root);
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", ex -> handle(root, ex));
    server.start();
  }

  /** The base URL without a trailing slash. */
  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private static void handle(Path root, HttpExchange ex) throws IOException {
    try (ex) {
      ex.getRequestBody().readAllBytes();
      String rel = ex.getRequestURI().getPath().replaceFirst("^/+", "");
      Path file = root.resolve(rel).normalize();
      if (!file.startsWith(root) || !Files.isRegularFile(file)) {
        ex.sendResponseHeaders(404, -1);
        return;
      }
      byte[] body = Files.readAllBytes(file);
      ex
        .getResponseHeaders()
        .add("Content-Type", rel.endsWith(".xml") ? "application/xml" : "application/json");
      ex.sendResponseHeaders(200, body.length);
      ex.getResponseBody().write(body);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
