package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;

public class RoutingPreferencesTest {

  @Test
  public void shouldCloneObjectFields() {
    // TODO VIA (Thomas): There are more objects that are cloned - check freezing
    var pref = new RoutingPreferences();

    var clone = pref.clone();

    assertNotSame(pref, clone);
    assertNotSame(pref.transit(), clone.transit());
    assertNotSame(pref.transfer(), clone.transfer());
    assertNotSame(pref.street(), clone.street());
    assertNotSame(pref.car(), clone.car());
    assertNotSame(pref.rental(), clone.rental());
    assertNotSame(pref.parking(), clone.parking());
    assertNotSame(pref.system(), clone.system());

    assertNotSame(pref.system().itineraryFilters(), clone.system().itineraryFilters());
    assertNotSame(pref.transit().raptorOptions(), clone.transit().raptorOptions());

    // Immutable classes should not change
    assertSame(pref.walk(), clone.walk());
    assertSame(pref.bike(), clone.bike());
    assertSame(pref.wheelchair(), clone.wheelchair());
  }
}
