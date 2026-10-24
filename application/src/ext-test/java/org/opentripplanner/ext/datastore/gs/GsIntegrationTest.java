package org.opentripplanner.ext.datastore.gs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.datastore.gs.GsHelper.toUri;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.api.GsParameters;
import org.opentripplanner.ext.datastore.gs.config.GsConfig;

/**
 * This is a manual integration test to test the Google Cloud Storage integration. To set up the
 * test you need to provide a service credential file and a bucket name. For all test to pass there
 * also need to be a gtfs.zip file in your store. Edit the {@link #CREDENTIALS_FILE},
 * {@link #BUCKET_NAME} and {@link #GTFS_URI} to make the test run.
 * <p>
 * This test comed in handy, not only to verify the implementation, but also to verify that your
 * credentials have the proper rights. If the test run, then OTP should also run with the same
 * credentials.
 */
@Disabled(
  "This test is a manual integration test, because it require an Google Cloud Store to run."
)
public class GsIntegrationTest {

  private static final String CREDENTIALS_FILE =
    "<Insert path to local Google Service Credential file here>";
  private static final String HOST = "<Insert host here>";
  private static final String BUCKET_NAME = "<Insert bucket name here>";
  private static final URI GTFS_URI = toUri(BUCKET_NAME, "gtfs.zip");
  private static final URI LARGE_OBJECT_URI = toUri(BUCKET_NAME, "graph.obj");
  private static final String DATA = "{ \"key\" : \"data\" }";

  private static final GsParameters GS_PARAMETERS = new GsConfig(HOST, CREDENTIALS_FILE);
  private GsDataSourceRepository repo;

  @BeforeEach
  public void setUp() {
    // Open a repository
    repo = new GsDataSourceRepository(GS_PARAMETERS);
    repo.open();
  }

  @Test
  public void testGsDirectory() throws Exception {
    // Get a virtual directory
    URI dirUri = toUri(BUCKET_NAME, "my-test-dir");
    CompositeDataSource dir = repo.findCompositeSource(dirUri, FileType.REPORT);

    assertEquals(dirUri.toString(), dir.path());
    assertEquals(FileType.REPORT, dir.type());
    assertEquals("my-test-dir", dir.name());
    assertTrue(dir.isWritable());

    Collection<DataSource> content;

    // Add at least one file
    writeDataToDataSource(dir.entry("a.txt"));

    // Assert content have at least one file
    content = dir.content();
    assertTrue(dir.exists());
    assertTrue(content.size() > 0, content.toString());
    // And the new file have the expected content
    assertEquals(DATA, new String(dir.entry("a.txt").asInputStream().readAllBytes(), UTF_8));

    // Delete all files in dir
    dir.delete();

    // Verify there is no data
    assertFalse(dir.exists());
    content = dir.content();
    assertEquals(0, content.size(), content.toString());

    // Write a file
    writeDataToDataSource(dir.entry("b.txt"));

    content = dir.content();
    // Assert file is moved
    assertEquals(1, content.size(), content.toString());
    assertTrue(content.toString().contains("my-test-dir/b.txt"), content.toString());

    // Cleanup
    dir.delete();
    dir.close();
  }

  @Test
  public void testGsDataSource() throws IOException {
    String tempDir = "temp-dir";

    // Make sure we start with an empty folder
    cleanUpDir(tempDir);

    // Create on new file
    URI dsUri = toUri(BUCKET_NAME, tempDir + "/ds.txt");
    DataSource ds = repo.findSource(dsUri, FileType.UNKNOWN);

    //assertFalse(ds.exists());
    assertEquals(tempDir + "/ds.txt", ds.name());
    Assertions.assertEquals(GsHelper.toUriString(BUCKET_NAME, tempDir + "/ds.txt"), ds.path());
    assertEquals(FileType.UNKNOWN, ds.type());
    assertTrue(ds.isWritable());
    assertFalse(ds.exists());

    // Create a new file
    writeDataToDataSource(ds);

    // Retrieve the new created file
    ds = repo.findSource(dsUri, FileType.UNKNOWN);

    // Validate the file
    assertTrue(ds.exists());
    assertEquals("temp-dir/ds.txt", ds.name());
    assertEquals("gs://" + BUCKET_NAME + "/temp-dir/ds.txt", ds.path());
    assertEquals(FileType.UNKNOWN, ds.type());
    assertTrue(ds.lastModified() > 0, "LastModified: " + ds.lastModified());
    assertTrue(ds.size() > 0, "Size: " + ds.size());
    assertTrue(ds.isWritable());
    assertEquals(DATA, new String(ds.asInputStream().readAllBytes(), UTF_8));

    // Cleanup
    cleanUpDir(tempDir);
  }

  @Test
  //@Ignore("This test is a manual test, because it require an Google Cloud Store to run.")
  public void testReadingZipFile() throws Exception {
    CompositeDataSource ds = repo.findCompositeSource(GTFS_URI, FileType.GTFS);

    DataSource stops = ds.entry("stops.txt");
    String text = new String(stops.asInputStream().readAllBytes(), UTF_8);
    assertTrue(text.contains("stop"), text);
  }

  /**
   * Verify against a real bucket that reading a large object as parallel byte-ranges returns
   * exactly the same bytes as the sequential path, and report the wall time of each so the effect
   * can be seen. Point {@link #LARGE_OBJECT_URI} at an object of at least a few hundred megabytes.
   */
  @Test
  public void testParallelRangeReadMatchesSequentialRead() throws Exception {
    var sequential = new GsDataSourceRepository(new GsConfig(HOST, CREDENTIALS_FILE, 0, 16, 32));
    var parallel = new GsDataSourceRepository(new GsConfig(HOST, CREDENTIALS_FILE, 4, 16, 32));
    sequential.open();
    parallel.open();

    byte[] sequentialDigest = digestOf(sequential, "sequential");
    byte[] parallelDigest = digestOf(parallel, "parallel");

    Assertions.assertArrayEquals(sequentialDigest, parallelDigest);
  }

  private byte[] digestOf(GsDataSourceRepository repo, String label) throws Exception {
    DataSource source = repo.findSource(LARGE_OBJECT_URI, FileType.UNKNOWN);
    var digest = java.security.MessageDigest.getInstance("SHA-256");
    long start = System.nanoTime();
    long bytes = 0;
    byte[] buffer = new byte[1 << 16];
    try (var in = source.asInputStream()) {
      int n;
      while ((n = in.read(buffer)) > 0) {
        digest.update(buffer, 0, n);
        bytes += n;
      }
    }
    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.printf(
      "%s: %d bytes in %d ms (%.1f MB/s)%n",
      label,
      bytes,
      ms,
      bytes / 1_048_576.0 / (ms / 1000.0)
    );
    return digest.digest();
  }

  private void cleanUpDir(String dir) {
    CompositeDataSource tempdir = repo.findCompositeSource(
      toUri(BUCKET_NAME, dir),
      FileType.REPORT
    );
    tempdir.delete();
  }

  private void writeDataToDataSource(DataSource entry) throws IOException {
    try (OutputStream output = entry.asOutputStream()) {
      output.write(DATA.getBytes(UTF_8));
      output.flush();
    }
  }
}
