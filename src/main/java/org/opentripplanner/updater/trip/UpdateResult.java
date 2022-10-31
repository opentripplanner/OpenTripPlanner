package org.opentripplanner.updater.trip;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.List;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.transit.model.framework.Result;

public record UpdateResult(
  int successful,
  int failed,
  Multimap<UpdateError.UpdateErrorType, UpdateError> failures
) {
  public static UpdateResult empty() {
    return new UpdateResult(0, 0, ArrayListMultimap.create());
  }

  public static UpdateResult ofResults(List<Result<?, UpdateError>> results) {
    var errors = results.stream().filter(Result::isFailure).map(Result::failureValue).toList();
    var successfullyApplied = results.stream().filter(Result::isSuccess).count();
    var errorIndex = Multimaps.index(errors, UpdateError::errorType);
    return new UpdateResult((int) successfullyApplied, errors.size(), errorIndex);
  }
}
