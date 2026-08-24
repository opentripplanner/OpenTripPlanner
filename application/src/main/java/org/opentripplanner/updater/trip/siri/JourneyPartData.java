package org.opentripplanner.updater.trip.siri;

/// Data extracted from siri RelatedJourneyPartStructure
record JourneyPartData(int fromPos, int toPos) {
  public JourneyPartData {
    if (fromPos >= toPos) {
      throw new IllegalArgumentException("toPos must be after fromPos");
    }
  }
}
