package org.opentripplanner.traffic;

/**
 * Keeps track of street speed snapshots, handles concurrency.
 */
public class StreetSpeedSnapshotSource {
    private StreetSpeedSnapshot snapshot;

    /** Get a speed snapshot. */
    // not synchronized; reference writes and reads are atomic in java
    public StreetSpeedSnapshot getSnapshot () {
        return this.snapshot;
    }

    public synchronized void setSnapshot(StreetSpeedSnapshot snapshot) {
        this.snapshot = snapshot;
    }
}
