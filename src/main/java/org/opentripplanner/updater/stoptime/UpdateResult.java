package org.opentripplanner.updater.stoptime;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.model.UpdateError;

public record UpdateResult(
  int successful,
  int failed,
  Multimap<UpdateError.UpdateErrorType, UpdateError> failures
) {
  public static UpdateResult empty() {
    return new UpdateResult(0, 0, ArrayListMultimap.create());
  }

  public static UpdateResult ofOptions(List<Optional<UpdateError>> results) {
    var errors = results.stream().filter(Optional::isPresent).map(Optional::get).toList();
    var successfullyApplied = results.stream().filter(Optional::isEmpty).count();
    var errorIndex = Multimaps.index(errors, UpdateError::errorType);
    return new UpdateResult((int) successfullyApplied, errors.size(), errorIndex);
  }
}
