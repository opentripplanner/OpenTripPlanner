package org.opentripplanner.updater.trip.siri;

import javax.annotation.Nullable;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;

/**
 * A pair of vehicle journey id and service date used to uniquely identify a DatedVehicleJourney.
 */
record VehicleJourneyIdAndServiceDate(
  @Nullable String vehicleJourneyId,
  @Nullable String serviceDate
) {
  @Nullable
  static VehicleJourneyIdAndServiceDate of(@Nullable FramedVehicleJourneyRefStructure ref) {
    if (ref == null) {
      return null;
    }
    var dataFrameRef = ref.getDataFrameRef();
    return new VehicleJourneyIdAndServiceDate(
      ref.getDatedVehicleJourneyRef(),
      dataFrameRef != null ? dataFrameRef.getValue() : null
    );
  }
}
