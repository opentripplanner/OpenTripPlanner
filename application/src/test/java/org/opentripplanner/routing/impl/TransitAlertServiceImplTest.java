package org.opentripplanner.routing.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

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

  private static final TimetableRepository TIMETABLE_REPOSITORY = new TimetableRepository(
    getSiteRepository(),
    new Deduplicator()
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
    var iut = new TransitAlertServiceImpl(TIMETABLE_REPOSITORY);
    var railStationAlert = TransitAlert.of(id("rail_station_alert"))
      .addEntity(new EntitySelector.Stop(id(RAIL_STATION_ID)))
      .build();
    var railStopAlert = TransitAlert.of(id("rail_stop_alert"))
      .addEntity(new EntitySelector.Stop(id(RAIL_P1_ID)))
      .build();
    var busStopAlert = TransitAlert.of(id("bus_stop_alert"))
      .addEntity(new EntitySelector.Stop(id(BUS_STOP_ID)))
      .build();
    iut.setAlerts(List.of(railStationAlert, railStopAlert, busStopAlert));

    assertEquals(Set.of(railStationAlert), Set.copyOf(iut.getStopAlerts(id(RAIL_STATION_ID))));
    assertEquals(
      Set.of(railStationAlert, railStopAlert),
      Set.copyOf(iut.getStopAlerts(id(RAIL_P1_ID)))
    );
    assertEquals(Set.of(railStationAlert), Set.copyOf(iut.getStopAlerts(id(RAIL_PA_ID))));
    assertEquals(Set.of(), Set.copyOf(iut.getStopAlerts(id(METRO_STATION_ID))));
    assertEquals(Set.of(), Set.copyOf(iut.getStopAlerts(id(METRO_P1_ID))));
    assertEquals(Set.of(busStopAlert), Set.copyOf(iut.getStopAlerts(id(BUS_STOP_ID))));
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }
}
