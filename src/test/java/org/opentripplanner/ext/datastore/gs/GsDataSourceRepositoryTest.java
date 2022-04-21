package org.opentripplanner.ext.datastore.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URI;
import org.junit.Test;
import org.opentripplanner.datastore.FileType;

public class GsDataSourceRepositoryTest {

  private final GsDataSourceRepository subject = new GsDataSourceRepository(null);

  @Test
  public void description() {
    assertEquals("Google Cloud Storage", subject.description());
  }

  @Test
  public void findSource() throws Exception {
    assertNull(
      "Expect to return null for unknown URI without connection to store",
      subject.findSource(new URI("file:/a.txt"), FileType.UNKNOWN)
    );
  }
}
