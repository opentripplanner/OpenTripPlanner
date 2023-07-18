package org.opentripplanner.datastore.https;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

class HttpsDataSourceRepositoryTest {

  private final HttpsDataSourceRepository subject = new HttpsDataSourceRepository();

  @Test
  void testDescription() {
    assertEquals("HTTPS", subject.description());
  }

  @Test
  void testFindUnsupportedSource() throws Exception {
    assertNull(
      subject.findSource(new URI("file:/a.txt"), FileType.UNKNOWN),
      "Expect to return null for unsupported URI"
    );
  }

  @Test
  void testFindSupportedSource() throws Exception {
    HttpsDataSourceRepository httpsDataSourceRepository = stubHttpsDataSourceRepository();
    DataSource source = httpsDataSourceRepository.findSource(
      new URI("https://server/path/to/dataset.xml"),
      FileType.UNKNOWN
    );
    assertNotNull(source);
  }

  @Test
  void testFindUnsupportedCompositeSource() throws Exception {
    assertNull(
      subject.findCompositeSource(new URI("file:/a.zip"), FileType.UNKNOWN),
      "Expect to return null for unsupported URI"
    );
  }

  @Test
  void testFindSupportedCompositeSource() throws URISyntaxException {
    HttpsDataSourceRepository httpsDataSourceRepository = stubHttpsDataSourceRepository();
    CompositeDataSource compositeSource = httpsDataSourceRepository.findCompositeSource(
      new URI("https://server/path/to/dataset.zip"),
      FileType.UNKNOWN
    );
    assertNotNull(compositeSource);
  }

  private static HttpsDataSourceRepository stubHttpsDataSourceRepository() {
    return new HttpsDataSourceRepository() {
      @Override
      protected List<Header> getHttpHeaders(URI uri) {
        return List.of();
      }
    };
  }
}
