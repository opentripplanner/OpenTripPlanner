package org.opentripplanner.api.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.CAR_HAILING;
import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;
import static org.opentripplanner.routing.api.request.StreetMode.SCOOTER_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;
import static org.opentripplanner.transit.model.basic.TransitMode.AIRPLANE;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.CABLE_CAR;
import static org.opentripplanner.transit.model.basic.TransitMode.CARPOOL;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;
import static org.opentripplanner.transit.model.basic.TransitMode.FUNICULAR;
import static org.opentripplanner.transit.model.basic.TransitMode.GONDOLA;
import static org.opentripplanner.transit.model.basic.TransitMode.MONORAIL;
import static org.opentripplanner.transit.model.basic.TransitMode.RAIL;
import static org.opentripplanner.transit.model.basic.TransitMode.SUBWAY;
import static org.opentripplanner.transit.model.basic.TransitMode.TRAM;
import static org.opentripplanner.transit.model.basic.TransitMode.TROLLEYBUS;

import jakarta.ws.rs.BadRequestException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.transit.model.basic.TransitMode;

class QualifiedModeSetTest {

  @Test
  void emptyModeSet() {
    assertThrows(BadRequestException.class, () -> new QualifiedModeSet(""));
  }

  @Test
  void singleWalk() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK");
    assertEquals(Set.of(new QualifiedMode("WALK")), modeSet.qModes);
    assertEquals(
      RequestModes.of()
        .withAccessMode(WALK)
        .withEgressMode(WALK)
        .withDirectMode(WALK)
        .withTransferMode(WALK)
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  void multipleWalks() {
    QualifiedModeSet modeSet = new QualifiedModeSet(new String[] { "WALK", "WALK", "WALK" });
    assertEquals(Set.of(new QualifiedMode("WALK")), modeSet.qModes);
    assertEquals(RequestModes.of().withAllStreetModes(WALK).build(), modeSet.getRequestModes());
  }

  @Test
  void singleWalkAndBicycle() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE");
    assertEquals(Set.of(new QualifiedMode("WALK"), new QualifiedMode("BICYCLE")), modeSet.qModes);
    assertEquals(RequestModes.of().withAllStreetModes(BIKE).build(), modeSet.getRequestModes());
  }

