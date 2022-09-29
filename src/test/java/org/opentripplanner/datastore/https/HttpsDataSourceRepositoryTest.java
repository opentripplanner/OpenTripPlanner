package org.opentripplanner.datastore.https;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;

class HttpsDataSourceRepositoryTest {

  private final HttpsDataSourceRepository subject = new HttpsDataSourceRepository();

  @Test
  void testDescription() {
    assertEquals("HTTPS", subject.description());
  }

  @Test
  void testFindSource() throws Exception {
    assertNull(
      subject.findSource(new URI("file:/a.txt"), FileType.UNKNOWN),
      "Expect to return null for unknown URI without connection to store"
    );
  }
}
