package org.opentripplanner.service.transitalert.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertSelectRequest;

class DefaultTransitAlertServiceTest {

  private static final String FEED_ID = "GB";
  private static final String RAIL_STATION_ID = "910GSTPX";
  private static final String RAIL_P1_ID = "9100STPX1";
  private static final String RAIL_PA_ID = "9100STPXBOXA";

  private static final String METRO_STATION_ID = "940GZZLUKSX";

  private static final String METRO_P1_ID = "9400ZZLUKSX1";

  private static final String BUS_STOP_ID = "490001276S";

  private static final TransitAlert RAIL_STATION_ALERT = TransitAlert.of(id("rail_station_alert"))
    .addEntity(new EntitySelector.Stop(id(RAIL_STATION_ID)))
    .build();
  private static final TransitAlert RAIL_STOP_ALERT = TransitAlert.of(id("rail_stop_alert"))
    .addEntity(new EntitySelector.Stop(id(RAIL_P1_ID)))
    .build();
  private static final TransitAlert BUS_STOP_ALERT = TransitAlert.of(id("bus_stop_alert"))
    .addEntity(new EntitySelector.Stop(id(BUS_STOP_ID)))
    .build();
  private static final TransitAlert ACCIDENT_ALERT = TransitAlert.of(id("accident_alert"))
    .addEntity(new EntitySelector.Stop(id(RAIL_P1_ID)))
    .withCause(AlertCause.ACCIDENT)
    .build();
  private static final TransitAlert WEATHER_ALERT = TransitAlert.of(id("weather_alert"))
    .addEntity(new EntitySelector.Stop(id(BUS_STOP_ID)))
    .withCause(AlertCause.WEATHER)
    .build();

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
    var alert = TransitAlert.of(id("multi_stop_alert"))
      .addEntity(new EntitySelector.Stop(id(RAIL_P1_ID)))
      .addEntity(new EntitySelector.Stop(id(RAIL_STATION_ID)))
      .build();
    var iut = serviceWith(List.of(alert));

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

  private static DefaultTransitAlertService serviceWithStopAlerts() {
    return serviceWith(List.of(RAIL_STATION_ALERT, RAIL_STOP_ALERT, BUS_STOP_ALERT));
  }

  private static DefaultTransitAlertService serviceWith(List<TransitAlert> alerts) {
    var repository = new DefaultTransitAlertRepository();
    repository.replaceAlerts(FEED_ID, alerts);
    return new DefaultTransitAlertService(repository.freeze());
  }

  @Test
  void findAlertsWithoutFiltersReturnsAll() {
    var iut = serviceWith(List.of(ACCIDENT_ALERT, WEATHER_ALERT));

    assertThat(iut.findAlerts(TransitAlertRequest.of().build())).containsExactly(
      ACCIDENT_ALERT,
      WEATHER_ALERT
    );
  }

  @Test
  void findAlertsSelectsMatchingCause() {
    var iut = serviceWith(List.of(ACCIDENT_ALERT, WEATHER_ALERT));

    var request = request(
      FilterRequest.<TransitAlertSelectRequest>of().addSelect(causeSelector(AlertCause.ACCIDENT))
    );

    assertThat(iut.findAlerts(request)).containsExactly(ACCIDENT_ALERT);
  }

  @Test
  void findAlertsExcludesMatchingCause() {
    var iut = serviceWith(List.of(ACCIDENT_ALERT, WEATHER_ALERT));

    var request = request(
      FilterRequest.<TransitAlertSelectRequest>of().addNot(causeSelector(AlertCause.ACCIDENT))
    );

    assertThat(iut.findAlerts(request)).containsExactly(WEATHER_ALERT);
  }

  @Test
  void findAlertsCombinesFiltersWithOr() {
    var iut = serviceWith(List.of(ACCIDENT_ALERT, WEATHER_ALERT));

    var request = TransitAlertRequest.of()
      .withFilters(
        List.of(
          FilterRequest.<TransitAlertSelectRequest>of()
            .addSelect(causeSelector(AlertCause.ACCIDENT))
            .build(),
          FilterRequest.<TransitAlertSelectRequest>of()
            .addSelect(causeSelector(AlertCause.WEATHER))
            .build()
        )
      )
      .build();

    assertThat(iut.findAlerts(request)).containsExactly(ACCIDENT_ALERT, WEATHER_ALERT);
  }

  private static TransitAlertRequest request(
    FilterRequest.Builder<TransitAlertSelectRequest> filter
  ) {
    return TransitAlertRequest.of().withFilters(List.of(filter.build())).build();
  }

  private static TransitAlertSelectRequest causeSelector(AlertCause cause) {
    return TransitAlertSelectRequest.of().withCauses(List.of(cause)).build();
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }
}
