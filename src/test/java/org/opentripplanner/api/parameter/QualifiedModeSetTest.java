package org.opentripplanner.api.parameter;

import org.junit.Test;
import org.opentripplanner.routing.api.request.RequestModes;

import javax.ws.rs.BadRequestException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_RENTAL;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE_TO_PARK;
import static org.opentripplanner.routing.api.request.StreetMode.FLEXIBLE;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;

public class QualifiedModeSetTest {
    @Test
    public void emptyModeSet() {
        assertThrows(BadRequestException.class, () -> new QualifiedModeSet(""));
    }

    @Test
    public void singleWalk() {
        QualifiedModeSet modeSet = new QualifiedModeSet("WALK");
        assertEquals(Set.of(new QualifiedMode("WALK")), modeSet.qModes);
        assertEquals(new RequestModes(WALK, WALK, WALK, Set.of()), modeSet.getRequestModes());
    }

    @Test
    public void multipleWalks() {
        QualifiedModeSet modeSet = new QualifiedModeSet("WALK,WALK,WALK");
        assertEquals(Set.of(new QualifiedMode("WALK")), modeSet.qModes);
        assertEquals(new RequestModes(WALK, WALK, WALK, Set.of()), modeSet.getRequestModes());
    }

    @Test
    public void singleWalkAndBicycle() {
        QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE");
        assertEquals(Set.of(
                new QualifiedMode("WALK"),
                new QualifiedMode("BICYCLE")
        ), modeSet.qModes);
        assertEquals(new RequestModes(BIKE, BIKE, BIKE, Set.of()), modeSet.getRequestModes());
    }

    @Test
    public void singleWalkAndBicycleRental() {
        QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE_RENT");
        assertEquals(Set.of(
                new QualifiedMode("WALK"),
                new QualifiedMode("BICYCLE_RENT")
        ), modeSet.qModes);
        assertEquals(new RequestModes(BIKE_RENTAL, BIKE_RENTAL, BIKE_RENTAL, Set.of()), modeSet.getRequestModes());
    }

    @Test
    public void singleWalkAndBicycleToPark() {
        QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE_PARK");
        assertEquals(Set.of(
                new QualifiedMode("WALK"),
                new QualifiedMode("BICYCLE_PARK")
        ), modeSet.qModes);
        assertEquals(new RequestModes(BIKE_TO_PARK, WALK, BIKE_TO_PARK, Set.of()), modeSet.getRequestModes());
    }

    @Test
    public void multipleWalksAndBicycle() {
        QualifiedModeSet modeSet = new QualifiedModeSet("WALK,BICYCLE,WALK");
        assertEquals(Set.of(
                new QualifiedMode("WALK"),
                new QualifiedMode("BICYCLE")
        ), modeSet.qModes);
        assertEquals(new RequestModes(BIKE, BIKE, BIKE, Set.of()), modeSet.getRequestModes());
    }

    @Test
    public void multipleNonWalkModes() {
        assertThrows(IllegalStateException.class, () -> new QualifiedModeSet("WALK,BICYCLE,CAR").getRequestModes());
    }

    @Test
    public void allFlexible() {
        QualifiedModeSet modeSet = new QualifiedModeSet("FLEX_ACCESS,FLEX_EGRESS,FLEX_DIRECT");
        assertEquals(Set.of(
                new QualifiedMode("FLEX_DIRECT"),
                new QualifiedMode("FLEX_EGRESS"),
                new QualifiedMode("FLEX_ACCESS")
        ), modeSet.qModes);
        assertEquals(new RequestModes(FLEXIBLE, FLEXIBLE, FLEXIBLE, Set.of()), modeSet.getRequestModes());
    }

    @Test
    public void bicycleToParkWithFlexibleEgress() {
        QualifiedModeSet modeSet = new QualifiedModeSet("BICYCLE_PARK,FLEX_EGRESS");
        assertEquals(Set.of(
                new QualifiedMode("FLEX_EGRESS"),
                new QualifiedMode("BICYCLE_PARK")
        ), modeSet.qModes);
        assertEquals(new RequestModes(BIKE_TO_PARK, FLEXIBLE, BIKE_TO_PARK, Set.of()), modeSet.getRequestModes());
    }
}
