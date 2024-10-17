package org.opentripplanner.transit.speed_test.model.testcase;

import java.time.Duration;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.framework.tostring.ValueObjectToStringBuilder;
import org.opentripplanner.model.GenericLocation;

public record TestCaseDefinition(
  String id,
  String description,
  int departureTime,
  int arrivalTime,
  Duration window,
  GenericLocation fromPlace,
  GenericLocation toPlace,
  /**
   * A test cases can be grouped into a category used to group similar cases, like "Flex" or
   * "Long Distance".
   */
  String category,
  QualifiedModeSet modes
) {
  @Override
  public String toString() {
    return String.format(
      "#%s %s - %s, %s - %s, %s-%s(%s)",
      id,
      fromPlace.label,
      toPlace.label,
      coordinateString(fromPlace),
      coordinateString(toPlace),
      TimeUtils.timeToStrCompact(departureTime, TestCase.NOT_SET),
      TimeUtils.timeToStrCompact(arrivalTime, TestCase.NOT_SET),
      DurationUtils.durationToStr(window)
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

  private String coordinateString(GenericLocation location) {
    return ValueObjectToStringBuilder.of().addCoordinate(location.lat, location.lng).toString();
  }
}
