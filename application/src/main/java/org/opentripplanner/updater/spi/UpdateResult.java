package org.opentripplanner.updater.spi;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.List;

/**
 * An aggregation of results of the application of realtime updates which makes it easy to get
 * an overview of what the success rate of the update was and which specific problems were
 * encountered.
 */
public record UpdateResult(
  int successful,
  int failed,
  Multimap<UpdateErrorType, UpdateError> failures,
  List<UpdateSuccess> successes,
  List<UpdateError> errors
) {
  /**
   * Create an empty result.
   */
  public static UpdateResult empty() {
    return new UpdateResult(0, 0, ArrayListMultimap.create(), List.of(), List.of());
  }

  /**
   * Aggregate a list of results and errors into an instance of {@link UpdateResult}.
   */
  public static UpdateResult of(List<UpdateSuccess> successes, List<UpdateError> errors) {
    ImmutableListMultimap errorIndex = Multimaps.index(errors, UpdateError::errorType);
    return new UpdateResult(successes.size(), errors.size(), errorIndex, successes, errors);
  }
}
