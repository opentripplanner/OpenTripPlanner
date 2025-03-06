package org.opentripplanner.updater.trip.siri;

import org.opentripplanner.utils.tostring.ToStringBuilder;
import uk.org.siri.siri20.DataFrameRefStructure;
import uk.org.siri.siri20.DatedVehicleJourneyRef;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.LineRef;
import uk.org.siri.siri20.OperatorRefStructure;
import uk.org.siri.siri20.VehicleRef;

/**
 * Create pretty strings for various SIRI elements, which is useful for debug printing.
 */
public class DebugString {

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
