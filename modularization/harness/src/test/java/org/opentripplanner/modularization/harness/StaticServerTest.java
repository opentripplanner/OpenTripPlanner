package org.opentripplanner.modularization.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StaticServerTest {

  private static HttpResponse<String> send(String method, String url)
    throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(URI.create(url))
      .method(method, method.equals("POST") ? HttpRequest.BodyPublishers.ofString("{}") : HttpRequest.BodyPublishers.noBody())
      .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void servesFilesForGetAndPostAndReadsThemOnEveryRequest(@TempDir Path tmp) throws Exception {
    Files.createDirectories(tmp.resolve("a"));
    Files.writeString(tmp.resolve("a/x.json"), "one");
    try (var server = new StaticServer(tmp)) {
      assertEquals("one", send("GET", server.baseUrl() + "/a/x.json").body());
      assertEquals("one", send("POST", server.baseUrl() + "/a/x.json").body());

      Files.writeString(tmp.resolve("a/x.json"), "two");
      assertEquals("two", send("GET", server.baseUrl() + "/a/x.json").body());
    }
  }

  @Test
  void missingFilesAndPathsOutsideTheRootGiveNotFound(@TempDir Path tmp) throws Exception {
    Path root = Files.createDirectories(tmp.resolve("root"));
    Files.writeString(tmp.resolve("secret.json"), "secret");
    try (var server = new StaticServer(root)) {
      assertEquals(404, send("GET", server.baseUrl() + "/nope.json").statusCode());
      assertEquals(404, send("GET", server.baseUrl() + "/../secret.json").statusCode());
    }
  }
}
