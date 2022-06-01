package org.opentripplanner.transit.raptor.speed_test.model.testcase;

import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

public record TestCaseDefinition(
  String id,
  String description,
  int departureTime,
  int arrivalTime,
  int window,
  GenericLocation fromPlace,
  GenericLocation toPlace,
  /**
   * A test cases can be grouped into a category used to group similar cases, like "Flex" or
   * "Long Distance".
   */
  String category,
  RequestModes modes
) {
  @Override
  public String toString() {
    return String.format(
      "#%s %s - %s, %s - %s, %s-%s(%s)",
      id,
      fromPlace.label,
      toPlace.label,
      fromPlace.getCoordinate(),
      toPlace.getCoordinate(),
      TimeUtils.timeToStrCompact(departureTime, TestCase.NOT_SET),
      TimeUtils.timeToStrCompact(arrivalTime, TestCase.NOT_SET),
      DurationUtils.durationToStr(window, TestCase.NOT_SET)
    );
  }

  /**
   * Return a short unique descriptive text for this test-case definition. The returned string is
   * a combination of {@code id} and {@code description}.
   */
  public String idAndDescription() {
    return id + " " + description;
  }
}
