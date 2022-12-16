package org.opentripplanner.updater.trip;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.List;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.model.UpdateSuccess;
import org.opentripplanner.transit.model.framework.Result;

public record UpdateResult(
  int successful,
  int failed,
  Multimap<UpdateError.UpdateErrorType, UpdateError> failures,
  List<UpdateSuccess.WarningType> warnings
) {
  public static UpdateResult empty() {
    return new UpdateResult(0, 0, ArrayListMultimap.create(), List.of());
  }

  public static UpdateResult ofResults(List<Result<UpdateSuccess, UpdateError>> results) {
    var errors = results.stream().filter(Result::isFailure).map(Result::failureValue).toList();
    var successes = results.stream().filter(Result::isSuccess).map(Result::successValue).toList();
    var warnings = successes.stream().flatMap(s -> s.warnings().stream()).toList();
    var errorIndex = Multimaps.index(errors, UpdateError::errorType);
    return new UpdateResult(successes.size(), errors.size(), errorIndex, warnings);
  }
}
