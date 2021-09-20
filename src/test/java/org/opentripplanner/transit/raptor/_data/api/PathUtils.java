package org.opentripplanner.transit.raptor._data.api;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;

/**
 * This utility help converting a Raptor path to a string witch is used in several
 * unit tests for easy comparison. The Stop index(1..n) is translated to stop names(A..N)
 * using {@link RaptorTestConstants#stopIndexToName(int)}.
 */
public class PathUtils {
  private static final RaptorTestConstants TRANSLATOR = new RaptorTestConstants() {};

  /** Util class, private constructor */
  private PathUtils() { }

  public static String pathsToString(RaptorResponse<TestTripSchedule> response) {
    return pathsToString(response.paths(), p -> p.toString(TRANSLATOR::stopIndexToName));
  }

  public static String pathsToStringDetailed(RaptorResponse<TestTripSchedule> response) {
    return pathsToString(response.paths(), p -> p.toStringDetailed(TRANSLATOR::stopIndexToName));
  }


  /* private methods */

  private static String pathsToString(
          Collection<Path<TestTripSchedule>> paths,
          Function<Path<?>, String> mapToStr
  ) {
    return paths.stream()
            .sorted()
            .map(mapToStr)
            .collect(Collectors.joining("\n"));
  }
}
