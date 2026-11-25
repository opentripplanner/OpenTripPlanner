package org.opentripplanner.modularization.harness;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * One request sent to a running OTP, and how its response is normalized.
 *
 * <p>Scenario files are JSON: {@code api} is {@code gtfs}, {@code transmodel} or {@code info}.
 * {@code query} and {@code variables} are the GraphQL request. {@code ignore} lists path globs of
 * values that are not stable between runs. {@code sortArraysBy} names a field used to sort arrays
 * of objects whose order has no meaning. {@code unordered} lists path globs of arrays that are
 * sorted by content, for lists where the API gives no ordering guarantee.
 */
public record Scenario(
  String name,
  String api,
  String query,
  JsonNode variables,
  List<Glob> ignore,
  List<Glob> unordered,
  String sortArraysBy
) {
  static List<Scenario> loadAll(Path dir) {
    var result = new ArrayList<Scenario>();
    try (Stream<Path> files = Files.list(dir)) {
      for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
        result.add(load(f));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return result;
  }

  static Scenario load(Path file) {
    try {
      JsonNode n = Json.MAPPER.readTree(file.toFile());
      String fileName = file.getFileName().toString();
      var ignore = new ArrayList<Glob>();
      n.path("ignore").forEach(i -> ignore.add(new Glob(i.asText())));
      var unordered = new ArrayList<Glob>();
      n.path("unordered").forEach(i -> unordered.add(new Glob(i.asText())));
      return new Scenario(
        fileName.substring(0, fileName.length() - ".json".length()),
        n.path("api").asText(),
        n.path("query").asText(null),
        n.path("variables"),
        ignore,
        unordered,
        n.hasNonNull("sortArraysBy") ? n.get("sortArraysBy").asText() : null
      );
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
