package org.opentripplanner.transit.raptor._data.api;

import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;

import java.util.List;
import java.util.stream.Collectors;

public class PathUtils {
  /** Util class, private constructor */
  private PathUtils() { }

  public static List<Path<TestTripSchedule>> pathsSorted(RaptorResponse<TestTripSchedule> response) {
    return response.paths().stream().sorted().collect(Collectors.toList());
  }

  public static String pathsToString(RaptorResponse<TestTripSchedule> response) {
    return pathsSorted(response).stream().map(Object::toString).collect(Collectors.joining("\n"));
  }
}
