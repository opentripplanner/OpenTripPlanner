package org.opentripplanner.service.worldenvelope.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class WorldEnvelopeTest {

  private static final int S10 = -10;
  private static final int S20 = -20;
  private static final int N30 = 30;
  private static final int N40 = 40;
  private static final int E50 = 50;
  private static final int E160 = 160;
  private static final int W60 = -60;
  private static final int W170 = -170;

  private static final WorldEnvelope EAST = WorldEnvelope
    .of()
    .expandToIncludeStreetEntities(S10, E50)
    .expandToIncludeStreetEntities(S20, E160)
    .build();
  private static final WorldEnvelope WEST = WorldEnvelope
    .of()
    .expandToIncludeStreetEntities(N30, W60)
    .expandToIncludeStreetEntities(N40, W170)
    .build();
  private static final WorldEnvelope GREENWICH = WorldEnvelope
    .of()
    .expandToIncludeStreetEntities(N30, W60)
    .expandToIncludeStreetEntities(S10, E50)
    .build();
  private static final WorldEnvelope MERIDIAN_180 = WorldEnvelope
    .of()
    .expandToIncludeStreetEntities(N40, W170)
    .expandToIncludeStreetEntities(S20, E160)
    .build();

  @Test
  void testEast() {
    var expectedCenter = new WgsCoordinate(-15d, 105d);

    assertEquals(S20, EAST.lowerLeft().latitude());
    assertEquals(E50, EAST.lowerLeft().longitude());
    assertEquals(S10, EAST.upperRight().latitude());
    assertEquals(E160, EAST.upperRight().longitude());
    assertEquals(expectedCenter, EAST.meanCenter());
    assertEquals(expectedCenter, EAST.center());
    assertTrue(EAST.transitMedianCenter().isEmpty());
  }

  @Test
  void transitMedianCenter() {
    var expectedCenter = new WgsCoordinate(S10, E50);

    var subject = WorldEnvelope
      .of()
      .expandToIncludeTransitEntities(
        List.of(
          new WgsCoordinate(S10, E50),
          new WgsCoordinate(S20, E160),
          new WgsCoordinate(N40, W60)
        ),
        WgsCoordinate::latitude,
        WgsCoordinate::longitude
      )
      .build();

    assertTrue(subject.transitMedianCenter().isPresent(), subject.transitMedianCenter().toString());
    assertEquals(expectedCenter, subject.transitMedianCenter().get());
    assertEquals(expectedCenter, subject.center());
    assertEquals(
      "WorldEnvelope{lowerLeft: (-20.0, -60.0), upperRight: (40.0, 160.0), meanCenter: (10.0, 50.0), transitMedianCenter: (-10.0, 50.0)}",
      subject.toString()
    );
  }

  @Test
  void testToString() {
    assertEquals(
      "WorldEnvelope{lowerLeft: (-20.0, 50.0), upperRight: (-10.0, 160.0), meanCenter: (-15.0, 105.0)}",
      EAST.toString()
    );
    assertEquals(
      "WorldEnvelope{lowerLeft: (30.0, -170.0), upperRight: (40.0, -60.0), meanCenter: (35.0, -115.0)}",
      WEST.toString()
    );
    assertEquals(
      "WorldEnvelope{lowerLeft: (-10.0, -60.0), upperRight: (30.0, 50.0), meanCenter: (10.0, -5.0)}",
      GREENWICH.toString()
    );
    assertEquals(
      "WorldEnvelope{lowerLeft: (-20.0, 160.0), upperRight: (40.0, -170.0), meanCenter: (10.0, 175.0)}",
      MERIDIAN_180.toString()
    );
  }
}
