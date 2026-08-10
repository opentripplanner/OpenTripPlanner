package org.opentripplanner.updater.trip.siri.support;

import javax.annotation.Nullable;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;

/**
 * Helper class for building a compact, human-readable reference to the trip that a SIRI
 * {@link EstimatedVehicleJourney} refers to.
 */
public class TripReferenceHelper {

  /**
   * A compact, best-effort identifier of the trip a journey refers to, used for logging when the
   * trip could not be resolved to an internal id (for example because the failure was raised before
   * resolution, or before the journey could even be wrapped). It mirrors the reference chain that
   * {@code EntityResolver} uses to resolve the trip: the framed vehicle journey (service journey id
   * and service date), the dated vehicle journey, the estimated vehicle journey code (added trips),
   * and finally the vehicle ref (used as the fuzzy matching key).
   *
   * @return the identifier, or {@code null} if the journey carries no usable reference
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
