package org.opentripplanner.transit.raptor.api.transit;


import java.util.Iterator;


/**
 * This interface defines the data needed for the StdTransitWorker to do transit. This interface
 * define that role, and make it possible to write small adapter in between the "OTP Transit Layer"
 * and the Raptor algorithm. This also simplify the use of the Worker with other data sources,
 * importing and adapting this code into other software like OTP.
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
     * Hence; It is safe to use a cursor/flyweight pattern to represent both the TransferLeg
     * and the Iterator<TransferLeg> - this will most likely be the best performing
     * implementation.
     * <p/>
     * Example:
     * <pre>
     *class LightweightTransferIterator implements Iterator&lt;TransferLeg&gt;, TransferLeg {
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
     *     public TransferLeg next()   { return this; }
     * }
     * </pre>
     * @return a map of distances from the given input stop to all other stops.
     */
    Iterator<? extends RaptorTransfer> getTransfers(int fromStop);

    /**
     * Return a set of all patterns visiting the given set of stops.
     * <p/>
     * The implementation may implement a lightweight {@link RaptorTripPattern} representation.
     * See {@link #getTransfers(int)} for detail on how to implement this.
     *
     * @param stops set of stops for find all patterns for.
     */
    Iterator<? extends RaptorRoute<T>> routeIterator(IntIterator stops);

    /**
     * This is the total number of stops, it should be possible to retrieve transfers and pattern for every stop
     * from 0 to {@code numberOfStops()-1}.
     */
    int numberOfStops();


    /**
     * Return a the cost of boarding and alighting a trip at a particular stop.
     */
    int[] stopBoarAlightCost();
}
