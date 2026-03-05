package org.opentripplanner.transit.speed_test.model.testcase;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

public record TestCaseDefinition(
  String id,
  String description,
  int departureTime,
  int arrivalTime,
  Duration window,
  GenericLocation fromPlace,
  GenericLocation toPlace,
  @Nullable VisitViaLocation viaLocation,
  /**
   * A test case can be grouped into a category used to group similar cases, like "Flex" or
   * "Long Distance".
   */
  String category,
  QualifiedModeSet modes
) {
  @Override
  public String toString() {
    return String.format(
      "#%s %s - via:%s - %s, %s - via:%s - %s, %s-%s(%s)",
      id,
      fromPlace.label,
      viaLocation != null ? viaLocation.label() : null,
      toPlace.label,
      coordinateString(fromPlace),
      viaLocation != null ? coordinateString(viaLocation.coordinateLocation()) : null,
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
