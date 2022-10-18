package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;

public class RoutingPreferencesTest {

  @Test
  public void copyOfShouldReturnTheSameInstanceWhenBuild() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().build();
    assertNotSame(pref, copy);

    // Immutable classes should not change
    assertSame(pref.car(), copy.car());
    assertSame(pref.bike(), copy.bike());
    assertSame(pref.walk(), copy.walk());
    assertSame(pref.transfer(), copy.transfer());
    assertSame(pref.wheelchair(), copy.wheelchair());
    assertSame(pref.transit(), copy.transit());
    assertSame(pref.street(), copy.street());
    assertSame(pref.parking(), copy.parking());
    assertSame(pref.rental(), copy.rental());
    assertSame(pref.itineraryFilter(), copy.itineraryFilter());
    assertSame(pref.system(), copy.system());
  }

  @Test
  public void copyOfWithCarChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withCar(c -> c.withReluctance(3.5)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.car(), copy.car());
    assertSame(pref.bike(), copy.bike());
  }

  @Test
  public void copyOfWithBikeChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withBike(b -> b.withReluctance(2.5)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.bike(), copy.bike());
    assertSame(pref.walk(), copy.walk());
  }

  @Test
  public void copyOfWithWalkChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withWalk(w -> w.withReluctance(2.5)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.walk(), copy.walk());
    assertSame(pref.transfer(), copy.transfer());
  }

  @Test
  public void copyOfWithTransferChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withTransfer(t -> t.withSlack(2)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.transfer(), copy.transfer());
    assertSame(pref.wheelchair(), copy.wheelchair());
  }

  @Test
  public void copyOfWithWheelchairChanges() {
    var pref = new RoutingPreferences();
    var copy = pref
      .copyOf()
      .withWheelchair(
        new WheelchairPreferences(
          AccessibilityPreferences.ofOnlyAccessible(),
          AccessibilityPreferences.ofOnlyAccessible(),
          AccessibilityPreferences.ofOnlyAccessible(),
          5,
          0.01,
          12,
          3
        )
      )
      .build();

    assertNotSame(pref, copy);
    assertNotSame(pref.wheelchair(), copy.wheelchair());
    assertSame(pref.transit(), copy.transit());
  }

  @Test
  public void copyOfWithTransitChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withTransit(t -> t.withDefaultBoardSlackSec(2)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.transit(), copy.transit());
    assertSame(pref.street(), copy.street());
  }

  @Test
  public void copyOfWithStreetChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withStreet(s -> s.withTurnReluctance(2)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.street(), copy.street());
    assertSame(pref.parking(), copy.parking());
  }

  @Test
  public void copyOfWithParkingChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withParking(VehicleParkingPreferences.of(true)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.parking(), copy.parking());
    assertSame(pref.rental(), copy.rental());
  }

  @Test
  public void copyOfWithRentalChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withRental(r -> r.withDropoffCost(2)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.rental(), copy.rental());
    assertSame(pref.itineraryFilter(), copy.itineraryFilter());
  }

  @Test
  public void copyOfWithItineraryFilterChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withItineraryFilter(i -> i.withGroupSimilarityKeepOne(2)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.itineraryFilter(), copy.itineraryFilter());
    assertSame(pref.system(), copy.system());
  }

  @Test
  public void copyOfWithSystemChanges() {
    var pref = new RoutingPreferences();
    var copy = pref.copyOf().withSystem(s -> s.withGeoidElevation(true)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.system(), copy.system());
    assertSame(pref.car(), copy.car());
  }
}
