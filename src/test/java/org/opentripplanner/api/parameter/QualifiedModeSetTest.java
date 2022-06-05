package org.opentripplanner.api.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;

import java.util.Set;
import javax.ws.rs.BadRequestException;
import org.junit.Test;
import org.opentripplanner.routing.api.request.RequestModes;

public class QualifiedModeSetTest {

  @Test
  public void emptyModeSet() {
    assertThrows(BadRequestException.class, () -> new QualifiedModeSet(""));
  }

  @Test
  public void singleWalk() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK");
    assertEquals(Set.of(new QualifiedMode("WALK")), modeSet.qModes);
    assertEquals(
      RequestModes
        .of()
        .withAccessMode(WALK)
        .withEgressMode(WALK)
        .withDirectMode(WALK)
        .withTransferMode(WALK)
        .clearTransitMode()
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  public void multipleWalks() {
    QualifiedModeSet modeSet = new QualifiedModeSet(new String[] { "WALK", "WALK", "WALK" });
    assertEquals(Set.of(new QualifiedMode("WALK")), modeSet.qModes);
    assertEquals(
      RequestModes.of().withAllStreetModes(WALK).clearTransitMode().build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  public void singleWalkAndBicycle() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE");
    assertEquals(Set.of(new QualifiedMode("WALK"), new QualifiedMode("BICYCLE")), modeSet.qModes);
    assertEquals(
      RequestModes.of().withAllStreetModes(BIKE).clearTransitMode().build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  public void singleWalkAndBicycleRental() {
    QualifiedModeSet modeSet = new QualifiedModeSet(new String[] { "WALK", "BICYCLE_RENT" });
    assertEquals(
      Set.of(new QualifiedMode("WALK"), new QualifiedMode("BICYCLE_RENT")),
      modeSet.qModes
    );
    assertEquals(
      RequestModes.of().withAllStreetModes(BIKE_RENTAL).clearTransitMode().build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  public void singleWalkAndBicycleToPark() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE_PARK");
    assertEquals(
      Set.of(new QualifiedMode("WALK"), new QualifiedMode("BICYCLE_PARK")),
      modeSet.qModes
    );
    assertEquals(
      RequestModes
        .of()
        .withAccessMode(BIKE_TO_PARK)
        .withEgressMode(WALK)
        .withDirectMode(BIKE_TO_PARK)
        .withTransferMode(WALK)
        .clearTransitMode()
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  public void multipleWalksAndBicycle() {
    QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE,WALK");
    assertEquals(Set.of(new QualifiedMode("WALK"), new QualifiedMode("BICYCLE")), modeSet.qModes);
    assertEquals(
      RequestModes
        .of()
        .withAccessMode(BIKE)
        .withEgressMode(BIKE)
        .withDirectMode(BIKE)
        .withTransferMode(BIKE)
        .clearTransitMode()
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  public void multipleNonWalkModes() {
    assertThrows(
      IllegalStateException.class,
      () -> new QualifiedModeSet("WALK,BICYCLE,CAR").getRequestModes()
    );
  }

  @Test
  public void allFlexible() {
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
      RequestModes
        .of()
        .withAccessMode(FLEXIBLE)
        .withEgressMode(FLEXIBLE)
        .withDirectMode(FLEXIBLE)
        .withTransferMode(WALK)
        .clearTransitMode()
        .build(),
      modeSet.getRequestModes()
    );
  }

  @Test
  public void bicycleToParkWithFlexibleEgress() {
    QualifiedModeSet modeSet = new QualifiedModeSet("BICYCLE_PARK,FLEX_EGRESS");
    assertEquals(
      Set.of(new QualifiedMode("FLEX_EGRESS"), new QualifiedMode("BICYCLE_PARK")),
      modeSet.qModes
    );
    assertEquals(
      RequestModes
        .of()
        .withAccessMode(BIKE_TO_PARK)
        .withEgressMode(FLEXIBLE)
        .withDirectMode(BIKE_TO_PARK)
        .withTransferMode(WALK)
        .clearTransitMode()
        .build(),
      modeSet.getRequestModes()
    );
  }
}
