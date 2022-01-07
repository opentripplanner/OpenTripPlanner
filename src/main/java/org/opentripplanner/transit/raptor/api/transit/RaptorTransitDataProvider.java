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
     * <p/>
     * The implementation may implement a lightweight {@link RaptorTransfer} representation.
     * The iterator element only needs to be valid for the duration og a single iterator step.
     * Hence; It is safe to use a cursor/flyweight pattern to represent both the Transfer
     * and the Iterator<Transfer> - this will most likely be the best performing implementation.
     * <p/>
     * Example:
     * <pre>
     *class LightweightTransferIterator implements Iterator&lt;RaptorTransfer&gt;, RaptorTransfer {
     *     private static final int[] EMPTY_ARRAY = new int[0];
     *     private final int[] a;
     *     private int index;
     *
     *     LightweightTransferIterator(int[] a) {
     *         this.a = a == null ? EMPTY_ARRAY : a;
     *         this.index = this.a.length == 0 ? 0 : -2;
     *     }
     *
     *     public int stop()              { return a[index]; }
     *     public int durationInSeconds() { return a[index+1]; }
     *     public boolean hasNext()       { index += 2; return index < a.length; }
     *     public RaptorTransfer next()   { return this; }
     * }
     * </pre>
     * @return a map of distances from the given input stop to all other stops.
     */
    Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop);

    /**
     * This method is responsible for providing all transfers to a given stop from all
     * possible stops around that stop.
     * See {@link #getTransfersFromStop(int)} for detail on how to implement this.
     * @return a map of distances to the given input stop from all other stops.
     */
    Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop);

    /**
     * Return a set of all patterns visiting the given set of stops.
     * <p/>
     * The implementation may implement a lightweight {@link RaptorTripPattern} representation.
     * See {@link #getTransfersFromStop(int)} for detail on how to implement this.
     *
     * @param stops set of stops for find all patterns for.
     */
    Iterator<? extends RaptorRoute<T>> routeIterator(IntIterator stops);

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
