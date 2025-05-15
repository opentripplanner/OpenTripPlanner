package org.opentripplanner.ext.datastore.gs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.api.GsParameters;

public class GsDataSourceRepositoryTest {

  private final GsDataSourceRepository subject = new GsDataSourceRepository(
    GsParameters.defaultValues()
  );

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
