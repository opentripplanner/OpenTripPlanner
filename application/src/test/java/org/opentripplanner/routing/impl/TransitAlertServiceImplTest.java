package org.opentripplanner.routing.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertEntityResolver;
import org.opentripplanner.transit.model.filter.transit.TransitAlertEntitySelectRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertSelectRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertStopOrStationSelectRequest;

class TransitAlertServiceImplTest {

  private static final String FEED_ID = "GB";
  private static final String RAIL_STATION_ID = "910GSTPX";
  private static final String RAIL_P1_ID = "9100STPX1";
  private static final String RAIL_PA_ID = "9100STPXBOXA";

  private static final String METRO_STATION_ID = "940GZZLUKSX";

  private static final String METRO_P1_ID = "9400ZZLUKSX1";

  private static final String BUS_STOP_ID = "490001276S";

  private static final TransitAlertEntityResolver NO_ENTITIES = request -> key -> false;

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

  @Test
  void listEntityKeys() {
    var iut = serviceWithStopAlerts();

    assertThat(iut.listEntityKeys()).containsExactly(
      new EntityKey.Stop(id(RAIL_STATION_ID)),
      new EntityKey.Stop(id(RAIL_P1_ID)),
      new EntityKey.Stop(id(BUS_STOP_ID))
    );
  }

  @Test
  void findAlertsForEntitiesDeduplicatesAlerts() {
    var iut = new TransitAlertServiceImpl();
    var alert = TransitAlert.of(id("multi_stop_alert"))
      .addEntity(new EntitySelector.Stop(id(RAIL_P1_ID)))
      .addEntity(new EntitySelector.Stop(id(RAIL_STATION_ID)))
      .build();
    iut.setAlerts(List.of(alert, BUS_STOP_ALERT));

    // the alert affects both entities, but it is returned only once
    assertThat(
      iut.findAlerts(
        List.of(new EntityKey.Stop(id(RAIL_P1_ID)), new EntityKey.Stop(id(RAIL_STATION_ID)))
      )
    ).containsExactly(alert);
  }

  private static TransitAlertServiceImpl serviceWithStopAlerts() {
    var service = new TransitAlertServiceImpl();
    service.setAlerts(List.of(RAIL_STATION_ALERT, RAIL_STOP_ALERT, BUS_STOP_ALERT));
    return service;
  }

