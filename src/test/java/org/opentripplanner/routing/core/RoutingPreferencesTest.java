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
    assertNotSame(pref.rental(), clone.rental());

    // Immutable classes should not change
    assertSame(pref.car(), clone.car());
    assertSame(pref.bike(), clone.bike());
    assertSame(pref.walk(), clone.walk());
    assertSame(pref.transfer(), clone.transfer());
    assertSame(pref.wheelchair(), clone.wheelchair());
    assertSame(pref.transit(), clone.transit());
    assertSame(pref.parking(), clone.parking());
    assertSame(pref.street(), clone.street());
    assertSame(pref.itineraryFilter(), clone.itineraryFilter());
    assertSame(pref.system(), clone.system());
  }
}