  @Test
  void singleWalkAndBicycleRental() {
    QualifiedModeSet modeSet = new QualifiedModeSet(new String[] { "WALK", "BICYCLE_RENT" });
    assertEquals(
      Set.of(new QualifiedMode("WALK"), new QualifiedMode("BICYCLE_RENT")),
      modeSet.qModes
    );
    assertEquals(
      RequestModes.of().withAllStreetModes(BIKE_RENTAL).build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  void singleWalkAndBicycleToPark() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE_PARK");
    assertEquals(
      Set.of(new QualifiedMode("WALK"), new QualifiedMode("BICYCLE_PARK")),
      modeSet.qModes
    );
    assertEquals(
      RequestModes.of()
        .withAccessMode(BIKE_TO_PARK)
        .withEgressMode(WALK)
        .withDirectMode(BIKE_TO_PARK)
        .withTransferMode(WALK)
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  void multipleWalksAndBicycle() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE,WALK");
    assertEquals(Set.of(new QualifiedMode("WALK"), new QualifiedMode("BICYCLE")), modeSet.qModes);
    assertEquals(
      RequestModes.of()
        .withAccessMode(BIKE)
        .withEgressMode(BIKE)
        .withDirectMode(BIKE)
        .withTransferMode(BIKE)
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  void multipleNonWalkModes() {
    assertThrows(IllegalStateException.class, () ->
      new QualifiedModeSet("WALK,BICYCLE,CAR").getRequestModes()
    );
  }

  @Test
  void allFlexible() {
    QualifiedModeSet modeSet = new QualifiedModeSet("FLEX_ACCESS,FLEX_EGRESS,FLEX_DIRECT");
    assertEquals(
      Set.of(
        new QualifiedMode("FLEX_DIRECT"),
        new QualifiedMode("FLEX_EGRESS"),
        new QualifiedMode("FLEX_ACCESS")
      ),
      modeSet.qModes
    );
    assertEquals(
      RequestModes.of()
        .withAccessMode(FLEXIBLE)
        .withEgressMode(FLEXIBLE)
        .withDirectMode(FLEXIBLE)
        .withTransferMode(WALK)
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  void bicycleToParkWithFlexibleEgress() {
    QualifiedModeSet modeSet = new QualifiedModeSet("BICYCLE_PARK,FLEX_EGRESS");
    assertEquals(
      Set.of(new QualifiedMode("FLEX_EGRESS"), new QualifiedMode("BICYCLE_PARK")),
      modeSet.qModes
    );
    assertEquals(
      RequestModes.of()
        .withAccessMode(BIKE_TO_PARK)
        .withEgressMode(FLEXIBLE)
        .withDirectMode(BIKE_TO_PARK)
        .withTransferMode(WALK)
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  void walkTransitExcludesCarpool() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK,TRANSIT");
    assertEquals(Set.of(new QualifiedMode("WALK"), new QualifiedMode("TRANSIT")), modeSet.qModes);

    Set<TransitMode> expected = Set.of(
      TransitMode.RAIL,
      TransitMode.COACH,
      SUBWAY,
      BUS,
      TRAM,
      FERRY,
      AIRPLANE,
      CABLE_CAR,
      GONDOLA,
      FUNICULAR,
      TROLLEYBUS,
      MONORAIL,
      TransitMode.TAXI
    );

    var mainModes = Set.copyOf(modeSet.getTransitModes());

    assertEquals(mainModes, expected);
  }

  @Test
  void specificallyRequestCarpool() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK,TRANSIT,CARPOOL");

    Set<TransitMode> expected = Set.of(
      TransitMode.RAIL,
      TransitMode.COACH,
      SUBWAY,
      BUS,
      TRAM,
      FERRY,
      AIRPLANE,
      CABLE_CAR,
      GONDOLA,
      FUNICULAR,
      TROLLEYBUS,
      CARPOOL,
      MONORAIL,
      TransitMode.TAXI
    );

    var mainModes = Set.copyOf(modeSet.getTransitModes());

    assertEquals(mainModes, expected);
  }

  @Test
  void carHail() {
    var modeSet = new QualifiedModeSet("CAR_HAIL");
    assertTrue(modeSet.getTransitModes().isEmpty());

    assertEquals(WALK, modeSet.getRequestModes().directMode);
    assertEquals(CAR_HAILING, modeSet.getRequestModes().accessMode);
    assertEquals(CAR_HAILING, modeSet.getRequestModes().egressMode);
  }

  @Test
  void carHailWithTransit() {
    var modeSet = new QualifiedModeSet("CAR_HAIL,BUS,RAIL");
    assertEquals(Set.of(BUS, RAIL), Set.copyOf(modeSet.getTransitModes()));

    assertEquals(WALK, modeSet.getRequestModes().directMode);
    assertEquals(CAR_HAILING, modeSet.getRequestModes().accessMode);
    assertEquals(CAR_HAILING, modeSet.getRequestModes().egressMode);
  }

  @Test
  void scooterTransfer() {
    var modeSet = new QualifiedModeSet("SCOOTER_RENT,BUS,RAIL,SUBWAY");
    assertEquals(Set.of(BUS, RAIL, SUBWAY), Set.copyOf(modeSet.getTransitModes()));

    var requestModes = modeSet.getRequestModes();
    assertEquals(SCOOTER_RENTAL, requestModes.directMode);
    assertEquals(WALK, requestModes.transferMode);
    assertEquals(SCOOTER_RENTAL, requestModes.egressMode);
  }
}
