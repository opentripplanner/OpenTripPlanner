package org.opentripplanner.routing.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;

class TransitAlertServiceImplTest {

  private static final String FEED_ID = "GB";
  private static final String RAIL_STATION_ID = "910GSTPX";
  private static final String RAIL_P1_ID = "9100STPX1";
  private static final String RAIL_PA_ID = "9100STPXBOXA";

  private static final String METRO_STATION_ID = "940GZZLUKSX";

  private static final String METRO_P1_ID = "9400ZZLUKSX1";

  private static final String BUS_STOP_ID = "490001276S";
  private static final Station RAIL_STATION = Station.of(id(RAIL_STATION_ID))
    .withName(I18NString.of("London St Pancras"))
    .withCoordinate(51.532719, -0.126997)
    .build();
  private static final Station METRO_STATION = Station.of(id(METRO_STATION_ID))
    .withName(I18NString.of("King's Cross St. Pancras"))
    .withCoordinate(51.5306090, -0.1239491)
    .build();

  private static final TransitAlert RAIL_STATION_ALERT = TransitAlert.of(id("rail_station_alert"))
    .addEntity(new EntitySelector.Stop(id(RAIL_STATION_ID)))
    .build();
  private static final TransitAlert RAIL_STOP_ALERT = TransitAlert.of(id("rail_stop_alert"))
    .addEntity(new EntitySelector.Stop(id(RAIL_P1_ID)))
    .build();
  private static final TransitAlert BUS_STOP_ALERT = TransitAlert.of(id("bus_stop_alert"))
    .addEntity(new EntitySelector.Stop(id(BUS_STOP_ID)))
    .build();

  private static final TransitRepository TIMETABLE_REPOSITORY = new TransitRepository(
    getSiteRepository()
  );

  private static SiteRepository getSiteRepository() {
    var builder = SiteRepository.of().withStations(List.of(RAIL_STATION, METRO_STATION));
    return builder
      .withRegularStops(
        List.of(
          builder.regularStop(id(RAIL_P1_ID)).withParentStation(RAIL_STATION).build(),
          builder.regularStop(id(RAIL_PA_ID)).withParentStation(RAIL_STATION).build(),
          builder.regularStop(id(METRO_P1_ID)).withParentStation(METRO_STATION).build(),
          builder.regularStop(id(BUS_STOP_ID)).withCoordinate(51.5314719, -0.1272119).build()
        )
      )
      .build();
  }

  @Test
  void getStopAlerts() {
    var iut = serviceWithStopAlerts();

    // getStopAlerts only returns alerts for the exact stop - parent stations are not included.
    assertEquals(Set.of(RAIL_STATION_ALERT), Set.copyOf(iut.getStopAlerts(id(RAIL_STATION_ID))));
    assertEquals(Set.of(RAIL_STOP_ALERT), Set.copyOf(iut.getStopAlerts(id(RAIL_P1_ID))));
    assertEquals(Set.of(), Set.copyOf(iut.getStopAlerts(id(RAIL_PA_ID))));
    assertEquals(Set.of(), Set.copyOf(iut.getStopAlerts(id(METRO_STATION_ID))));
    assertEquals(Set.of(), Set.copyOf(iut.getStopAlerts(id(METRO_P1_ID))));
    assertEquals(Set.of(BUS_STOP_ALERT), Set.copyOf(iut.getStopAlerts(id(BUS_STOP_ID))));
  }

  @Test
  void getStopLocationsAlertsIncludesAlertsForAllIds() {
    var iut = serviceWithStopAlerts();

    // unlike getStopAlerts, both the stop's own alert and the parent station alert are returned
    assertThat(
      iut.getStopLocationsAlerts(List.of(id(RAIL_P1_ID), id(RAIL_STATION_ID)))
    ).containsExactly(RAIL_STOP_ALERT, RAIL_STATION_ALERT);
  }

  @Test
  void getStopLocationsAlertsForSingleId() {
    var iut = serviceWithStopAlerts();

    assertThat(iut.getStopLocationsAlerts(List.of(id(BUS_STOP_ID)))).containsExactly(
      BUS_STOP_ALERT
    );
  }

  @Test
  void getStopLocationsAlertsForStopWithoutOwnAlert() {
    var iut = serviceWithStopAlerts();

    // the stop itself has no alert, so only the parent station alert is returned
    assertThat(
      iut.getStopLocationsAlerts(List.of(id(RAIL_PA_ID), id(RAIL_STATION_ID)))
    ).containsExactly(RAIL_STATION_ALERT);
  }

  @Test
  void getStopLocationsAlertsDeduplicatesAlerts() {
    var iut = new TransitAlertServiceImpl();
    var alert = TransitAlert.of(id("multi_stop_alert"))
      .addEntity(new EntitySelector.Stop(id(RAIL_P1_ID)))
      .addEntity(new EntitySelector.Stop(id(RAIL_STATION_ID)))
      .build();
    iut.setAlerts(List.of(alert));

    // the same alert matches both ids, but it is returned only once
    assertThat(
      iut.getStopLocationsAlerts(List.of(id(RAIL_P1_ID), id(RAIL_STATION_ID)))
    ).containsExactly(alert);
  }

  @Test
  void getStopLocationsAlertsIgnoresUnrelatedAndUnknownIds() {
    var iut = serviceWithStopAlerts();

    assertThat(
      iut.getStopLocationsAlerts(List.of(id(METRO_P1_ID), id(METRO_STATION_ID), id("unknown")))
    ).isEmpty();
  }

  @Test
  void getStopLocationsAlertsWithEmptyIdList() {
    var iut = serviceWithStopAlerts();

    assertThat(iut.getStopLocationsAlerts(List.of())).isEmpty();
  }

  private static TransitAlertServiceImpl serviceWithStopAlerts() {
    var service = new TransitAlertServiceImpl();
    service.setAlerts(List.of(RAIL_STATION_ALERT, RAIL_STOP_ALERT, BUS_STOP_ALERT));
    return service;
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }
}
