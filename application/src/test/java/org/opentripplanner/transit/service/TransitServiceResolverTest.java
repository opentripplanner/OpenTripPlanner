package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;

class GenericLocationHelperTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final WgsCoordinate EXPLICIT = new WgsCoordinate(59.9, 10.7);

  private static final Station STATION = TEST_MODEL.station("C").build();
  private static final RegularStop STOP = TEST_MODEL.stop("A").withParentStation(STATION).build();
  private static final MultiModalStation MM_STATION = MultiModalStation.of(id("mm_s"))
    .withChildStations(List.of(STATION))
    .withCoordinate(new WgsCoordinate(60.1, 10.1))
    .withName(I18NString.of("MM1"))
    .build();
  private static final GroupOfStations GO_STATIONS = GroupOfStations.of(id("gos"))
    .addChildStation(MM_STATION)
    .withCoordinate(new WgsCoordinate(60.2, 10.2))
    .withName(I18NString.of("GOS"))
    .build();

  private final TransitService service = new DefaultTransitService(
    new TransitRepository(
      TEST_MODEL.siteRepositoryBuilder()
        .withRegularStop(STOP)
        .withStation(STATION)
        .withMultiModalStation(MM_STATION)
        .withGroupOfStation(GO_STATIONS)
        .build()
    ),
    DefaultTimetableRepository.scheduledOnly(new TimetableDataImport().toScheduledTimetableData())
  );

  @Test
  void explicitCoordinateTakesPrecedenceOverStopId() {
    var location = GenericLocation.fromStopIdWithFallback(
      STOP.getId(),
      EXPLICIT.latitude(),
      EXPLICIT.longitude(),
      null
    );
    assertEquals(EXPLICIT, GenericLocationHelper.findCoordinate(service, location));
  }

  @Test
  void stopIdResolvesToStopCoordinate() {
    var result = GenericLocationHelper.findCoordinate(
      service,
      GenericLocation.fromStopId(STOP.getId())
    );
    assertEquals(STOP.getCoordinate(), result);
  }

  @Test
  void stationIdResolvesToStationCoordinate() {
    var result = GenericLocationHelper.findCoordinate(
      service,
      GenericLocation.fromStopId(STATION.getId())
    );
    assertEquals(STATION.getCoordinate(), result);
  }

  @Test
  void groupOfStationsIdResolvesToGroupCoordinate() {
    var result = GenericLocationHelper.findCoordinate(
      service,
      GenericLocation.fromStopId(GO_STATIONS.getId())
    );
    assertEquals(GO_STATIONS.getCoordinate(), result);
  }

  @Test
  void unknownStopIdResolvesToNull() {
    assertNull(
      GenericLocationHelper.findCoordinate(service, GenericLocation.fromStopId(id("unknown")))
    );
  }
}
