package org.opentripplanner.raptorlegacy._data.api;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;

/**
 * This utility help converting a Raptor path to a string which is used in several unit tests for
 * easy comparison. The Stop index(1..n) is translated to stop names(A..N) using {@link
 * RaptorTestConstants#stopIndexToName(int)}.
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public class PathUtils {

  private static final RaptorTestConstants TRANSLATOR = new RaptorTestConstants() {};

  /** Util class, private constructor */
  private PathUtils() {}

  public static String pathsToString(Collection<? extends RaptorPath<?>> paths) {
    return pathsToString(paths, p -> p.toString(TRANSLATOR::stopIndexToName));
  }

  public static String pathsToStringDetailed(Collection<? extends RaptorPath<?>> paths) {
    return pathsToString(paths, p -> p.toStringDetailed(TRANSLATOR::stopIndexToName));
  }

  public static String pathsToString(
    Collection<? extends RaptorPath<?>> paths,
    Function<RaptorPath<?>, String> mapToStr
  ) {
    return paths.stream().sorted().map(mapToStr).collect(Collectors.joining("\n"));
  }
}
