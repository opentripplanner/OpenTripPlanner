package org.opentripplanner.routing.impl;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;

/**
 * Tests that the delegating service merges the alerts of all registered delegates.
 */
class DelegatingTransitAlertServiceImplTest {

  private static final String FEED_ID = "GB";
  private static final String STATION_ID = "910GSTPX";
  private static final String STOP_ID = "9100STPX1";

  private static final TransitAlert STATION_ALERT = TransitAlert.of(id("station_alert"))
    .addEntity(new EntitySelector.Stop(id(STATION_ID)))
    .build();
  private static final TransitAlert STOP_ALERT = TransitAlert.of(id("stop_alert"))
    .addEntity(new EntitySelector.Stop(id(STOP_ID)))
    .build();

  @Test
  void getStopLocationsAlertsCombinesAlertsOfAllDelegates() {
    // each updater keeps its own service, here one holds the stop alert, the other the station one
    var iut = delegatingService(List.of(STOP_ALERT), List.of(STATION_ALERT));

    assertThat(iut.getStopLocationsAlerts(List.of(id(STOP_ID), id(STATION_ID)))).containsExactly(
      STOP_ALERT,
      STATION_ALERT
    );
  }

  @Test
  void getStopLocationsAlertsDeduplicatesAlertsFromMultipleDelegates() {
    // the same alert is present in two delegates, it must only be returned once
    var iut = delegatingService(List.of(STOP_ALERT), List.of(STOP_ALERT));

    assertThat(iut.getStopLocationsAlerts(List.of(id(STOP_ID)))).containsExactly(STOP_ALERT);
  }

  @Test
  void getStopLocationsAlertsWithoutDelegates() {
    assertThat(
      new DelegatingTransitAlertServiceImpl().getStopLocationsAlerts(List.of(id(STOP_ID)))
    ).isEmpty();
  }

  @SafeVarargs
  private static DelegatingTransitAlertServiceImpl delegatingService(
    List<TransitAlert>... alertsPerDelegate
  ) {
    var service = new DelegatingTransitAlertServiceImpl();
    for (var alerts : alertsPerDelegate) {
      var delegate = new TransitAlertServiceImpl();
      delegate.setAlerts(alerts);
      service.addDelegate(delegate);
    }
    return service;
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }
}
