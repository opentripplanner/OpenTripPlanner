package org.opentripplanner.analyst.scenario;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Modification is a single change that can be applied non-destructively to RaptorWorkerData.
 */
// we use the property "type" to determine what type of modification it is. The string values are defined here.
// Each class's getType should return the same value.
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "remove-trip", value = RemoveTrip.class),
        @JsonSubTypes.Type(name = "adjust-headway", value = AdjustHeadway.class),
        @JsonSubTypes.Type(name = "adjust-dwell-time", value = AdjustDwellTime.class),
        @JsonSubTypes.Type(name = "skip-stop", value = SkipStop.class),
        @JsonSubTypes.Type(name = "add-trip-pattern", value = AddTripPattern.class),
        @JsonSubTypes.Type(name = "convert-to-frequency", value = ConvertToFrequency.class),
        @JsonSubTypes.Type(name = "transfer-rule", value = TransferRule.class)
})
public abstract class Modification implements Serializable {

    /** Distinguish between modification types when a list of Modifications are serialized out as JSON. */
    public abstract String getType();

    /** Do nothing */
    public final void setType (String type) {
        /* do nothing */
    }

    public final Set<String> warnings = new HashSet<String>();
}
