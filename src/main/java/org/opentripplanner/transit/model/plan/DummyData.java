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

  public static final RegularStop STOP_A = RegularStop
    .of(id("SA"))
    .withName("Majorstua")
    .withCoordinate(0, 0)
    .build();
  public static final RegularStop STOP_B = RegularStop
    .of(id("SB"))
    .withName("Stortinget")
    .withCoordinate(0, 0)
    .build();
  public static final RegularStop STOP_C = RegularStop
    .of(id("SC"))
    .withName("Oslo S")
    .withCoordinate(0, 0)
    .build();
  public static final RegularStop STOP_D = RegularStop
    .of(id("SD"))
    .withName("Tøyen")
    .withCoordinate(0, 0)
    .build();

  public static final StopModel STOP_MODEL = StopModel
    .of()
    .withRegularStop(STOP_A)
    .withRegularStop(STOP_B)
    .withRegularStop(STOP_C)
    .withRegularStop(STOP_D)
    .build();

  private static final Route ROUTE = Route
    .of(id("ROUTE_ID"))
    .withMode(TransitMode.BUS)
    .withAgency(AGENCY)
    .withLongName(new NonLocalizedString("ROUTE_LONG_NAME"))
    .build();

  private static final Timetable TIMETABLE_1 = TimetableBuilder
    .of()
    .schedule("0:03 0:05 0:08")
    .build();

  private static final Timetable TIMETABLE_2 = TimetableBuilder
    .of()
    .schedule("0:07 0:09 0:14")
    .build();

  private static final List<StopLocation> STOPS_IN_PATTERN = List.of(STOP_B, STOP_C, STOP_D);
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
      STOPS_IN_PATTERN.stream().mapToInt(StopLocation::getIndex).toArray(),
      ROUTE_BOARD_ALIGHT_BIT_SET,
      ROUTE_BOARD_ALIGHT_BIT_SET,
      ROUTE.getMode(),
      "RoutingTripPattern name"
    );
  }

  private static PatternsOnDays createPatternsOnDays() {
    OperatingDay operatingDay = CALENDAR_DAYS.operatingDay(0);
    PatternOnDay patternOnDay1 = new PatternOnDay(operatingDay, ROUTING_TRIP_PATTERN, TIMETABLE_1);
    PatternOnDay patternOnDay2 = new PatternOnDay(operatingDay, ROUTING_TRIP_PATTERN, TIMETABLE_2);
    PatternsOnDay patternsOnDay = new PatternsOnDay(List.of(patternOnDay1, patternOnDay2));
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