  @Test
  void findAlertsWithoutFiltersReturnsAll() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT));

    assertThat(iut.findAlerts(TransitAlertRequest.of().build(), NO_ENTITIES)).containsExactly(
      ACCIDENT_ALERT,
      WEATHER_ALERT
    );
  }

  @Test
  void findAlertsSelectsMatchingCause() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT));

    var request = request(
      FilterRequest.<TransitAlertSelectRequest>of().addSelect(causeSelector(AlertCause.ACCIDENT))
    );

    assertThat(iut.findAlerts(request, NO_ENTITIES)).containsExactly(ACCIDENT_ALERT);
  }

  @Test
  void findAlertsExcludesMatchingCause() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT));

    var request = request(
      FilterRequest.<TransitAlertSelectRequest>of().addNot(causeSelector(AlertCause.ACCIDENT))
    );

    assertThat(iut.findAlerts(request, NO_ENTITIES)).containsExactly(WEATHER_ALERT);
  }

  @Test
  void findAlertsCombinesFiltersWithOr() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT));

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

    assertThat(iut.findAlerts(request, NO_ENTITIES)).containsExactly(ACCIDENT_ALERT, WEATHER_ALERT);
  }

  /**
   * The entities are resolved first, then the alerts of the selected entities are fetched and
   * finally the alert-level criteria are applied.
   */
  @Test
  void findAlertsSelectsAlertsOfSelectedEntities() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT, RAIL_STATION_ALERT));
    var resolver = resolverSelecting(new EntityKey.Stop(id(RAIL_P1_ID)));

    var entities = List.of(stopEntities());
    var select = TransitAlertSelectRequest.of().withEntities(entities).build();
    assertThat(
      iut.findAlerts(
        request(FilterRequest.<TransitAlertSelectRequest>of().addSelect(select)),
        resolver
      )
    ).containsExactly(ACCIDENT_ALERT);

    var selectWithCause = TransitAlertSelectRequest.of()
      .withEntities(entities)
      .withCauses(List.of(AlertCause.WEATHER))
      .build();
    assertThat(
      iut.findAlerts(
        request(FilterRequest.<TransitAlertSelectRequest>of().addSelect(selectWithCause)),
        resolver
      )
    ).isEmpty();
  }

  @Test
  void findAlertsExcludesAlertsOfSelectedEntities() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT));
    var resolver = resolverSelecting(new EntityKey.Stop(id(RAIL_P1_ID)));

    var not = TransitAlertSelectRequest.of().withEntities(List.of(stopEntities())).build();
    assertThat(
      iut.findAlerts(request(FilterRequest.<TransitAlertSelectRequest>of().addNot(not)), resolver)
    ).containsExactly(WEATHER_ALERT);
  }

  /**
   * An alert is only excluded by the entity criteria if all the entities it affects are excluded.
   */
  @Test
  void findAlertsDoesNotExcludeAlertsWhichAlsoAffectOtherEntities() {
    var railAndBusAlert = TransitAlert.of(id("rail_and_bus_alert"))
      .addEntity(new EntitySelector.Stop(id(RAIL_P1_ID)))
      .addEntity(new EntitySelector.Stop(id(BUS_STOP_ID)))
      .build();
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(RAIL_STOP_ALERT, BUS_STOP_ALERT, railAndBusAlert));
    var resolver = resolverSelecting(new EntityKey.Stop(id(RAIL_P1_ID)));

    var not = TransitAlertSelectRequest.of().withEntities(List.of(stopEntities())).build();
    assertThat(
      iut.findAlerts(request(FilterRequest.<TransitAlertSelectRequest>of().addNot(not)), resolver)
    ).containsExactly(BUS_STOP_ALERT, railAndBusAlert);

    var select = TransitAlertSelectRequest.of().withEntities(List.of(stopEntities())).build();
    assertThat(
      iut.findAlerts(
        request(FilterRequest.<TransitAlertSelectRequest>of().addSelect(select)),
        resolver
      )
    ).containsExactly(RAIL_STOP_ALERT, railAndBusAlert);
  }

  @Test
  void findAlertsResolvesEachEntitySelectorOnce() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT));
    var resolved = new ArrayList<TransitAlertEntitySelectRequest>();
    TransitAlertEntityResolver resolver = entities -> {
      resolved.add(entities);
      return key -> true;
    };

    var entities = stopEntities();
    var select = TransitAlertSelectRequest.of().withEntities(List.of(entities)).build();
    iut.findAlerts(
      request(FilterRequest.<TransitAlertSelectRequest>of().addSelect(select)),
      resolver
    );

    assertEquals(List.of(entities), resolved);
  }

  /**
   * The entity and alert criteria of a selector are applied together, so an alert of an entity
   * selected by one selector isn't matched by the alert criteria of another selector.
   */
  @Test
  void findAlertsKeepsCriteriaOfASelectorTogether() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT));
    var railStop = stopEntities(RAIL_P1_ID);
    var busStop = stopEntities(BUS_STOP_ID);
    TransitAlertEntityResolver resolver = entities -> {
      var stopId = entities.stopsOrStations().get().iterator().next().ids().get().iterator().next();
      return new EntityKey.Stop(stopId)::equals;
    };

    var railWeather = TransitAlertSelectRequest.of()
      .withEntities(List.of(railStop))
      .withCauses(List.of(AlertCause.WEATHER))
      .build();
    var busAccident = TransitAlertSelectRequest.of()
      .withEntities(List.of(busStop))
      .withCauses(List.of(AlertCause.ACCIDENT))
      .build();
    assertThat(
      iut.findAlerts(
        request(
          FilterRequest.<TransitAlertSelectRequest>of()
            .addSelect(railWeather)
            .addSelect(busAccident)
        ),
        resolver
      )
    ).isEmpty();

    var railAccident = TransitAlertSelectRequest.of()
      .withEntities(List.of(railStop))
      .withCauses(List.of(AlertCause.ACCIDENT))
      .build();
    assertThat(
      iut.findAlerts(
        request(
          FilterRequest.<TransitAlertSelectRequest>of()
            .addSelect(railAccident)
            .addSelect(busAccident)
        ),
        resolver
      )
    ).containsExactly(ACCIDENT_ALERT);
  }

  @Test
  void findAlertsResolvesIncludeAndExcludeSelectorsOnce() {
    var iut = new TransitAlertServiceImpl();
    iut.setAlerts(List.of(ACCIDENT_ALERT, WEATHER_ALERT));
    var resolved = new ArrayList<TransitAlertEntitySelectRequest>();
    TransitAlertEntityResolver resolver = entities -> {
      resolved.add(entities);
      return key -> true;
    };

    var entities = stopEntities();
    var selector = TransitAlertSelectRequest.of().withEntities(List.of(entities)).build();
    assertThat(
      iut.findAlerts(
        request(FilterRequest.<TransitAlertSelectRequest>of().addSelect(selector).addNot(selector)),
        resolver
      )
    ).isEmpty();

    assertEquals(List.of(entities), resolved);
  }

  private static TransitAlertEntityResolver resolverSelecting(EntityKey selected) {
    return entities -> selected::equals;
  }

  private static TransitAlertEntitySelectRequest stopEntities() {
    return stopEntities(RAIL_P1_ID);
  }

  private static TransitAlertEntitySelectRequest stopEntities(String stopId) {
    return TransitAlertEntitySelectRequest.of()
      .withStopsOrStations(
        List.of(
          TransitAlertStopOrStationSelectRequest.of()
            .withIds(List.of(id(stopId)))
            .build()
        )
      )
      .build();
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
