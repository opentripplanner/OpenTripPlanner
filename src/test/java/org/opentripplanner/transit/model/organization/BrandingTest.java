package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;

public class BrandingTest {

  private static final String ID = "Branding:1";
  private static final String SHORT_NAME = "test_short_name";
  private static final String NAME = "test_name";
  private static final String URL = "test_url";
  private static final String DESCRIPTION = "test_description";
  private static final String IMAGE = "test_image";

  Branding subject = Branding
    .of(TransitModelForTest.id(ID))
    .setShortName(SHORT_NAME)
    .setName(NAME)
    .setUrl(URL)
    .setDescription(DESCRIPTION)
    .setImage(IMAGE)
    .build();

  @Test
  void copy() {
    var copy = subject.copy().setId(TransitModelForTest.id("v2")).build();

    assertEquals("v2", copy.getId().getId());
    assertEquals(SHORT_NAME, copy.getShortName());
    assertEquals(NAME, copy.getName());
    assertEquals(URL, copy.getUrl());
    assertEquals(DESCRIPTION, copy.getDescription());
    assertEquals(IMAGE, copy.getImage());
  }

  @Test
  public void testToString() {
    assertEquals(
      "Branding{" +
      "id: F:Branding:1, " +
      "shortName: 'test_short_name', " +
      "name: 'test_name', " +
      "url: 'test_url', " +
      "description: 'test_description', " +
      "image: 'test_image'" +
      "}",
      subject.toString()
    );
  }
}
