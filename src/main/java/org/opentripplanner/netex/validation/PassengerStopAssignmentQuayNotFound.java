package org.opentripplanner.netex.validation;

import org.opentripplanner.netex.index.api.HMapValidationRule;


/**
 * Ensure quay exist for PassengerStopAssignment.
 */
class PassengerStopAssignmentQuayNotFound extends AbstractHMapValidationRule<String, String> {
    @Override
    public HMapValidationRule.Status validate(String stopPointRef, String quayRef) {
        return index.getQuayById().lookupLastVersionById(quayRef) == null
            ? Status.DISCARD : Status.OK;
    }

    @Override
    public String logMessage(String key, String value) {
        return String.format("PassengerStopAssignment quay not found. scheduledStopPoint=%s, quay=%s", key, value);
    }
}
