package org.opentripplanner.ext.datastore.gs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;

public class GsDataSourceRepositoryTest {

  private final GsDataSourceRepository subject = new GsDataSourceRepository(null);

  @Test
  public void description() {
    assertEquals("Google Cloud Storage", subject.description());
  }

  @Test
  public void findSource() throws Exception {
    assertNull(
      subject.findSource(new URI("file:/a.txt"), FileType.UNKNOWN),
      "Expect to return null for unknown URI without connection to store"
    );
  }
}
