package org.opentripplanner.model;

/**
 * This interface is used to retrieve the latest available instance of TimetableSnapshot
 * that is ready for use in routing. Slightly newer TimetableSnapshots may be available, but still
 * in the process of accumulating updates or being indexed and finalized for read-only routing use.
 * <p>
 * Any provider implementing this interface is responsible for ensuring access to the latest
 * {@code TimetableSnapshot} is handled in a thread-safe manner, as this method can be called by
 * any number of concurrent routing requests at once.
 * <p>
 * Note that in the long run we don't necessarily want multiple snapshot providers. Ideally we'll
 * just have one way of handling these concurrency concerns, so no need for an interface and
 * multiple implementations. But in the short term, handling both GTFS-RT and SIRI has led to two
 * different providers.
 */
public interface TimetableSnapshotProvider {
  TimetableSnapshot getTimetableSnapshot();
}
