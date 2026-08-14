package org.opentripplanner.gbfs.manifest;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSDataset;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSVersion;
import org.opentripplanner.framework.io.HttpHeaders;

class GbfsManifestLoaderTest {

  private static final URI MANIFEST = Path.of("src/test/resources/gbfs/manifest.json")
    .toAbsolutePath()
    .toUri();

  @Test
  void loadsAManifestFromAFile() {
    var manifest = GbfsManifestLoader.loadManifest(MANIFEST, HttpHeaders.empty());

    assertNotNull(manifest);
    assertThat(
      manifest.getData().getDatasets().stream().map(GBFSDataset::getSystemId).toList()
    ).containsExactly("tieroslo", "duplicate-stations");
  }

  @Test
  void returnsNullWhenTheManifestCannotBeLoaded() {
    var manifest = GbfsManifestLoader.loadManifest(
      Path.of("src/test/resources/gbfs/does-not-exist.json").toAbsolutePath().toUri(),
      HttpHeaders.empty()
    );

    assertNull(manifest);
  }

  @Test
  void selectsTheNewestPublishedVersion() {
    var dataset = dataset("2.3", "v2-url", "3.0", "v3-url");

    assertThat(GbfsManifestLoader.selectBestVersion(dataset)).hasValue("v3-url");
  }

  @Test
  void selectsNoVersionWhenTheDatasetPublishesNone() {
    var dataset = new GBFSDataset();
    dataset.setSystemId("no-versions");
    dataset.setVersions(List.of());

    assertThat(GbfsManifestLoader.selectBestVersion(dataset)).isEmpty();
  }

  private static GBFSDataset dataset(String... versionAndUrl) {
    var versions = new ArrayList<GBFSVersion>();
    for (int i = 0; i < versionAndUrl.length; i += 2) {
      var version = new GBFSVersion();
      version.setVersion(GBFSVersion.Version.fromValue(versionAndUrl[i]));
      version.setUrl(versionAndUrl[i + 1]);
      versions.add(version);
    }
    var dataset = new GBFSDataset();
    dataset.setSystemId("test");
    dataset.setVersions(versions);
    return dataset;
  }
}
