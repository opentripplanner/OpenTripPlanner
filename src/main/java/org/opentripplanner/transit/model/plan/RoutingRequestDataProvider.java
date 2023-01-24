package org.opentripplanner.transit.model.plan;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.calendar.TransitCalendar;
import org.opentripplanner.transit.model.calendar.TripScheduleSearchOnDays;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.network.TripPatternBuilder;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.trip.RoutingTripPattern;
import org.opentripplanner.transit.model.trip.TripOnDate;

/**
 * This is the transit request scoped service to perform a routing request.
 */

/**
 * Stops: 0..3
 *
 * Stop on route (stop indexes):
 *   R1:  1 - 2 - 3
 *
 * Schedule:
 *   R1: 00:01 - 00:03 - 00:05
 *
 * Access (toStop & duration):
 *   1  30s
 *
 * Egress (fromStop & duration):
 *   3  20s
 */

public class RoutingRequestDataProvider implements RaptorTransitDataProvider<TripOnDate> {

  private static final FeedScopedId FEED_ID = new FeedScopedId("FEED_ID", "TRIP_PATTERN_ID");
  public static final StopPattern STOP_PATTERN = createStopPattern();

  private static StopPattern createStopPattern() {
    StopPattern.StopPatternBuilder stopPatternBuilder = StopPattern.create(3);
    stopPatternBuilder.stops[0] = RegularStop.of(new FeedScopedId("FEED_ID", "STOP_ID_1")).build();
    stopPatternBuilder.stops[1] = RegularStop.of(new FeedScopedId("FEED_ID", "STOP_ID_2")).build();
    stopPatternBuilder.stops[2] = RegularStop.of(new FeedScopedId("FEED_ID", "STOP_ID_3")).build();
    Arrays.fill(stopPatternBuilder.pickups, PickDrop.SCHEDULED);
    Arrays.fill(stopPatternBuilder.dropoffs, PickDrop.SCHEDULED);
    return stopPatternBuilder.build();
  }

  private final TransitCalendar transitCalendar;

  private static final int[] ROUTE_STOP_INDEX = new int[] { 1, 2, 3 };
  private static final BitSet ROUTE_BOARD_ALIGHT_BIT_SET = createBitSet(3);
  private static final Agency AGENCY = Agency
    .of(new FeedScopedId("FEED_ID", "AGENCY_ID"))
    .withName("AGENCY_NAME")
    .withTimezone("Europe/Oslo")
    .build();
  private static final Route ROUTE = Route
    .of(new FeedScopedId("FEED_ID", "ROUTE_ID"))
    .withMode(TransitMode.BUS)
    .withAgency(AGENCY)
    .withLongName(new NonLocalizedString("ROUTE_LONG_NAME"))
    .build();
  private static final org.opentripplanner.transit.model.network.RoutingTripPattern ROUTING_TRIP_PATTERN = createRoutingTripPattern();

  public static final RaptorSlackProvider SLACK_PROVIDER = new DefaultSlackProvider(60, 0, 0);

  private static org.opentripplanner.transit.model.network.RoutingTripPattern createRoutingTripPattern() {
    TripPatternBuilder tripPatternBuilder = TripPattern
      .of(FEED_ID)
      .withRoute(ROUTE)
      .withStopPattern(STOP_PATTERN);
    return new org.opentripplanner.transit.model.network.RoutingTripPattern(
      tripPatternBuilder.build(),
      tripPatternBuilder
    );
  }

  private TripPatternForDate tripPatternForDate = createTripPatternForDate();

  private TripPatternForDate createTripPatternForDate() {
    Trip trip = Trip.of(new FeedScopedId("FEED_ID", "TRIP_ID")).withRoute(ROUTE).build();

    StopTime st1 = new StopTime();
    st1.setDepartureTime(60);
    StopTime st2 = new StopTime();
    st1.setDepartureTime(180);
    StopTime st3 = new StopTime();
    st1.setDepartureTime(300);

    Collection<StopTime> stopTimes = List.of(st1, st2, st3);
    TripTimes tripTime = new TripTimes(trip, stopTimes, new Deduplicator());
    return new TripPatternForDate(
      ROUTING_TRIP_PATTERN,
      List.of(tripTime),
      List.of(),
      LocalDate.now()
    );
  }

  private static BitSet createBitSet(int size) {
    BitSet bitSet = new BitSet(size);
    for (int i = 0; i < size; i++) {
      bitSet.set(i);
    }
    return bitSet;
  }

  public RoutingRequestDataProvider(TransitCalendar transitCalendar) {
    this.transitCalendar = transitCalendar;
  }

  @Override
  public int numberOfStops() {
    // TODO RTM
    return 4;
  }

  @Override
  public IntIterator routeIndexIterator(IntIterator stops) {
    // TODO RTM
    BitSet patternMask = new BitSet(1);
    patternMask.set(0);
    return new BitSetIterator(patternMask);
  }

  @Override
  public RaptorRoute<TripOnDate> getRouteForIndex(int routeIndex) {
    // TODO RTM
    return new RaptorRouteAdaptor(
      new RoutingTripPattern(
        ROUTE_STOP_INDEX,
        ROUTE_BOARD_ALIGHT_BIT_SET,
        ROUTE_BOARD_ALIGHT_BIT_SET,
        TransitMode.BUS,
        "ROUTE_1"
      ),
      new TripScheduleSearchOnDays(tripPatternForDate, 0)
    );
  }

  @Override
  public RaptorPathConstrainedTransferSearch<TripOnDate> transferConstraintsSearch() {
    // TODO RTM
    return null;
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
    // TODO RTM
    return null;
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
    // TODO RTM
    return null;
  }

  @Override
  public CostCalculator multiCriteriaCostCalculator() {
    // TODO RTM
    return null;
  }

  @Override
  public RaptorSlackProvider slackProvider() {
    return SLACK_PROVIDER;
  }

  @Override
  public RaptorStopNameResolver stopNameResolver() {
    // TODO RTM
    return null;
  }

  @Override
  public int getValidTransitDataStartTime() {
    //TODO RTM
    return 0;
  }

  @Override
  public int getValidTransitDataEndTime() {
    //TODO RTM
    return Integer.MAX_VALUE;
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripOnDate> transferConstraintsForwardSearch(
    int routeIndex
  ) {
    return null;
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripOnDate> transferConstraintsReverseSearch(
    int routeIndex
  ) {
    return null;
  }
}
