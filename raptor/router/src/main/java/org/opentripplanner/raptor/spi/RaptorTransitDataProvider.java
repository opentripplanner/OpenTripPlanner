package org.opentripplanner.raptor.spi;

/**
 * This interface defines the data needed by Raptor. It is the main/top-level interface and
 * together with the {@code RaptorRequest} if provide all information needed by Raptor to perform
 * the search. It makes it possible to write small adapter between the "OTP Transit Layer" and the
 * Raptor algorithm.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorTransitDataProvider<T extends RaptorTripSchedule> {
  /**
   * This method is called once, right after the constructor, before the routing start.
   * <p>
   * Strictly not needed, logic can be moved to constructor, but is separated out to be able to
   * measure performance as part of the route method.
   */
  default void setup() {}

  /**
   * This is the total number of stops, it should be possible to retrieve transfers and pattern for
   * every stop from 0 to {@code numberOfStops()-1}.
   */
  int numberOfStops();

  /**
   * This is the total number of trip patterns. All trip patterns must have a index
   * ({@link RaptorTripPattern#patternIndex()}) < this value. Holes are acceptable.
   */
  int numberOfTripPatterns();

  /**
   * Return an iterator of route indices for all routes visiting the given set of stops.
   *
   * @param stops set of stops for find all routes for.
   */
  IntIterator routeIndexIterator(IntIterator stops);

  /**
   * Returns the raptor route for a specific route index
   * <p/>
   * The implementation may implement a lightweight {@link RaptorTripPattern} representation. See
   * {@link #getTransfersFromStop(int)} for detail on how to implement this.
   * @throws IndexOutOfBoundsException if routeIndex not found
   */
  RaptorRoute<T> getRouteForIndex(int routeIndex);

  /**
   * Create/provide the cost criteria calculator.
   * <p>
   * TODO - This should be moved the the request, it is not data but request specific.
   */
  RaptorCostCalculator<T> multiCriteriaCostCalculator();

  /**
   * The board-, alight- and transfer-slack provider.
   * <p>
   * TODO - This should be moved the the request, it is not data but request specific.
   */
  RaptorSlackProvider slackProvider();

  /**
   * Raptor relies on stop indexes for all references to stops for performance reasons, but when a
   * critical error occurs, it is nice to be able to inject information to the log event or during
   * debugging to see which stop it is. This is important to be able to reproduce the error. This
   * method is used by Raptor to translate from the stop index to a string which should be short and
   * identify the stop given the related pattern, for example the stop name would be great.
   */
  RaptorStopNameResolver stopNameResolver();

  /**
   * Returns the beginning of valid transit data. All trips running even partially after this time
   * are included.
   * <p>
   * Unit: seconds since midnight of the day of the search.
   */
  int getValidTransitDataStartTime();

  /**
   * Returns the end time of valid transit data. All trips running even partially before this time
   * are included.
   * <p>
   * Unit: seconds since midnight of the day of the search
   */
  int getValidTransitDataEndTime();

  /**
   * Return a reference for a give trip schedule. It can be stored and later used to fetch
   * {@link RaptorRoute}, {@link RaptorTripPattern} and {@link RaptorTripSchedule} later on.
   * <p>
   * <b>IMPLEMENTATION NOTES</b>
   * <p>
   * Raptor uses this to fetch information in places where the original Raptor routing context
   * (iterating over the stop of a pattern) is no longer available. Raptor could pass this
   * information down the call stack, but that would have an effect on the performance. An other
   * alternative is to add methods for this to the {@link RaptorTripSchedule}, but that would
   * couple the trip schedule to the route and trip-pattern.
   * <p>
   * This method is <em>NOT</em> performance critical, but it should not be slow.
   */
  RaptorTripScheduleReference tripScheduleReference(T trip);
}
