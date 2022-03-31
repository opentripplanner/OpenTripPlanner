package org.opentripplanner.transit.raptor.api.transit;


import java.util.Iterator;
import javax.validation.constraints.NotNull;


/**
 * This interface defines the data needed by Raptor. It is the main/top-level interface and together
 * with the {@link org.opentripplanner.transit.raptor.api.request.RaptorRequest} if provide all
 * information needed by Raptor to perform the search. It makes it possible to write small adapter
 * between the "OTP Transit Layer" and the Raptor algorithm.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorTransitDataProvider<T extends RaptorTripSchedule> {

    /**
     * This method is called once, right after the constructor, before the routing start.
     * <p>
     * Strictly not needed, logic can be moved to constructor, but is separated out
     * to be able to measure performance as part of the route method.
     */
    default void setup() {}

    /**
     * This method is responsible for providing all transfers from a given stop to all
     * possible stops around that stop.
     * @return an array of {@link RaptorTransfer} from the given input stop to all other stops.
     */
    RaptorTransfer[] getTransfersFromStop(int fromStop);

    /**
     * This method is responsible for providing all transfers to a given stop from all
     * possible stops around that stop.
     * See {@link #getTransfersFromStop(int)} for detail on how to implement this.
     * @return an array of {@link RaptorTransfer} to the given input stop from all other stops.
     */
    RaptorTransfer[] getTransfersToStop(int toStop);

    /**
     * Return an iterator of route indices for all routes visiting the given set of stops.
     *
     * @param stops set of stops for find all routes for.
     */
    IntIterator routeIndexIterator(IntIterator stops);

    /**
     * Returns the raptor route for a specific route index
     * <p/>
     * The implementation may implement a lightweight {@link RaptorTripPattern} representation.
     * See {@link #getTransfersFromStop(int)} for detail on how to implement this.
     */
    RaptorRoute<T> getRouteForIndex(int routeIndex);

    /**
     * This is the total number of stops, it should be possible to retrieve transfers and pattern
     * for every stop from 0 to {@code numberOfStops()-1}.
     */
    int numberOfStops();

    /**
     * Create/provide the cost criteria calculator.
     */
    CostCalculator multiCriteriaCostCalculator();


    /**
     * Implement this method to provide a service to search for {@link RaptorTransferConstraint}.
     * This is not used during the routing, but after a path is found to attach constraint
     * information to the path.
     * <p>
     * The search should have good performance, but it is not a critical part of the overall
     * performance.
     */
    RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch();

    /**
     * Raptor relies on stop indexes for all references to stops for performance reasons, but
     * when a critical error occurs, it is nice to be able to inject information to the
     * log event or during debugging to see which stop it is. This is important to be able to
     * reproduce the error. This method is used by Raptor to translate from the stop index to a
     * string which should be short and identify the stop given the related pattern, for example
     * the stop name would be great.
     */
    @NotNull
    RaptorStopNameResolver stopNameResolver();

    /**
     * Returns the beginning of valid transit data. All trips running even partially after this time are included.
     * <p>
     * Unit: seconds since midnight of the day of the search.
     */
    int getValidTransitDataStartTime();

    /**
     * Returns the end time of valid transit data. All trips running even partially before this time are included.
     * <p>
     * Unit: seconds since midnight of the day of the search
     */
    int getValidTransitDataEndTime();
}
