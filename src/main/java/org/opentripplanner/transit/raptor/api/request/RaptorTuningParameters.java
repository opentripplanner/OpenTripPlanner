package org.opentripplanner.transit.raptor.api.request;


/**
 * Tuning parameters - changing these parameters change the performance (speed and/or memory consumption).
 */
public interface RaptorTuningParameters {

    /**
     * This parameter is used to allocate enough memory space for Raptor.
     * Set it to the maximum number of transfers for any given itinerary expected to
     * be found within the entire transit network.
     * <p/>
     * Default value is 12.
     */
    default int maxNumberOfTransfers() {
        return 12;
    }

    /**
     * This threshold is used to determine when to perform a binary trip schedule search
     * to reduce the number of trips departure time lookups and comparisons. When testing
     * with data from Entur and all of Norway as a Graph, the optimal value was about 50.
     * <p/>
     * If you calculate the departure time every time or want to fine tune the performance,
     * changing this may improve the performance a few percent.
     * <p/>
     * Default value is 50.
     */
    default int scheduledTripBinarySearchThreshold() {
        return 50;
    }

    /**
     * Step for departure times between each RangeRaptor iterations.
     * This is a performance optimization parameter.
     * A transit network usually uses minute resolution for the its timetable,
     * so to match that set this variable to 60 seconds. Setting it
     * to less then 60 will not give better result, but degrade performance.
     * Setting it to 120 seconds will improve performance, but you might get a
     * slack of 60 seconds somewhere in the result - most likely in the first
     * walking leg.
     * <p/>
     * Default value is 60.
     */
    default int iterationDepartureStepInSeconds() {
        return 60;
    }

    /**
     * Split a travel search in smaller jobs and run them in parallel to improve performance. Use this
     * parameter to set the total number of executable threads available across all searches.
     * <p/>
     * Multiple searches can run in parallel - this parameter have no effect with regard to that.
     * <p/>
     * The default value is 0 - zero. If 0, no extra threads are stated and the search is done in one thread.
     */
    default int searchThreadPoolSize() {
        return 0;
    }


    /**
     * Coefficients used to calculate raptor-search-window parameters dynamically  from heuristics.
     */
    default DynamicSearchWindowCoefficients dynamicSearchWindowCoefficients() {
        return new DynamicSearchWindowCoefficients() {};
    }
}
