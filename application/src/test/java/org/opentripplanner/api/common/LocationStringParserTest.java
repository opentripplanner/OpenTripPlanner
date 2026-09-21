package org.opentripplanner.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;

public class LocationStringParserTest {

  @Test
  public void testFromOldStyleString() {
    var loc = LocationStringParser.fromOldStyleString("name::12345");
    assertEquals(Optional.empty(), loc);
  }

  @Test
  public void testWithLabelAndCoord() {
    GenericLocation loc = LocationStringParser.fromOldStyleString("name::1.0,2.5").get();
    assertEquals("name", loc.label());
    assertNull(loc.stopId());
    assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());

    loc = LocationStringParser.fromOldStyleString("Label Label::-15.0,  170").get();
    assertEquals("Label Label", loc.label());
    assertNull(loc.stopId());
    assertEquals(new Coordinate(170, -15), loc.getCoordinate());

    loc = LocationStringParser.fromOldStyleString("A Label::89,-22.3").get();
    assertEquals("A Label", loc.label());
    assertNull(loc.stopId());
    assertEquals(new Coordinate(-22.3, 89), loc.getCoordinate());
  }

  @Test
  public void testWithId() {
    GenericLocation loc = LocationStringParser.fromOldStyleString("name::aFeed:A1B2C3").get();
    assertEquals("name", loc.label());
    assertEquals(loc.stopId(), new FeedScopedId("aFeed", "A1B2C3"));
    assertNull(loc.getCoordinate());

    loc = LocationStringParser.fromOldStyleString("feed:4321").get();
    assertNull(loc.label());
    assertEquals(loc.stopId(), new FeedScopedId("feed", "4321"));
    assertNull(loc.getCoordinate());
  }

  @Test
  public void testWithCoordOnly() {
    GenericLocation loc = LocationStringParser.fromOldStyleString("1.0,2.5").get();
    assertNull(loc.label());
    assertNull(loc.stopId());
    assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());

    loc = LocationStringParser.fromOldStyleString("    -15.0,  170").get();
    assertNull(loc.label());
    assertNull(loc.stopId());
    assertEquals(new Coordinate(170, -15), loc.getCoordinate());

    loc = LocationStringParser.fromOldStyleString("89,-22.3   ").get();
    assertNull(loc.label());
    assertNull(loc.stopId());
    assertEquals(new Coordinate(-22.3, 89), loc.getCoordinate());
  }

  @Test
  public void testFromOldStyleStringIncomplete() {
    assertEquals(Optional.empty(), LocationStringParser.fromOldStyleString("0::"));
    assertEquals(Optional.empty(), LocationStringParser.fromOldStyleString("::1"));
    assertEquals(Optional.empty(), LocationStringParser.fromOldStyleString("::"));
  }
}
