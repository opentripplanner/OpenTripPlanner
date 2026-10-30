package org.opentripplanner.transfer.dataimport;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;

class StopsVisitedByPatternsTest {

  private static final TransitRepositoryForTest MODEL = TransitRepositoryForTest.of();
  private static final RegularStop A = MODEL.stop("A").build();
  private static final RegularStop B = MODEL.stop("B").build();
  private static final RegularStop C = MODEL.stop("C").build();
  private static final RegularStop D = MODEL.stop("D").build();

  private static final Trip TRIP_1 = TransitRepositoryForTest.trip("t1").build();
  private static final Trip TRIP_2 = TransitRepositoryForTest.trip("t2").build();
  private static final TripPattern P1 = TransitRepositoryForTest.tripPattern(
    "p1",
    TRIP_1.getRoute()
  )
    .withStopPattern(TransitRepositoryForTest.stopPattern(A, B))
    .build();
  private static final TripPattern P2 = TransitRepositoryForTest.tripPattern(
    "p2",
    TRIP_2.getRoute()
  )
    .withStopPattern(TransitRepositoryForTest.stopPattern(B, C))
    .build();

  private final StopsVisitedByPatterns subject = createSubject();

  @Test
  void containsTheStopsVisitedByAPattern() {
    assertThat(subject.contains(A.getId())).isTrue();
    assertThat(subject.contains(B.getId())).isTrue();
    assertThat(subject.contains(C.getId())).isTrue();
    assertThat(subject.contains(D.getId())).isFalse();
  }

  @Test
  void findsThePatternsVisitingAStop() {
    assertThat(subject.findPatterns(A.getId())).containsExactly(P1);
    assertThat(subject.findPatterns(B.getId())).containsExactly(P1, P2);
    assertThat(subject.findPatterns(D.getId())).isEmpty();
  }

  @Test
  void listsTheStopsOfEachPattern() {
    assertThat(subject.stopsOfEachPattern()).containsExactly(
      List.of(A.getId(), B.getId()),
      List.of(B.getId(), C.getId())
    );
  }

  private static StopsVisitedByPatterns createSubject() {
    var timetableDataImport = new TimetableDataImport();
    timetableDataImport.addTripPattern(P1.getId(), P1);
    timetableDataImport.addTripPattern(P2.getId(), P2);
    return new StopsVisitedByPatterns(timetableDataImport);
  }
}
