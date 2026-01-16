package org.opentripplanner.framework.snapshot;

public class TripUpdate implements SnapshotUpdate {

  private final String tripId;

  public TripUpdate(String tripId) {
    this.tripId = tripId;
  }

  @Override
  public TransitSnapshot apply(TransitSnapshot snapshot) {
    return new TransitSnapshot(
      snapshot.getTransferRepo(),
      snapshot.getTimetableRepo().updateTrip(this.tripId)
    );
  }
}
