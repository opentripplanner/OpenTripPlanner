package org.opentripplanner.datastore.https;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;

class HttpsFileDataSourceTest {

  public static final String TEST_HOST = "https://localhost";
  public static final String TEST_RELATIVE_PATH = "path/to";
  public static final String TEST_NAME = "dataset.zip";
  public static final String TEST_DIRECTORY = TEST_HOST + '/' + TEST_RELATIVE_PATH;
  public static final String TEST_URI = TEST_DIRECTORY + '/' + TEST_NAME;
  public static final String TEST_URI_WITH_PARAMETERS =
    TEST_DIRECTORY + '/' + TEST_NAME + "?key=value";

  @Test
  void testNotWritable() throws URISyntaxException {
    HttpsFileDataSource httpsFileDataSource = new HttpsFileDataSource(
      new URI(TEST_URI),
      FileType.UNKNOWN,
      new HttpsDataSourceMetadata(Map.of())
    );
    assertFalse(httpsFileDataSource.isWritable());
  }

  @Test
  void testExist() throws URISyntaxException {
    HttpsFileDataSource httpsFileDataSource = new HttpsFileDataSource(
      new URI(TEST_URI),
      FileType.UNKNOWN,
      new HttpsDataSourceMetadata(Map.of())
    );
    assertTrue(httpsFileDataSource.exists());
  }

  @Test
  void testHttpsFileDataSourceConstruction() throws URISyntaxException {
    URI uri = new URI(TEST_URI);
    Instant now = Instant.now();
    Map<String, String> headers = Map.of(
      HttpHeaders.CONTENT_TYPE,
      HttpsDataSourceMetadata.CONTENT_TYPE_APPLICATION_ZIP,
      HttpHeaders.CONTENT_LENGTH,
      "1024",
      HttpHeaders.LAST_MODIFIED,
      DateUtils.formatStandardDate(Instant.now())
    );
    HttpsDataSourceMetadata metadata = new HttpsDataSourceMetadata(headers);
    HttpsFileDataSource httpsFileDataSource = new HttpsFileDataSource(
      uri,
      FileType.UNKNOWN,
      metadata
    );
    assertEquals(TEST_NAME, httpsFileDataSource.name());
    assertEquals(TEST_URI, httpsFileDataSource.path());
    assertEquals(TEST_DIRECTORY, httpsFileDataSource.directory());
    assertEquals(1024, httpsFileDataSource.size());
    assertTrue(httpsFileDataSource.httpsDataSourceMetadata().isZipContentType());
    assertEquals(
      now.truncatedTo(ChronoUnit.SECONDS).toEpochMilli(),
      httpsFileDataSource.lastModified()
    );
  }

  @Test
  void testUriWithParameters() throws URISyntaxException {
    URI uri = new URI(TEST_URI_WITH_PARAMETERS);
    HttpsDataSourceMetadata metadata = new HttpsDataSourceMetadata(Map.of());
    HttpsFileDataSource httpsFileDataSource = new HttpsFileDataSource(
      uri,
      FileType.UNKNOWN,
      metadata
    );
    assertEquals(TEST_NAME, httpsFileDataSource.name());
    assertEquals(TEST_URI_WITH_PARAMETERS, httpsFileDataSource.path());
    assertEquals(TEST_DIRECTORY, httpsFileDataSource.directory());
  }
}
