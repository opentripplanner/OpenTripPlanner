package org.opentripplanner.model;

import org.opentripplanner.routing.edgetype.TimetableSnapshot;

/**
 * This interface is used to retrieve the current instance of the TimetableSnapshot. Any provider
 * implementing this interface is responsible for thread-safe access to the latest valid
 * instance of the {@code TimetableSnapshot}.
 */
public interface TimetableSnapshotProvider {
    TimetableSnapshot getTimetableSnapshot();
}
