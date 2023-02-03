package org.opentripplanner.transit.model.plan;

import java.util.BitSet;
import java.util.List;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.calendar.CalendarDays;
import org.opentripplanner.transit.model.calendar.OperatingDay;
import org.opentripplanner.transit.model.calendar.PatternOnDay;
import org.opentripplanner.transit.model.calendar.PatternsOnDay;
import org.opentripplanner.transit.model.calendar.PatternsOnDays;
import org.opentripplanner.transit.model.calendar.TransitCalendar;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPatternV2;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.trip.Timetable;
import org.opentripplanner.transit.model.trip.timetable.TimetableBuilder;
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

  private static final Route ROUTE = Route
    .of(id("ROUTE_ID"))
    .withMode(TransitMode.BUS)
    .withAgency(AGENCY)
    .withLongName(new NonLocalizedString("ROUTE_LONG_NAME"))
    .build();

  private static final Timetable TIMETABLE = TimetableBuilder
    .of()
    .schedule("0:00 0:01 0:03")
    .build();

  private static final List<StopLocation> STOP_LIST = List.of(STOP_1, STOP_2, STOP_3);
  private static final BitSet ROUTE_BOARD_ALIGHT_BIT_SET = createBitSet(3);
  private static final CalendarDays CALENDAR_DAYS = CalendarDays.of().build();
  private static final RoutingTripPatternV2 ROUTING_TRIP_PATTERN = createRoutingTripPattern();
  private static final PatternsOnDays PATTERNS_ON_DAYS = createPatternsOnDays();

  public static final TransitCalendar TRANSIT_CALENDAR = new TransitCalendar(
    CALENDAR_DAYS,
    PATTERNS_ON_DAYS
  );

  private static RoutingTripPatternV2 createRoutingTripPattern() {
    return new RoutingTripPatternV2(
      STOP_LIST.stream().mapToInt(StopLocation::getIndex).toArray(),
      ROUTE_BOARD_ALIGHT_BIT_SET,
      ROUTE_BOARD_ALIGHT_BIT_SET,
      ROUTE.getMode(),
      "RoutingTripPattern name"
    );
  }

  private static PatternsOnDays createPatternsOnDays() {
    OperatingDay operatingDay = CALENDAR_DAYS.operatingDay(0);
    PatternOnDay patternOnDay = new PatternOnDay(operatingDay, ROUTING_TRIP_PATTERN, TIMETABLE);
    PatternsOnDay patternsOnDay = new PatternsOnDay(List.of(patternOnDay));
    return new PatternsOnDays(List.of(patternsOnDay));
  }

  private static BitSet createBitSet(int size) {
    BitSet bitSet = new BitSet(size);
    bitSet.set(0, size);
    return bitSet;
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }
}
