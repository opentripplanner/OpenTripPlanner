package org.opentripplanner.updater.trip.siri;

import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import uk.org.siri.siri21.DataFrameRefStructure;
import uk.org.siri.siri21.DatedVehicleJourneyRef;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri21.LineRef;
import uk.org.siri.siri21.OperatorRefStructure;
import uk.org.siri.siri21.VehicleRef;

/**
 * Create pretty strings for various SIRI elements, which is useful for debug printing.
 */
public class DebugString {

  /**
   * A compact, best-effort identifier of the trip a journey refers to, used for logging when the
   * trip could not be resolved to an internal id (for example because the failure was raised before
   * resolution). It mirrors the reference chain that {@code EntityResolver} uses to resolve the
   * trip: the framed vehicle journey (service journey id and service date), the dated vehicle
   * journey, the estimated vehicle journey code (added trips), and finally the vehicle ref (used as
   * the fuzzy matching key).
   *
   * @return the identifier, or {@code null} if the journey carries no usable reference
   */
  @Nullable
  static String tripReference(EstimatedVehicleJourney journey) {
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

  static String of(EstimatedVehicleJourney estimatedVehicleJourney) {
    if (estimatedVehicleJourney == null) {
      return null;
    }

    return ToStringBuilder.of(estimatedVehicleJourney.getClass())
      .addStr(
        "EstimatedVehicleJourneyCode",
        estimatedVehicleJourney.getEstimatedVehicleJourneyCode()
      )
      .addObjOp(
        "DatedVehicleJourney",
        estimatedVehicleJourney.getDatedVehicleJourneyRef(),
        DatedVehicleJourneyRef::getValue
      )
      .addObjOp("FramedVehicleJourney", estimatedVehicleJourney.getFramedVehicleJourneyRef(), it ->
        ToStringBuilder.of(it.getClass())
          .addStr("VehicleJourney", it.getDatedVehicleJourneyRef())
          .addObjOp("Date", it.getDataFrameRef(), DataFrameRefStructure::getValue)
          .toString()
      )
      .addObjOp(
        "Operator",
        estimatedVehicleJourney.getOperatorRef(),
        OperatorRefStructure::getValue
      )
      .addCol("VehicleModes", estimatedVehicleJourney.getVehicleModes())
      .addObjOp("Line", estimatedVehicleJourney.getLineRef(), LineRef::getValue)
      .addObjOp("Vehicle", estimatedVehicleJourney.getVehicleRef(), VehicleRef::getValue)
      .toString();
  }
}
