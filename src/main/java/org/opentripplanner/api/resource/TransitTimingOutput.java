package org.opentripplanner.api.resource;

public class TransitTimingOutput {
    public final long tripPatternFilterTime;
    public final long accessEgressTime;
    public final long raptorSearchTime;
    public final long itineraryCreationTime;

    public TransitTimingOutput(
        long tripPatternFilterTime,
        long accessEgressTime,
        long raptorSearchTime,
        long itineraryCreationTime
    ) {
        this.tripPatternFilterTime = tripPatternFilterTime;
        this.accessEgressTime = accessEgressTime;
        this.raptorSearchTime = raptorSearchTime;
        this.itineraryCreationTime = itineraryCreationTime;
    }
}
