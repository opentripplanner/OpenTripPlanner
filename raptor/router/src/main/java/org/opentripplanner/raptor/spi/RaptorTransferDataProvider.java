package org.opentripplanner.raptor.spi;

import java.util.Iterator;

/**
 * This interface is responsible for providing all regular and constrained transfers from a given stop to all possible
 * stops around that stop. The implementation may implement a lightweight {@link RaptorTransfer} representation for
 * regular stops.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorTransferDataProvider<T extends RaptorTripSchedule> {
  /**
   * This method is responsible for providing all transfers from a given stop to all possible stops
   * around that stop.
   * <p/>
   * The implementation may implement a lightweight {@link RaptorTransfer} representation. The
   * iterator element only needs to be valid for the duration og a single iterator step. Hence; It
   * is safe to use a cursor/flyweight pattern to represent both the Transfer and the
   * Iterator<Transfer> - this will most likely be the best performing implementation.
   * <p/>
   * Example:
   * <pre>
   * class LightweightTransferIterator implements Iterator&lt;RaptorTransfer&gt;, RaptorTransfer {
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
   *
   * @return a map of distances from the given input stop to all other stops.
   */
  @Flyweight
  Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop);

  /**
   * This method is responsible for providing all transfers to a given stop from all possible stops
   * around that stop. See {@link #getTransfersFromStop(int)} for detail on how to implement this.
   *
   * @return a map of distances to the given input stop from all other stops.
   */
  @Flyweight
  Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop);

  /**
   * Implement this method to provide a service to search for {@link RaptorTransferConstraint}. This
   * is not used during the routing, but after a path is found to attach constraint information to
   * the path.
   * <p>
   * The search should have good performance, but it is not a critical part of the overall
   * performance.
   */
  RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch();

  /**
   * List of transfers TO this pattern for each stop position in pattern used by Raptor during the
   * FORWARD search.
   */
  RaptorConstrainedBoardingSearch<T> transferConstraintsForwardSearch(int routeIndex);

  /**
   * List of transfers FROM this pattern for each stop position in pattern used by Raptor during the
   * REVERSE search.
   */
  RaptorConstrainedBoardingSearch<T> transferConstraintsReverseSearch(int routeIndex);
}
