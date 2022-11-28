package org.opentripplanner.transit.speed_test.model.testcase;

import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RequestModes;

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

  public boolean departureTimeSet() {
    return departureTime != TestCase.NOT_SET;
  }
  public boolean arrivalTimeSet() {
    return arrivalTime != TestCase.NOT_SET;
  }
}
