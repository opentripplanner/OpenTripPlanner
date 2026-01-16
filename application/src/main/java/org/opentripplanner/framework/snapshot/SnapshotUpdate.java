package org.opentripplanner.framework.snapshot;

public interface SnapshotUpdate {

  TransitSnapshot apply(TransitSnapshot snapshot);
}
