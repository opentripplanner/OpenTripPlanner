package org.opentripplanner.raptor._data.api;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.response.RaptorResponse;

/**
 * This utility help converting a Raptor path to a string which is used in several unit tests for
 * easy comparison. The Stop index(1..n) is translated to stop names(A..N) using {@link
 * RaptorTestConstants#stopIndexToName(int)}.
 */
public class PathUtils {

  private static final RaptorTestConstants TRANSLATOR = new RaptorTestConstants() {};

  /** Util class, private constructor */
  private PathUtils() {}

  public static String pathsToString(RaptorResponse<?> response) {
    return pathsToString(response.paths());
  }

  public static String pathsToString(Collection<? extends Path<?>> paths) {
    return pathsToString(paths, p -> p.toString(TRANSLATOR::stopIndexToName));
  }

  public static String pathsToStringDetailed(RaptorResponse<?> response) {
    return pathsToString(response.paths(), p -> p.toStringDetailed(TRANSLATOR::stopIndexToName));
  }

  public static String join(String... paths) {
    return String.join("\n", paths);
  }

  /* private methods */

  private static String pathsToString(
    Collection<? extends Path<?>> paths,
    Function<Path<?>, String> mapToStr
  ) {
    return paths.stream().sorted().map(mapToStr).collect(Collectors.joining("\n"));
  }
}
