package org.opentripplanner.updater.trip.siri.support;

import org.jspecify.annotations.Nullable;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;

/**
 * Helper class for building a compact, human-readable reference to the trip that a SIRI
 * {@link EstimatedVehicleJourney} refers to.
 */
public class TripReferenceHelper {

  /**
   * A best-effort identifier of the trip a journey refers to, for journeys that could not be
   * resolved to a trip. In order of preference: the framed vehicle journey with its data frame, the
   * dated vehicle journey, the estimated vehicle journey code and the vehicle ref. {@code null} if
   * the journey has none of these.
   */
  @Nullable
  public static String tripReference(@Nullable EstimatedVehicleJourney journey) {
    if (journey == null) {
      return null;
    }
    FramedVehicleJourneyRefStructure framed = journey.getFramedVehicleJourneyRef();
    if (framed != null && framed.getDatedVehicleJourneyRef() != null) {
      var dataFrameRef = framed.getDataFrameRef();
      return dataFrameRef != null
        ? "%s (%s)".formatted(framed.getDatedVehicleJourneyRef(), dataFrameRef.getValue())
        : framed.getDatedVehicleJourneyRef();
    }
    if (journey.getDatedVehicleJourneyRef() != null) {
      return journey.getDatedVehicleJourneyRef().getValue();
    }
    if (journey.getEstimatedVehicleJourneyCode() != null) {
      return journey.getEstimatedVehicleJourneyCode();
    }
    if (journey.getVehicleRef() != null) {
      return journey.getVehicleRef().getValue();
    }
    return null;
  }
}
