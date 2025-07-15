package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;

public class RoutingPreferencesTest {

  private static final Locale LOCALE = Locale.FRENCH;

  @Test
  public void copyOfShouldReturnTheSameInstanceWhenBuild() {
    var pref = RoutingPreferences.DEFAULT;
    var same = pref.copyOf().build();
    assertSame(pref, same);
    // Change one thing to force making a copy
    var copy = pref.copyOf().withCar(c -> c.withReluctance(3.5)).build();
    assertNotSame(pref.car(), copy.car());

    // Immutable classes should not change
    assertSame(pref.bike(), copy.bike());
    assertSame(pref.walk(), copy.walk());
    assertSame(pref.transfer(), copy.transfer());
    assertSame(pref.wheelchair(), copy.wheelchair());
    assertSame(pref.transit(), copy.transit());
    assertSame(pref.street(), copy.street());
    assertSame(pref.itineraryFilter(), copy.itineraryFilter());
    assertSame(pref.system(), copy.system());
  }

  @Test
  public void copyOfWithCarChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withCar(c -> c.withReluctance(3.5)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.car(), copy.car());
    assertSame(pref.bike(), copy.bike());
  }

  @Test
  public void copyOfWithBikeChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.of().withBike(b -> b.withReluctance(2.5)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.bike(), copy.bike());
    assertSame(pref.walk(), copy.walk());
  }

  @Test
  public void copyOfWithScooterChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withScooter(b -> b.withReluctance(2.5)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.scooter(), copy.scooter());
    assertSame(pref.walk(), copy.walk());
  }

  @Test
  public void copyOfWithWalkChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withWalk(w -> w.withReluctance(2.5)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.walk(), copy.walk());
    assertSame(pref.transfer(), copy.transfer());
  }

  @Test
  public void copyOfWithTransferChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withTransfer(t -> t.withSlack(Duration.ofSeconds(2))).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.transfer(), copy.transfer());
    assertSame(pref.wheelchair(), copy.wheelchair());
  }

  @Test
  public void copyOfWithWheelchairChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref
      .copyOf()
      .withWheelchair(it ->
        it
          .withStopOnlyAccessible()
          .withTripOnlyAccessible()
          .withElevatorOnlyAccessible()
          .withInaccessibleStreetReluctance(5)
          .withMaxSlope(0.01)
          .withSlopeExceededReluctance(12)
          .withStairsReluctance(3)
      )
      .build();

    assertNotSame(pref, copy);
    assertNotSame(pref.wheelchair(), copy.wheelchair());
    assertSame(pref.transit(), copy.transit());
  }

  @Test
  public void copyOfWithTransitChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withTransit(t -> t.withDefaultBoardSlackSec(2)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.transit(), copy.transit());
    assertSame(pref.street(), copy.street());
  }

  @Test
  public void copyOfWithStreetChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withStreet(s -> s.withTurnReluctance(2)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.street(), copy.street());
  }

  @Test
  public void copyOfWithItineraryFilterChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withItineraryFilter(i -> i.withGroupSimilarityKeepOne(2)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.itineraryFilter(), copy.itineraryFilter());
    assertSame(pref.system(), copy.system());
  }

  @Test
  public void copyOfWithSystemChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withSystem(s -> s.withGeoidElevation(true)).build();

    assertNotSame(pref, copy);
    assertNotSame(pref.system(), copy.system());
    assertSame(pref.car(), copy.car());
  }

  @Test
  public void copyOfWithLocaleChanges() {
    var pref = RoutingPreferences.DEFAULT;
    var copy = pref.copyOf().withLocale(LOCALE).build();

    assertEquals(LOCALE, copy.locale());
    assertNotSame(pref, copy);
  }
}
