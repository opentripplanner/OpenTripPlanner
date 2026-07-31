package org.opentripplanner.updater.trip.siri;

import java.time.LocalDate;
import javax.annotation.Nullable;
import org.opentripplanner.utils.time.ServiceDateUtils;
import uk.org.siri.siri21.DataFrameRefStructure;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;

/**
 * A pair of vehicle journey id and service date used to uniquely identify a DatedVehicleJourney.
 */
record VehicleJourneyIdAndServiceDate(
  @Nullable String vehicleJourneyId,
  @Nullable LocalDate serviceDate
) {
  @Nullable
  static VehicleJourneyIdAndServiceDate of(@Nullable FramedVehicleJourneyRefStructure ref) {
    if (ref == null) {
      return null;
    }
    return new VehicleJourneyIdAndServiceDate(
      ref.getDatedVehicleJourneyRef(),
      parseServiceDate(ref.getDataFrameRef())
    );
  }

  /**
   * The Nordic SIRI profile requires the DataFrameRef to contain the service date. A value that is
   * not a valid date is treated as a missing service date.
   */
  @Nullable
  private static LocalDate parseServiceDate(@Nullable DataFrameRefStructure dataFrameRef) {
    if (dataFrameRef == null || dataFrameRef.getValue() == null) {
      return null;
    }
    return ServiceDateUtils.parseStringToOptional(dataFrameRef.getValue()).orElse(null);
  }
}
