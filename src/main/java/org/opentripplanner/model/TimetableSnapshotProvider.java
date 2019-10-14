package org.opentripplanner.model;

/**
 * This interface is used to retrieve the current instance of the TimetableSnapshot. Any provider
 * implementing this interface is responsible for thread-safe access to the latest valid
 * instance of the {@code TimetableSnapshot}.
 *
 * Note that in the long run we don't necessarily want multiple snapshot providers. Ideally we'll just have one way of
 * handling these concurrency concerns, so no need for an interface and multiple implementations. But in the short term,
 * handling both GTFS-RT and SIRI has led to two different providers.
 */
public interface TimetableSnapshotProvider {
    TimetableSnapshot getTimetableSnapshot();
}
