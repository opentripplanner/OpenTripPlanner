package org.opentripplanner.smoketest;

import static org.opentripplanner.client.model.RequestMode.SCOOTER_RENT;
import static org.opentripplanner.client.model.RequestMode.TRAM;
import static org.opentripplanner.client.model.RequestMode.WALK;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.smoketest.util.SmokeTestRequest;

@Tag("smoke-test")
@Tag("portland")
public class PortlandSmokeTest {

  Coordinate cennentenial = new Coordinate(45.504602, -122.4968719);
  Coordinate buckman = new Coordinate(45.51720, -122.652289867);
  Coordinate hazelwood = new Coordinate(45.52463, -122.5583);
  Coordinate piedmont = new Coordinate(45.5746, -122.6697);
  Coordinate mountTaborPark = new Coordinate(45.511399, -122.594203);

  @Test
  public void railTrip() {
    // this used to be across the city by since the train is interrupted in April '23 this is a
    // much shorter trip
    var plan = SmokeTest.basicRouteTest(
      new SmokeTestRequest(cennentenial, hazelwood, Set.of(TRAM, WALK)),
      List.of("WALK", "TRAM", "WALK")
    );

    SmokeTest.assertThatAllTransitLegsHaveFareProducts(plan);
  }

  @Nested
  @Disabled
  class GeofencingZones {

    /**
     * Checks that a scooter rental finishes at the edge of the park area and is continued on
     * foot rather than scootering all the way to the destination.
     */
    @ParameterizedTest(name = "scooter rental in a geofencing zone with arriveBy={0}")
    @ValueSource(booleans = { true, false })
    public void geofencingZone(boolean arriveBy) {
      SmokeTest.basicRouteTest(
        new SmokeTestRequest(buckman, mountTaborPark, Set.of(SCOOTER_RENT, WALK), arriveBy),
        List.of("WALK", "SCOOTER", "WALK")
      );
    }

    @ParameterizedTest(name = "scooter rental with arriveBy={0}")
    @ValueSource(booleans = { true, false })
    void scooterRent(boolean arriveBy) {
      SmokeTest.basicRouteTest(
        new SmokeTestRequest(cennentenial, piedmont, Set.of(SCOOTER_RENT, WALK), arriveBy),
        List.of("WALK", "SCOOTER")
      );
    }
  }
}
