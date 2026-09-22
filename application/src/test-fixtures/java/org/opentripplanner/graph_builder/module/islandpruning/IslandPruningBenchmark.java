package org.opentripplanner.graph_builder.module.islandpruning;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone (non-JUnit) benchmark for {@link IslandPruningModule}, run manually - it downloads a
 * full country OSM extract, builds a street graph from it, and times island pruning on it.
 * {@link IslandPruningModule} is CPU/memory sensitive at country scale, and that only shows up on
 * real-sized data - the unit tests only exercise small fixtures.
 * <p>
 * Run with (bump -Xmx as needed for the chosen extract):
 * <pre>
 * MAVEN_OPTS="-Xmx6g" mvn --projects application exec:java \
 *   -Dexec.mainClass="org.opentripplanner.graph_builder.module.islandpruning.IslandPruningBenchmark" \
 *   -Dexec.classpathScope=test
 * </pre>
 * The OSM extract is downloaded once into {@code -Dbenchmark.dataDir} (default:
 * {@code $TMPDIR/otp-benchmark}) and reused on subsequent runs. Override the extract with
 * {@code -Dbenchmark.osmUrl=<url>}.
 */
public class IslandPruningBenchmark {

  private static final Logger LOG = LoggerFactory.getLogger(IslandPruningBenchmark.class);

  private static final String DEFAULT_URL =
    "https://download.geofabrik.de/europe/norway-latest.osm.pbf";

  private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);

  public static void main(String[] args) throws Exception {
    File osmFile = downloadIfMissing(
      System.getProperty("benchmark.osmUrl", DEFAULT_URL),
      Path.of(
        System.getProperty(
          "benchmark.dataDir",
          System.getProperty("java.io.tmpdir", "/tmp") + "/otp-benchmark"
        )
      )
    );

    System.out.println("Building street graph from " + osmFile + " ...");
    Instant osmStart = Instant.now();
    Graph graph = IslandPruningUtils.buildStreetGraph(osmFile);
    Duration osmDuration = Duration.between(osmStart, Instant.now());

    int streetVertices = graph.getVerticesOfType(StreetVertex.class).size();
    int streetEdges = countEdges(graph);
    System.out.printf(
      "OSM module: %s, %d street vertices, %d street edges%n",
      DurationUtils.durationToStr(osmDuration),
      streetVertices,
      streetEdges
    );

    System.gc();
    long heapBefore = usedHeapBytes();

    System.out.println("Running island pruning ...");
    Instant pruneStart = Instant.now();
    IslandPruningUtils.prune(graph, IslandPruningParameters.DEFAULTS);
    Duration pruneDuration = Duration.between(pruneStart, Instant.now());

    long heapAfterPruning = usedHeapBytes();
    System.gc();
    long heapAfterGc = usedHeapBytes();

    System.out.println();
    System.out.println("==== Results ====");
    System.out.printf("OSM module:      %8s%n", DurationUtils.durationToStr(osmDuration));
    System.out.printf("Island pruning:  %8s%n", DurationUtils.durationToStr(pruneDuration));
    System.out.printf(
      "Heap during pruning:  %,d MB (before pruning) -> %,d MB (right after, pre-GC)%n",
      heapBefore / 1_000_000,
      heapAfterPruning / 1_000_000
    );
    System.out.printf("Heap after forced GC post-pruning: %,d MB%n", heapAfterGc / 1_000_000);
    System.out.printf(
      "Remaining street vertices/edges after pruning: %d / %d%n",
      graph.getVerticesOfType(StreetVertex.class).size(),
      countEdges(graph)
    );
  }

  private static int countEdges(Graph graph) {
    int count = 0;
    for (StreetEdge ignored : graph.findEdges(StreetEdge.class)) {
      count++;
    }
    return count;
  }

  private static long usedHeapBytes() {
    return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
  }

  private static File downloadIfMissing(String url, Path dataDir) throws Exception {
    Files.createDirectories(dataDir);
    String fileName = url.substring(url.lastIndexOf('/') + 1);
    Path target = dataDir.resolve(fileName);

    if (Files.exists(target) && Files.size(target) > 0) {
      System.out.printf(
        "Using cached OSM extract at %s (%,d MB)%n",
        target,
        Files.size(target) / 1_000_000
      );
      return target.toFile();
    }

    System.out.println("Downloading " + url + " to " + target + " ...");
    Path tmp = dataDir.resolve(fileName + ".part");
    try (var clientFactory = new OtpHttpClientFactory()) {
      var client = clientFactory.create(LOG);
      try (
        InputStream in = client.getAsInputStream(
          URI.create(url),
          DOWNLOAD_TIMEOUT,
          HttpHeaders.empty()
        )
      ) {
        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    System.out.printf("Download complete: %,d MB%n", Files.size(target) / 1_000_000);
    return target.toFile();
  }
}
