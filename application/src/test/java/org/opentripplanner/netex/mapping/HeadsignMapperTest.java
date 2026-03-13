package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.MultilingualString;

class HeadsignMapperTest {

  private static final MultilingualString AAA = new MultilingualString().withValue("AAA");
  private static final MultilingualString BBB = new MultilingualString().withValue("BBB");

  @Test
  void onlyFrontText() {
    var dd = new DestinationDisplay().withFrontText(AAA);
    var result = HeadsignMapper.mapHeadsign(dd);
    assertEquals("AAA", result.toString());
  }

  @Test
  void onlyName() {
    var dd = new DestinationDisplay().withName(AAA);
    var result = HeadsignMapper.mapHeadsign(dd);
    assertEquals("AAA", result.toString());
  }

  @Test
  void frontTextPreferred() {
    var dd = new DestinationDisplay().withName(AAA).withFrontText(BBB);
    var result = HeadsignMapper.mapHeadsign(dd);
    assertEquals("BBB", result.toString());
  }

  @Test
  void nullable() {
    var dd = new DestinationDisplay();
    assertNull(HeadsignMapper.mapHeadsign(dd));
  }
}
