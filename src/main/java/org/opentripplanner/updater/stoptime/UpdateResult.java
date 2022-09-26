package org.opentripplanner.updater.stoptime;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.UpdateError;

public record UpdateResult(
  int successful,
  int failed,
  Multimap<UpdateError.UpdateErrorType, UpdateError> failures
) {
  public static UpdateResult empty() {
    return new UpdateResult(0, 0, ArrayListMultimap.create());
  }
}
