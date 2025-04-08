package org.opentripplanner.updater.trip.siri.updater;

import java.util.Optional;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import uk.org.siri.siri21.Siri;

/**
 * Interface for a blocking, polling approach to retrieving SIRI realtime timetable updates.
 * TODO RT_AB: Clearly document whether the methods should return as fast as possible, or if they
 *  should intentionally block and wait for refreshed data, and how this fits into the design.
 */
public interface EstimatedTimetableSource {
  /**
   * Wait for one message to arrive, and decode it into a List of TripUpdates. Blocking call.
   *
   * @return a Siri-object potentially containing updates for several trips, or empty if an
   * exception occurred while processing the message.
   */
  Optional<Siri> getUpdates();

  /**
   * @return The incrementality of the last collection of updates.
   * {@link UpdateIncrementality}
   */
  UpdateIncrementality incrementalityOfLastUpdates();
}
