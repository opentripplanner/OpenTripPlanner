package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.entur.netex2.model.GroupOfLines;
import org.entur.netex2.model.MultilingualString;
import org.entur.netex2.model.PrivateCodeStructure;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.GroupOfRoutes;

public class GroupOfRoutesMapperTest {

  private final String ID = "RUT:GroupOfLines:1";
  private final String PRIVATE_CODE = "test_private_code";
  private final String NAME = "test_name";
  private final String SHORT_NAME = "test_short_name";
  private final String DESCRIPTION = "description";

  @Test
  public void mapGroupOfRoutes() {
    GroupOfRoutesMapper mapper = new GroupOfRoutesMapper(MappingSupport.ID_FACTORY);

    GroupOfRoutes groupOfRoutes = mapper.mapGroupOfRoutes(createGroupOfLines());

    assertNotNull(groupOfRoutes);
    assertEquals(ID, groupOfRoutes.getId().getId());
    assertEquals(NAME, groupOfRoutes.getName());
    assertEquals(SHORT_NAME, groupOfRoutes.getShortName());
    assertEquals(DESCRIPTION, groupOfRoutes.getDescription());
  }

  private GroupOfLines createGroupOfLines() {
    return new GroupOfLines()
      .withId(ID)
      .withPrivateCode(new PrivateCodeStructure().withValue(PRIVATE_CODE))
      .withName(new MultilingualString().withContent(NAME))
      .withShortName(new MultilingualString().withContent(SHORT_NAME))
      .withDescription(new MultilingualString().withContent(DESCRIPTION));
  }
}
