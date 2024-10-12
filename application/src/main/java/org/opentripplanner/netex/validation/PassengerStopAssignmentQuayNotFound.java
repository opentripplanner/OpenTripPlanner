package org.opentripplanner.netex.validation;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.index.api.HMapValidationRule;
import org.opentripplanner.netex.issues.ObjectNotFound;

/**
 * Ensure quay exist for PassengerStopAssignment.
 */
class PassengerStopAssignmentQuayNotFound extends AbstractHMapValidationRule<String, String> {

  @Override
  public HMapValidationRule.Status validate(String quayRef) {
    return index.getQuayById().lookupLastVersionById(quayRef) == null ? Status.DISCARD : Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String stopPointRef, String quayRef) {
    return new ObjectNotFound("PassengerStopAssignment", stopPointRef, "quay", quayRef);
  }
}
