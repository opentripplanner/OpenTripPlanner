package org.opentripplanner.transit.model.plan;

import java.util.Arrays;
import java.util.BitSet;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.calendar.CalendarDays;
import org.opentripplanner.transit.model.calendar.TransitCalendar;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPatternV2;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.network.TripPatternBuilder;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.StopModel;

public class DummyData {

  private static final String FEED_ID = "F";

  private static final Agency AGENCY = Agency
    .of(id("A1"))
    .withName("AGENCY_NAME")
    .withTimezone("Europe/Oslo")
    .build();

  public static final RegularStop STOP_1 = RegularStop
    .of(id("S1"))
    .withName("Majorstua")
    .withCoordinate(0, 0)
    .build();
  public static final RegularStop STOP_2 = RegularStop
    .of(id("S2"))
    .withName("Stortinget")
    .withCoordinate(0, 0)
    .build();
  public static final RegularStop STOP_3 = RegularStop
    .of(id("S3"))
    .withName("Oslo S")
    .withCoordinate(0, 0)
    .build();
  public static final RegularStop STOP_4 = RegularStop
    .of(id("S4"))
    .withName("Tøyen")
    .withCoordinate(0, 0)
    .build();

  public static final StopModel STOP_MODEL = StopModel
    .of()
    .withRegularStop(STOP_1)
    .withRegularStop(STOP_2)
    .withRegularStop(STOP_3)
    .withRegularStop(STOP_4)
    .build();

  public static final StopPattern STOP_PATTERN = createStopPattern();

  public static final TransitCalendar TRANSIT_CALENDAR = new TransitCalendar(
    CalendarDays.of().build()
  );

  private static final int[] ROUTE_STOP_INDEX = new int[] { 1, 2, 3 };
  private static final BitSet ROUTE_BOARD_ALIGHT_BIT_SET = createBitSet(3);
  private static final Route ROUTE = Route
    .of(id("ROUTE_ID"))
    .withMode(TransitMode.BUS)
    .withAgency(AGENCY)
    .withLongName(new NonLocalizedString("ROUTE_LONG_NAME"))
    .build();
  private static final RoutingTripPatternV2 ROUTING_TRIP_PATTERN = createRoutingTripPattern();

  public static final RaptorSlackProvider SLACK_PROVIDER = new DefaultSlackProvider(60, 0, 0);

  private static RoutingTripPatternV2 createRoutingTripPattern() {
    TripPatternBuilder tripPatternBuilder = TripPattern
      .of(id("TP1"))
      .withRoute(ROUTE)
      .withStopPattern(STOP_PATTERN);
    TripPattern tripPattern = tripPatternBuilder.build();
    int[] stopIndexes = tripPattern.getStops().stream().mapToInt(StopLocation::getIndex).toArray();
    return new RoutingTripPatternV2(
      stopIndexes,
      tripPattern.getRoutingTripPattern().getBoardingPossible(),
      tripPattern.getRoutingTripPattern().getAlightingPossible(),
      tripPattern.getMode(),
      tripPattern.getName()
    );
  }

  private static BitSet createBitSet(int size) {
    BitSet bitSet = new BitSet(size);
    bitSet.set(0, size);
    return bitSet;
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }

  private static StopPattern createStopPattern() {
    StopPattern.StopPatternBuilder stopPatternBuilder = StopPattern.create(3);
    stopPatternBuilder.stops[0] = STOP_1;
    stopPatternBuilder.stops[1] = STOP_2;
    stopPatternBuilder.stops[2] = STOP_3;
    Arrays.fill(stopPatternBuilder.pickups, PickDrop.SCHEDULED);
    Arrays.fill(stopPatternBuilder.dropoffs, PickDrop.SCHEDULED);
    return stopPatternBuilder.build();
  }
}
