package org.opentripplanner.gtfs.graphbuilder;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone (non-JUnit) benchmark for {@link GtfsModule}, run manually - it downloads a large,
 * country-wide GTFS feed and times reading and mapping it into the transit model. GTFS import is
 * CPU/memory sensitive at that scale, and that only shows up on real-sized data - the unit tests
 * only exercise small fixtures.
 * <p>
 * Run with (the default feed, all of Germany, needs a large heap):
 * <pre>
 * MAVEN_OPTS="-Xmx40g" mvn --projects application exec:java \
 *   -Dexec.mainClass="org.opentripplanner.gtfs.graphbuilder.GtfsModuleBenchmark" \
 *   -Dexec.classpathScope=test
 * </pre>
 * The feed is downloaded once into {@code -Dbenchmark.dataDir} (default: {@code $TMPDIR}) and
 * reused on subsequent runs. Override the feed with {@code -Dbenchmark.gtfsUrl=<url>}.
 */
public class GtfsModuleBenchmark {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsModuleBenchmark.class);

  private static final String DEFAULT_URL = "https://data.siri.rcsued.delfi.de/delfi.tidy.gtfs.zip";

  private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(10);

  public static void main(String[] args) throws Exception {
    File gtfsFile = downloadIfMissing(
      System.getProperty("benchmark.gtfsUrl", DEFAULT_URL),
      Path.of(System.getProperty("benchmark.dataDir", System.getProperty("java.io.tmpdir", "/tmp")))
    );

    var graph = new Graph();
    var transitRepository = new TransitRepository(new SiteRepository());
    var module = GtfsModuleTestFactory.forTest(
      List.of(GtfsBundleTestFactory.forTest(gtfsFile)),
      transitRepository,
      graph,
      LocalDateRange.ofUnbounded()
    );

    System.gc();
    long heapBefore = usedHeapBytes();

    System.out.println("Running GTFS module on " + gtfsFile + " ...");
    Instant start = Instant.now();
    module.buildGraph();
    Duration duration = Duration.between(start, Instant.now());

    long heapAfterImport = usedHeapBytes();
    System.gc();
    long heapAfterGc = usedHeapBytes();

    var patterns = transitRepository.getAllTripPatterns();
    long trips = patterns
      .stream()
      .mapToLong(p -> p.scheduledTripsAsStream().count())
      .sum();

    System.out.println();
    System.out.println("==== Results ====");
    System.out.printf("GTFS module:  %8s%n", DurationUtils.durationToStr(duration));
    System.out.printf(
      "Heap during import:  %,d MB (before) -> %,d MB (right after, pre-GC)%n",
      heapBefore / 1_000_000,
      heapAfterImport / 1_000_000
    );
    System.out.printf("Heap after forced GC post-import: %,d MB%n", heapAfterGc / 1_000_000);
    System.out.printf(
      "Stops: %,d, trip patterns: %,d, scheduled trips: %,d%n",
      transitRepository.getSiteRepository().listStopLocations().size(),
      patterns.size(),
      trips
    );
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
        "Using cached GTFS feed at %s (%,d MB)%n",
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
