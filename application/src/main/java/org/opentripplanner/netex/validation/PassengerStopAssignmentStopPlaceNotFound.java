package org.opentripplanner.netex.validation;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;

/**
 * Ensure stop place exists for PassengerStopAssignment.
 */
class PassengerStopAssignmentStopPlaceNotFound extends AbstractHMapValidationRule<String, String> {

  @Override
  public Status validate(String stopPlaceRef) {
    return index.getStopPlaceById().lookupLastVersionById(stopPlaceRef) == null
      ? Status.DISCARD
      : Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String stopPointRef, String stopPlaceRef) {
    return new ObjectNotFound("PassengerStopAssignment", stopPointRef, "stopPlace", stopPlaceRef);
  }
}
