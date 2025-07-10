package org.opentripplanner.apis.gtfs.support.filter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopType.LOCATION;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopType.LOCATION_GROUP;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopType.STOP;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopType;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

class StopArrivalByTypeFilterTest {

  public static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  public static final RegularStop REGULAR = TEST_MODEL.stop("r").build();
  public static final AreaStop AREA = TEST_MODEL.areaStop("a").build();
  public static final GroupStop GROUP = TEST_MODEL.groupStop("g", REGULAR, REGULAR);

  @Test
  void emptyList() {
    assertThrows(IllegalArgumentException.class, () -> new StopArrivalByTypeFilter(Set.of()));
  }

  @Test
  void nullValue() {
    assertNull(new StopArrivalByTypeFilter(null).filter(null));
  }

  private static List<Arguments> filterCases() {
    return List.of(
      Arguments.of(List.of(GROUP, REGULAR, AREA), null, List.of(GROUP, REGULAR, AREA)),
      Arguments.of(List.of(REGULAR), null, List.of(REGULAR)),
      Arguments.of(List.of(REGULAR), Set.of(STOP), List.of(REGULAR)),
      Arguments.of(List.of(REGULAR, AREA), Set.of(STOP), List.of(REGULAR)),
      Arguments.of(List.of(AREA, AREA), Set.of(STOP), List.of()),
      Arguments.of(List.of(GROUP), Set.of(STOP), List.of()),
      Arguments.of(List.of(GROUP, GROUP, GROUP, AREA), Set.of(STOP), List.of()),
      Arguments.of(List.of(GROUP), Set.of(LOCATION_GROUP), List.of(GROUP)),
      Arguments.of(List.of(GROUP, AREA), Set.of(LOCATION_GROUP), List.of(GROUP)),
      Arguments.of(List.of(GROUP, REGULAR, AREA), Set.of(LOCATION_GROUP), List.of(GROUP)),
      Arguments.of(
        List.of(GROUP, REGULAR, AREA),
        Set.of(LOCATION_GROUP, STOP),
        List.of(GROUP, REGULAR)
      ),
      Arguments.of(
        List.of(GROUP, REGULAR, AREA),
        Set.of(LOCATION_GROUP, STOP, LOCATION),
        List.of(GROUP, REGULAR, AREA)
      )
    );
  }

  @ParameterizedTest
  @MethodSource("filterCases")
  void filter(List<StopLocation> stops, Set<GraphQLStopType> include, List<StopLocation> expected) {
    var filter = new StopArrivalByTypeFilter(include);
    var filtered = filter.filter(stopArrivals(stops)).stream().map(sa -> sa.place.stop).toList();
    assertThat(filtered).containsExactlyElementsIn(expected);
  }

  private static List<StopArrival> stopArrivals(Collection<StopLocation> stops) {
    return stops
      .stream()
      .map(s -> new StopArrival(Place.forStop(s), null, null, 0, 1, false))
      .toList();
  }
}
