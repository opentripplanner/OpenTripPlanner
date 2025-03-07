package org.opentripplanner.raptor._data.api;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.response.RaptorResponse;

/**
 * This utility help converting a Raptor path to a string which is used in several unit tests for
 * easy comparison. The Stop index(1..n) is translated to stop names(A..N) using {@link
 * RaptorTestConstants#stopIndexToName(int)}.
 */
public class PathUtils {

  /** Util class, private constructor */
  private PathUtils() {}

  public static String pathsToString(RaptorResponse<?> response) {
    return pathsToString(response.paths());
  }

  public static String pathsToString(Collection<? extends RaptorPath<?>> paths) {
    return pathsToString(paths, p -> p.toString(RaptorTestConstants::stopIndexToName));
  }

  public static String pathsToStringDetailed(RaptorResponse<?> response) {
    return pathsToStringDetailed(response.paths());
  }

  public static String pathsToStringDetailed(Collection<? extends RaptorPath<?>> paths) {
    return pathsToString(paths, p -> p.toStringDetailed(RaptorTestConstants::stopIndexToName));
  }

  public static String join(String... paths) {
    return String.join("\n", paths);
  }

  public static String withoutCost(String path) {
    return path.replaceAll(" C₁[\\d_]+", "");
  }

  public static String[] withoutCost(String... paths) {
    return Stream.of(paths).map(path -> withoutCost(path)).toList().toArray(new String[0]);
  }

  public static String pathsToString(
    Collection<? extends RaptorPath<?>> paths,
    Function<RaptorPath<?>, String> mapToStr
  ) {
    return paths.stream().sorted().map(mapToStr).collect(Collectors.joining("\n"));
  }
}
