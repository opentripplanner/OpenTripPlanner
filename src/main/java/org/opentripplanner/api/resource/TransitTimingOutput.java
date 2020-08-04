package org.opentripplanner.api.resource;

/**
 * Holds information to be included in the REST Response for debugging and profiling purposes of a
 * single transit routing search.
 */
public class TransitTimingOutput {

    /**
     * Time taken for the initialization of the raptor router in milliseconds.
     */
    public final long tripPatternFilterTime;

    /**
     * Time taken for the access and egress routing in milliseconds.
     */
    public final long accessEgressTime;

    /**
     * Time taken for the raptor search in milliseconds.
     */
    public final long raptorSearchTime;

    /**
     * Time taken for mapping from the raptor paths to itinerary objects in milliseconds.
     */
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
