package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.Branding;
import org.rutebanken.netex.model.MultilingualString;

public class BrandingMapperTest {

  private final String ID = "RUT:Branding:1";
  private final String NAME = "test_name";
  private final String SHORT_NAME = "test_short_name";
  private final String DESCRIPTION = "test_description";
  private final String URL = "test_url";
  private final String IMAGE = "test_image";

  @Test
  public void mapBranding() {
    BrandingMapper brandingMapper = new BrandingMapper(MappingSupport.ID_FACTORY);

    org.opentripplanner.transit.model.organization.Branding branding = brandingMapper.mapBranding(
      createBranding()
    );

    assertEquals(ID, branding.getId().getId());
    assertEquals(NAME, branding.getName());
    assertEquals(SHORT_NAME, branding.getShortName());
    assertEquals(DESCRIPTION, branding.getDescription());
    assertEquals(URL, branding.getUrl());
    assertEquals(IMAGE, branding.getImage());
  }

  public Branding createBranding() {
    return new Branding()
      .withId(ID)
      .withName(new MultilingualString().withValue(NAME))
      .withShortName(new MultilingualString().withValue(SHORT_NAME))
      .withDescription(new MultilingualString().withValue(DESCRIPTION))
      .withUrl(URL)
      .withImage(IMAGE);
  }
}
