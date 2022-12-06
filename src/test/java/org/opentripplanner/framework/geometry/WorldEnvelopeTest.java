package org.opentripplanner.framework.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class WorldEnvelopeTest {

  private static final int S10 = -10;
  private static final int S20 = -20;
  private static final int N30 = 30;
  private static final int N40 = 40;
  private static final int E50 = 50;
  private static final int E160 = 160;
  private static final int W60 = -60;
  private static final int W170 = -170;

  private static final Coordinate S10E50 = new Coordinate(E50, S10);
  private static final Coordinate S20E160 = new Coordinate(E160, S20);
  private static final Coordinate N30W60 = new Coordinate(W60, N30);
  private static final Coordinate N40W170 = new Coordinate(W170, N40);

  private static final WorldEnvelope EAST = WorldEnvelope
    .of()
    .expandToInclude(S10E50)
    .expandToInclude(S20E160)
    .build();
  private static final WorldEnvelope WEST = WorldEnvelope
    .of()
    .expandToInclude(N30W60)
    .expandToInclude(N40W170)
    .build();
  private static final WorldEnvelope GREENWICH = WorldEnvelope
    .of()
    .expandToInclude(N30W60)
    .expandToInclude(S10E50)
    .build();
  private static final WorldEnvelope MERIDIAN_180 = WorldEnvelope
    .of()
    .expandToInclude(N40W170)
    .expandToInclude(S20E160)
    .build();

  @Test
  void testToString() {
    assertEquals(
      "WorldEnvelope{lowerLeft: (-20.0 50.0), upperRight: (-10.0 160.0), center: (-15.0 105.0)}",
      EAST.toString()
    );
    assertEquals(
      "WorldEnvelope{lowerLeft: (30.0 -170.0), upperRight: (40.0 -60.0), center: (35.0 -115.0)}",
      WEST.toString()
    );
    assertEquals(
      "WorldEnvelope{lowerLeft: (-10.0 -60.0), upperRight: (30.0 50.0), center: (10.0 -5.0)}",
      GREENWICH.toString()
    );
    assertEquals(
      "WorldEnvelope{lowerLeft: (-20.0 160.0), upperRight: (40.0 -170.0), center: (10.0 175.0)}",
      MERIDIAN_180.toString()
    );
  }

  @Test
  void testEast() {
    assertEquals(S20, EAST.getLowerLeftLatitude());
    assertEquals(E50, EAST.getLowerLeftLongitude());
    assertEquals(S10, EAST.getUpperRightLatitude());
    assertEquals(E160, EAST.getUpperRightLongitude());
    assertEquals(-15d, EAST.centerLatitude());
    assertEquals(105d, EAST.centerLongitude());
  }
}
