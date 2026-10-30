package org.opentripplanner.transfer.dataimport;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.stopPattern;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.tripPattern;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.transfer.regular.api.maxduration.AllowedMode;

class StopsAllowedForModesTest {

  private static final TransitRepositoryForTest MODEL = TransitRepositoryForTest.of();
  private static final RegularStop PLAIN_1 = MODEL.stop("plain-1").build();
  private static final RegularStop PLAIN_2 = MODEL.stop("plain-2").build();
  private static final RegularStop BICYCLE_1 = MODEL.stop("bicycle-1").build();
  private static final RegularStop BICYCLE_2 = MODEL.stop("bicycle-2").build();
  private static final RegularStop CAR_1 = MODEL.stop("car-1").build();
  private static final RegularStop CAR_2 = MODEL.stop("car-2").build();
  private static final RegularStop FERRY_1 = MODEL.stop("ferry-1").build();
  private static final RegularStop FERRY_2 = MODEL.stop("ferry-2").build();
  private static final RegularStop NOT_VISITED = MODEL.stop("not-visited").build();

  private final StopsAllowedForModes subject = createSubject();

  @Test
  void aStopThatNoTripAllowsAModeForHasNoModes() {
    assertThat(subject.modesAt(PLAIN_1.getId())).isEmpty();
    assertThat(subject.modesAt(PLAIN_2.getId())).isEmpty();
  }

  @Test
  void aStopVisitedByATripThatAllowsBicyclesAllowsBicycles() {
    assertThat(subject.modesAt(BICYCLE_1.getId())).containsExactly(AllowedMode.BICYCLE);
  }

  @Test
  void aStopVisitedByATripThatAllowsCarsAllowsCars() {
    assertThat(subject.modesAt(CAR_1.getId())).containsExactly(AllowedMode.CAR);
  }

  @Test
  void aStopVisitedByTripsThatAllowBothAllowsBoth() {
    assertThat(subject.modesAt(FERRY_1.getId())).containsExactly(
      AllowedMode.CAR,
      AllowedMode.BICYCLE
    );
  }

  @Test
  void aStopNoPatternVisitsAllowsBicyclesSoATrainThatCarriesBicyclesCanChangePlatform() {
    assertThat(subject.modesAt(NOT_VISITED.getId())).containsExactly(AllowedMode.BICYCLE);
  }

  private static StopsAllowedForModes createSubject() {
    var dataImport = new TimetableDataImport();
    addPattern(dataImport, "p1", TransitRepositoryForTest.trip("t1").build(), PLAIN_1, PLAIN_2);
    addPattern(
      dataImport,
      "p2",
      TransitRepositoryForTest.trip("t2").withBikesAllowed(BikeAccess.ALLOWED).build(),
      BICYCLE_1,
      BICYCLE_2
    );
    addPattern(
      dataImport,
      "p3",
      TransitRepositoryForTest.trip("t3").withCarsAllowed(CarAccess.ALLOWED).build(),
      CAR_1,
      CAR_2
    );
    addPattern(
      dataImport,
      "p4",
      TransitRepositoryForTest.trip("t4")
        .withBikesAllowed(BikeAccess.ALLOWED)
        .withCarsAllowed(CarAccess.ALLOWED)
        .build(),
      FERRY_1,
      FERRY_2
    );
    return new StopsAllowedForModes(dataImport, new StopsVisitedByPatterns(dataImport));
  }

  private static void addPattern(
    TimetableDataImport dataImport,
    String id,
    Trip trip,
    RegularStop... stops
  ) {
    var pattern = tripPattern(id, trip.getRoute())
      .withStopPattern(stopPattern(stops))
      .withScheduledTimeTableBuilder(builder ->
        builder.addTripTimes(
          ScheduledTripTimes.of().withTrip(trip).withDepartureTimes("00:00 01:00").build()
        )
      )
      .build();
    dataImport.addTripPattern(id(id), pattern);
  }
}
