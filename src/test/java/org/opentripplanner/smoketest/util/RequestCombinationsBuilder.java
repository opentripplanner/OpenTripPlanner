package org.opentripplanner.smoketest.util;

import static org.opentripplanner.client.parameters.TripPlanParameters.SearchDirection.ARRIVE_BY;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.RequestMode;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.opentripplanner.client.parameters.TripPlanParametersBuilder;
import org.opentripplanner.framework.collection.ListUtils;

/**
 * Generates all possible combinations of requests given input parameters.
 * For example the coordinates will be used for start and end, wheelchair will be on/off and the
 * search direction (depart at/arrive by) toggled.
 */
public class RequestCombinationsBuilder {

  private List<Coordinate> places;
  private LocalDateTime time;
  private Set<RequestMode> modes;
  private boolean includeWheelchair = false;
  private boolean includeArriveBy = false;

  public RequestCombinationsBuilder withLocations(Coordinate p1, Coordinate p2, Coordinate... p3) {
    this.places = ListUtils.combine(List.of(p1, p2), Arrays.asList(p3));
    return this;
  }

  public RequestCombinationsBuilder withTime(LocalDateTime time) {
    this.time = time;
    return this;
  }

  public RequestCombinationsBuilder includeWheelchair() {
    includeWheelchair = true;
    return this;
  }

  public RequestCombinationsBuilder includeArriveBy() {
    includeArriveBy = true;
    return this;
  }

  public RequestCombinationsBuilder withModes(RequestMode... requestMode) {
    this.modes = Set.copyOf(List.of(requestMode));
    return this;
  }

  public List<TripPlanParameters> build() {
    Stream<TripPlanParametersBuilder> builder = combineLocations(places);

    return builder
      .map(b -> b.withTime(time).withModes(modes))
      .flatMap(original -> duplicateIf(includeWheelchair, original, o -> o.withWheelchair(true)))
      .flatMap(original ->
        duplicateIf(includeArriveBy, original, o -> o.withSearchDirection(ARRIVE_BY))
      )
      .map(TripPlanParametersBuilder::build)
      .toList();
  }

  private Stream<TripPlanParametersBuilder> duplicateIf(
    boolean includeWheelchair1,
    TripPlanParametersBuilder original,
    Function<TripPlanParametersBuilder, TripPlanParametersBuilder> duplicate
  ) {
    if (includeWheelchair1) {
      var withWheelchair = duplicate.apply(original.copy());
      return Stream.of(original, withWheelchair);
    } else {
      return Stream.of(original);
    }
  }

  private static Stream<TripPlanParametersBuilder> combineLocations(List<Coordinate> places) {
    return places
      .stream()
      .flatMap(place -> {
        var builder = TripPlanParameters.builder().withFrom(place);
        return places.stream().filter(p -> !p.equals(place)).map(p -> builder.copy().withTo(p));
      });
  }
}
