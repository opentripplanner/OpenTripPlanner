package org.opentripplanner.ext.flex.template;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.area;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import gnu.trove.set.hash.TIntHashSet;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.transit.TripMatcherFactory;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.time.ServiceDateUtils;

class ClosestTripTest {

  private static final UnscheduledTrip FLEX_TRIP = UnscheduledTrip.of(id("123"))
    .withStopTimes(List.of(area("10:00", "11:00"), area("10:00", "11:00")))
    .withTrip(TimetableRepositoryForTest.trip("123").build())
    .build();

  private static final LocalDate DATE = LocalDate.of(2025, 2, 28);
  private static final FlexServiceDate FSD = new FlexServiceDate(
    DATE,
    ServiceDateUtils.secondsSinceStartOfTime(DATE.atStartOfDay(ZoneIds.BERLIN), DATE),
    10,
    new TIntHashSet()
  );
  private static final StopLocation STOP = FLEX_TRIP.getStop(0);
  private static final FlexAccessEgressCallbackAdapter ADAPTER =
    new FlexAccessEgressCallbackAdapter() {
      @Override
      public TransitStopVertex getStopVertexForStopId(FeedScopedId id) {
        return null;
      }

      @Override
      public Collection<PathTransfer> getTransfersFromStop(StopLocation stop) {
        return List.of();
      }

      @Override
      public Collection<PathTransfer> getTransfersToStop(StopLocation stop) {
        return List.of();
      }

      @Override
      public Collection<FlexTrip<?, ?>> getFlexTripsByStop(StopLocation stopLocation) {
        return List.of(FLEX_TRIP);
      }

      @Override
      public boolean isDateActive(FlexServiceDate date, FlexTrip<?, ?> trip) {
        return true;
      }
    };

  @Test
  void doNotFilter() {
    var request = TripRequest.of().build();
    var matcher = TripMatcherFactory.of(request, id -> Set.of(DATE));

    var trips = closestTrips(matcher);
    assertThat(trips).hasSize(1);
    assertEquals(List.copyOf(trips).getFirst().flexTrip(), FLEX_TRIP);
  }

  @Test
  void filter() {
    var request = TripRequest.of()
      .withExcludeAgencies(List.of(FLEX_TRIP.getTrip().getRoute().getAgency().getId()))
      .build();

    var matcher = TripMatcherFactory.of(request, id -> Set.of(DATE));

    var trips = closestTrips(matcher);
    assertThat(trips).isEmpty();
  }

  private static Collection<ClosestTrip> closestTrips(Matcher<Trip> matcher) {
    return ClosestTrip.of(
      ADAPTER,
      List.of(new NearbyStop(STOP, 100, List.of(), null)),
      matcher,
      List.of(FSD),
      true
    );
  }
}
