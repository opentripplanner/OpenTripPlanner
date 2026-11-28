package org.opentripplanner.netex.validation;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;

/**
 * An assignment of a vehicle meeting point to a quay which does not exist is dropped. The meeting
 * point is then a stop at its own location.
 */
class VehicleMeetingPointAssignmentQuayNotFound extends AbstractHMapValidationRule<String, String> {

  @Override
  public Status validate(String quayRef) {
    return index.getQuayById().lookupLastVersionById(quayRef) == null ? Status.DISCARD : Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String vehicleMeetingPointRef, String quayRef) {
    return new ObjectNotFound(
      "VehicleMeetingPointAssignment",
      vehicleMeetingPointRef,
      "quay",
      quayRef
    );
  }
}
