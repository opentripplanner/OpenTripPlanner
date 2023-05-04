package org.opentripplanner.updater.spi;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.List;
import org.opentripplanner.transit.model.framework.Result;

/**
 * An aggregation of results of the application of realtime updates which makes it easy to get
 * an overview of what the success rate of the update was and which specific problems were
 * encountered.
 */
public record UpdateResult(
  int successful,
  int failed,
  Multimap<UpdateError.UpdateErrorType, UpdateError> failures,
  List<UpdateSuccess.WarningType> warnings
) {
  /**
   * Create an empty result.
   */
  public static UpdateResult empty() {
    return new UpdateResult(0, 0, ArrayListMultimap.create(), List.of());
  }

  /**
   * Aggregate a list of results into an instance of {@link UpdateResult}.
   */
  public static UpdateResult ofResults(List<Result<UpdateSuccess, UpdateError>> results) {
    var errors = results.stream().filter(Result::isFailure).map(Result::failureValue).toList();
    var successes = results.stream().filter(Result::isSuccess).map(Result::successValue).toList();
    var warnings = successes.stream().flatMap(s -> s.warnings().stream()).toList();
    var errorIndex = Multimaps.index(errors, UpdateError::errorType);
    return new UpdateResult(successes.size(), errors.size(), errorIndex, warnings);
  }
}
