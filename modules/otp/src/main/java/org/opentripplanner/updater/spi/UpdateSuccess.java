package org.opentripplanner.updater.spi;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.framework.collection.ListUtils;

/**
 * The result of a successful application of a realtime update, for example for trips or
 * vehicle positions. Its only extra information is a collection of possible warnings that
 * ought to be looked at but didn't prevent the application of the update.
 */
public record UpdateSuccess(List<WarningType> warnings) {
  /**
   * Create an instance with no warnings.
   */
  public static UpdateSuccess noWarnings() {
    return new UpdateSuccess(List.of());
  }
  /**
   * Create an instance with the provided warnings.
   */
  public static UpdateSuccess ofWarnings(WarningType... warnings) {
    return new UpdateSuccess(Arrays.asList(warnings));
  }

  /**
   * Return a copy of the instance with the provided warnings added.
   */
  public UpdateSuccess addWarnings(Collection<WarningType> addedWarnings) {
    return new UpdateSuccess(ListUtils.combine(this.warnings, addedWarnings));
  }

  public enum WarningType {
    /**
     * An added trip contained references to stops that are not in the static data. These
     * stops have been removed.
     */
    UNKNOWN_STOPS_REMOVED_FROM_ADDED_TRIP,
  }
}
