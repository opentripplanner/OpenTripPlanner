package org.opentripplanner.gbfs.manifest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import javax.annotation.Nullable;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSDataset;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSManifest;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSVersion;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a GBFS v3 {@code manifest.json}, which lists the systems a provider publishes and the GBFS
 * versions available for each.
 * <p>
 * Shared by the two sandboxes that discover their feeds from a manifest: the vehicle rental graph
 * builder (build phase) and the vehicle rental service directory (serve phase).
 */
public class GbfsManifestLoader {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsManifestLoader.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  /**
   * Loads the manifest from a remote URL or a local {@code file:} path.
   *
   * @return the parsed manifest, or {@code null} if it could not be fetched or parsed. Failing to
   *   reach a manifest must not fail the whole graph build or server startup, so this is logged
   *   and reported to the caller rather than thrown.
   */
  @Nullable
  public static GBFSManifest loadManifest(URI url, HttpHeaders headers) {
    try (var httpClientFactory = new OtpHttpClientFactory()) {
      var manifest = httpClientFactory
        .create(LOG)
        .getAndMapAsJsonObject(url, headers, OBJECT_MAPPER, GBFSManifest.class);
      LOG.info("Loaded GBFS manifest from {}", url);
      return manifest;
    } catch (OtpHttpClientException e) {
      LOG.error("Error loading GBFS manifest from {}", url, e);
      return null;
    }
  }

  /**
   * The URL of the newest GBFS version the dataset publishes, or empty if it publishes none.
   */
  public static Optional<String> selectBestVersion(GBFSDataset dataset) {
    if (dataset.getVersions() == null || dataset.getVersions().isEmpty()) {
      return Optional.empty();
    }

    // The generated version enum is declared oldest-first, so its natural order is ascending.
    return dataset
      .getVersions()
      .stream()
      .sorted(Comparator.comparing(GBFSVersion::getVersion).reversed())
      .map(GBFSVersion::getUrl)
      .findFirst();
  }
}
