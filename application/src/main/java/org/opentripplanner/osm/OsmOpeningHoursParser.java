package org.opentripplanner.osm;

import static ch.poole.openinghoursparser.RuleModifier.Modifier.CLOSED;
import static ch.poole.openinghoursparser.RuleModifier.Modifier.OFF;
import static ch.poole.openinghoursparser.RuleModifier.Modifier.OPEN;
import static ch.poole.openinghoursparser.RuleModifier.Modifier.UNKNOWN;
import static java.util.Map.entry;

import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.OpeningHoursParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;
import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for parsing OpenStreetMap format opening hours into {@link OHCalendar}.
 *
 * Part of the code is copied from https://github.com/leonardehrenfried/opening-hours-evaluator
 */
public class OsmOpeningHoursParser {

  private static final Logger LOG = LoggerFactory.getLogger(OsmOpeningHoursParser.class);

  private final OpeningHoursCalendarService openingHoursCalendarService;

  private final Supplier<ZoneId> zoneIdSupplier;

  private final DataImportIssueStore issueStore;

  private static final Set<RuleModifier.Modifier> CLOSED_MODIFIERS = Set.of(CLOSED, OFF);
  private static final Set<RuleModifier.Modifier> OPEN_MODIFIERS = Set.of(OPEN, UNKNOWN);
  private static final Map<WeekDay, DayOfWeek> dayOfWeekMap = Map.ofEntries(
    entry(WeekDay.MO, DayOfWeek.MONDAY),
    entry(WeekDay.TU, DayOfWeek.TUESDAY),
    entry(WeekDay.WE, DayOfWeek.WEDNESDAY),
    entry(WeekDay.TH, DayOfWeek.THURSDAY),
    entry(WeekDay.FR, DayOfWeek.FRIDAY),
    entry(WeekDay.SA, DayOfWeek.SATURDAY),
    entry(WeekDay.SU, DayOfWeek.SUNDAY)
  );

  private static final Map<Month, java.time.Month> monthMap = Map.ofEntries(
    entry(Month.JAN, java.time.Month.JANUARY),
    entry(Month.FEB, java.time.Month.FEBRUARY),
    entry(Month.MAR, java.time.Month.MARCH),
    entry(Month.APR, java.time.Month.APRIL),
    entry(Month.MAY, java.time.Month.MAY),
    entry(Month.JUN, java.time.Month.JUNE),
    entry(Month.JUL, java.time.Month.JULY),
    entry(Month.AUG, java.time.Month.AUGUST),
    entry(Month.SEP, java.time.Month.SEPTEMBER),
    entry(Month.OCT, java.time.Month.OCTOBER),
    entry(Month.NOV, java.time.Month.NOVEMBER),
    entry(Month.DEC, java.time.Month.DECEMBER)
  );

  public OsmOpeningHoursParser(
    OpeningHoursCalendarService openingHoursCalendarService,
    DataImportIssueStore issueStore
  ) {
    this(openingHoursCalendarService, () -> null, issueStore);
  }

  public OsmOpeningHoursParser(
    OpeningHoursCalendarService openingHoursCalendarService,
    Supplier<ZoneId> zoneIdSupplier,
    DataImportIssueStore issueStore
  ) {
    this.openingHoursCalendarService = openingHoursCalendarService;
    // TODO, zoneId should depend on the coordinates of the object
    this.zoneIdSupplier = zoneIdSupplier;
    this.issueStore = issueStore;
  }

  public OsmOpeningHoursParser(
    OpeningHoursCalendarService openingHoursCalendarService,
    ZoneId zoneId
  ) {
    this.openingHoursCalendarService = openingHoursCalendarService;
    // TODO, zoneId should depend on the coordinates of the object
    this.zoneIdSupplier = () -> zoneId;
    this.issueStore = null;
  }

  public OHCalendar parseOpeningHours(String openingHoursTag, String id, String link)
    throws OpeningHoursParseException {
    ZoneId zoneId = zoneIdSupplier.get();
    if (zoneId == null) {
      return null;
    }
    return parseOpeningHours(openingHoursTag, id, link, zoneId);
  }

  /**
   * Builds a {@link OHCalendar} by parsing rules from OSM format opening hours.
   * Currently, doesn't have support for all types of rules.
   */
  public OHCalendar parseOpeningHours(
    String openingHoursTag,
    String id,
    String link,
    ZoneId zoneId
  ) throws OpeningHoursParseException {
    var calendarBuilder = openingHoursCalendarService.newBuilder(zoneId);
    var parser = new OpeningHoursParser(new ByteArrayInputStream(openingHoursTag.getBytes()));
    var rules = parser.rules(false);
    var rulesWithoutFallback = rules
      .stream()
      .filter(rule -> !rule.isFallBack())
      .collect(Collectors.toList());
    List<OHCalendarBuilder.OpeningHoursBuilder> openingHoursBuilders = new ArrayList<>();
    rulesWithoutFallback.forEach(rule -> {
      List<OHCalendarBuilder.OpeningHoursBuilder> openingHoursBuildersForRule = new ArrayList<>();
      if (is247Rule(rule)) {
        openingHoursBuildersForRule.add(createOHCalendarBuilderForOpen247(calendarBuilder));
      } else if (rule.getYears() != null) {
        // TODO
        logUnhandled(rule, openingHoursTag, id, link);
      } else if (rule.getDates() != null) {
        openingHoursBuildersForRule.addAll(createOHCalendarBuildersForDates(calendarBuilder, rule));
      } else if (rule.getWeeks() != null) {
        // TODO
        logUnhandled(rule, openingHoursTag, id, link);
      } else if (rule.getDays() != null) {
        openingHoursBuildersForRule.addAll(
          createOHCalendarBuildersForDayRanges(calendarBuilder, rule)
        );
      }
      if (isClosedRule(rule) && hasTimes(rule)) {
        // Regardless if the rules is additive or not, we should handle it as such if it closes
        // the object for a time range https://github.com/opening-hours/opening_hours.js/issues/53.
        openingHoursBuildersForRule.forEach(openingHoursBuilder ->
          openingHoursBuilders.addAll(
            splitPreviousBuilders(openingHoursBuilder, openingHoursBuilders)
          )
        );
      } else if (!rule.isAdditive()) {
        openingHoursBuildersForRule
          .stream()
          // If a builder is after midnight, there is always another one that can be used to set days
          // off in other builders without having to shift the days in two directions
          .filter(openingHoursBuilder -> !openingHoursBuilder.isAfterMidnight())
          .forEach(openingHoursBuilder ->
            editPreviousBuilders(openingHoursBuilder, openingHoursBuilders)
          );
      }
      if (isOpenRule(rule)) {
        openingHoursBuilders.addAll(
          openingHoursBuildersForRule
            .stream()
            .filter(OHCalendarBuilder.OpeningHoursBuilder::isEverOn)
            .collect(Collectors.toList())
        );
      }
    });
    openingHoursBuilders.forEach(openingHoursBuilder -> openingHoursBuilder.add());
    return calendarBuilder.build();
  }

  /**
   * Creates a {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder}
   * that is always open on the days defined in the rule's {@link DateRange}.
   */
  private List<OHCalendarBuilder.OpeningHoursBuilder> createOHCalendarBuildersForOpen24DayRanges(
    OHCalendarBuilder calendarBuilder,
    List<WeekDayRange> dayRanges
  ) {
    return dayRanges
      .stream()
      .map(dayRange ->
        setWeekDayRangeRangeForOpeningHoursBuilder(
          calendarBuilder.openingHours(dayRange.toString(), LocalTime.MIN, LocalTime.MAX),
          dayRange
        )
      )
      .collect(Collectors.toList());
  }

  /**
   * Creates a {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder}
   * that is open according to the {@link TimeSpan} in the rule on the days defined
   * in the rule's {@link DateRange} and {@link WeekDayRange}.
   *
   * TODO there are some unhandled things here
   */
  private List<OHCalendarBuilder.OpeningHoursBuilder> createOHCalendarBuildersForDates(
    OHCalendarBuilder calendarBuilder,
    Rule rule
  ) {
    return rule
      .getDates()
      .stream()
      .flatMap(dateRange -> {
        if (rule.getDays() != null) {
          return rule
            .getDays()
            .stream()
            .flatMap(weekDayRange -> {
              String description = String.format(
                "%s %s",
                dateRange.toString(),
                weekDayRange.toString()
              );
              if (rule.getTimes() != null) {
                return rule
                  .getTimes()
                  .stream()
                  .flatMap(timeSpan ->
                    createOHCalendarBuildersForTimeSpan(calendarBuilder, description, timeSpan)
                      .stream()
                      .map(openingHoursBuilder ->
                        setDateRangeForOpeningHoursBuilder(
                          openingHoursBuilder,
                          dateRange,
                          weekDayRange
                        )
                      )
                  );
              }
              return Stream.of();
            });
        }
        return Stream.of();
      })
      .collect(Collectors.toList());
  }

  /**
   * Sets provided {@link WeekDayRange} to be open according to the {@link DateRange} on a
   * {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder}.
   *
   * TODO there are a lot of unhandled things here
   */
  private OHCalendarBuilder.OpeningHoursBuilder setDateRangeForOpeningHoursBuilder(
    OHCalendarBuilder.OpeningHoursBuilder openingHoursBuilder,
    DateRange dateRange,
    WeekDayRange weekDayRange
  ) {
    var startDate = dateRange.getStartDate();
    var endDate = dateRange.getEndDate();
    if (weekDayRange != null) {
      if (weekDayRange.getStartDay() == null || startDate == null || startDate.getMonth() == null) {
        return openingHoursBuilder;
      }
      DayOfWeek startDayOfWeek = dayOfWeekMap.getOrDefault(weekDayRange.getStartDay(), null);
      DayOfWeek endDayOfWeek = weekDayRange.getEndDay() != null
        ? dayOfWeekMap.getOrDefault(weekDayRange.getEndDay(), null)
        : null;
      java.time.Month startMonth = monthMap.getOrDefault(startDate.getMonth(), null);
      java.time.Month endMonth = endDate != null && endDate.getMonth() != null
        ? monthMap.getOrDefault(endDate.getMonth(), null)
        : null;
      openingHoursBuilder.on(startMonth, endMonth, startDayOfWeek, endDayOfWeek);
    }
    return openingHoursBuilder;
  }

  /**
   * Creates a {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder}
   * that is open according to the {@link TimeSpan} in the rule or 24 hours a day on the days defined
   * in the rule's {@link WeekDayRange}.
   */
  private List<OHCalendarBuilder.OpeningHoursBuilder> createOHCalendarBuildersForDayRanges(
    OHCalendarBuilder calendarBuilder,
    Rule rule
  ) {
    if (rule.getTimes() == null) {
      return createOHCalendarBuildersForOpen24DayRanges(calendarBuilder, rule.getDays());
    }
    return rule
      .getDays()
      .stream()
      .flatMap(dayRange -> {
        String description = dayRange.toString();
        return rule
          .getTimes()
          .stream()
          .flatMap(timeSpan ->
            createOHCalendarBuildersForTimeSpan(calendarBuilder, description, timeSpan)
              .stream()
              .map(openingHoursBuilder ->
                setWeekDayRangeRangeForOpeningHoursBuilder(openingHoursBuilder, dayRange)
              )
          );
      })
      .collect(Collectors.toList());
  }

  /**
   * Sets provided weekday(s) to be open on a
   * {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder}.
   *
   * TODO there are a some unhandled things here
   */
  private OHCalendarBuilder.OpeningHoursBuilder setWeekDayRangeRangeForOpeningHoursBuilder(
    OHCalendarBuilder.OpeningHoursBuilder openingHoursBuilder,
    WeekDayRange weekDayRange
  ) {
    if (weekDayRange.getStartDay() == null) {
      return openingHoursBuilder;
    }
    DayOfWeek startDayOfWeek = dayOfWeekMap.getOrDefault(weekDayRange.getStartDay(), null);
    if (weekDayRange.getEndDay() != null) {
      return openingHoursBuilder.on(
        startDayOfWeek,
        dayOfWeekMap.getOrDefault(weekDayRange.getEndDay(), null)
      );
    } else {
      return openingHoursBuilder.on(startDayOfWeek);
    }
  }

  /**
   * Creates a {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder}
   * that is open according to the provided {@link TimeSpan} but doesn't set the builder to be open on any days.
   * That part should be handled by a separate method.
   */
  private List<OHCalendarBuilder.OpeningHoursBuilder> createOHCalendarBuildersForTimeSpan(
    OHCalendarBuilder calendarBuilder,
    String description,
    TimeSpan timeSpan
  ) {
    if (timeSpan.getStart() < 0) {
      // TODO We filter out timespans that have events like "sunrise-sunset" but maybe they could be implemented
      return List.of();
    }
    if (timeSpan.getEnd() > 1440) {
      return List.of(
        calendarBuilder.openingHours(description, getStartTime(timeSpan.getStart()), LocalTime.MAX),
        calendarBuilder.openingHours(
          description + " after midnight",
          LocalTime.MIN,
          getEndTime(timeSpan.getEnd() - 1440),
          true
        )
      );
    }
    return List.of(
      calendarBuilder.openingHours(
        description,
        getStartTime(timeSpan.getStart()),
        getEndTime(timeSpan.getEnd()),
        false
      )
    );
  }

  /**
   * Creates a {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder}
   * that is always open.
   */
  private OHCalendarBuilder.OpeningHoursBuilder createOHCalendarBuilderForOpen247(
    OHCalendarBuilder calendarBuilder
  ) {
    var openingHoursBuilder = calendarBuilder.openingHours(
      "Every day",
      LocalTime.MIN,
      LocalTime.MAX
    );
    return openingHoursBuilder.everyDay();
  }

  /**
   * Edits {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder}
   * that have been created based on previous rules so that they are not on the same days as in the
   * newly created builder based on a new rule that can partly override previous rules.
   */
  private void editPreviousBuilders(
    OHCalendarBuilder.OpeningHoursBuilder newOpeningHoursBuilder,
    List<OHCalendarBuilder.OpeningHoursBuilder> previousOpeningHoursBuilders
  ) {
    previousOpeningHoursBuilders
      .stream()
      .forEach(openingHoursBuilder -> openingHoursBuilder.offWithTimeShift(newOpeningHoursBuilder));
  }

  /**
   * For each opening hours builder added based on the previous rules, we do the following according to the
   * new builder created based of a closed/off rule:
   * 1. If time spans or days don't overlap, do nothing
   * 2. if the place is closed for the whole opening period, edit the old builder to be off on the common days
   * 3. if the place is closed for the beginning or end part of the opening period, edit the old builder to be
   *    off on common days and create a new builder that is open on those common days for the remaining part
   * 4. if the place is closed in the middle of the opening period, edit the old builder to be off on the common days
   *    and create two new builders that are open on the common days, one for the beginning and one for the end part of the opening period
   *
   * @return a list of new {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder} created while
   * splitting existing builders.
   */
  private List<OHCalendarBuilder.OpeningHoursBuilder> splitPreviousBuilders(
    OHCalendarBuilder.OpeningHoursBuilder closedOpeningHoursBuilder,
    List<OHCalendarBuilder.OpeningHoursBuilder> previousOpeningHoursBuilders
  ) {
    return previousOpeningHoursBuilders
      .stream()
      .flatMap(openingHoursBuilder -> {
        var openingHoursBuilderAndNewBuilders =
          openingHoursBuilder.createBuildersForRelativeComplement(closedOpeningHoursBuilder);
        return openingHoursBuilderAndNewBuilders.newBuilders().stream();
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  /**
   * Converts minutes from the start of day to {@link LocalTime}.
   */
  private LocalTime getStartTime(int startTimeMinutes) {
    return LocalTime.ofSecondOfDay(startTimeMinutes * 60);
  }

  /**
   * Converts minutes from the start of day to {@link LocalTime}.
   * if end time is 24:00, we use 23:59 instead and if end time is unknown (time is defined with 10+ format),
   * we also use 23:59.
   */
  private LocalTime getEndTime(int endTimeMinutes) {
    if (endTimeMinutes < 0) {
      return LocalTime.MAX;
    }
    return LocalTime.ofSecondOfDay(Math.min(endTimeMinutes * 60, 86399));
  }

  /**
   * Checks if rule doesn't have any modifiers or if the modifier is open or unknown.
   *
   * TODO if a modifier only has a comment (such as "by appointment"), we don't consider the rule to be of the open type but maybe we should
   */
  private boolean isOpenRule(Rule rule) {
    var modifier = rule.getModifier();
    return (
      modifier == null ||
      (modifier.getModifier() != null && OPEN_MODIFIERS.contains(modifier.getModifier()))
    );
  }

  /**
   * Checks if rule is explicitly defined is off or closed.
   */
  private boolean isClosedRule(Rule rule) {
    var modifier = rule.getModifier();
    return (
      modifier != null &&
      modifier.getModifier() != null &&
      CLOSED_MODIFIERS.contains(modifier.getModifier())
    );
  }

  /**
   * Checks if rule has times defined and there is a {@link TimeSpan} definition with start time.
   *
   * TODO We filter out {@link TimeSpan} that have events like "sunrise-sunset" but maybe they could be implemented
   */
  private boolean hasTimes(Rule rule) {
    return (
      rule.getTimes() != null &&
      rule.getTimes().stream().anyMatch(timeSpan -> timeSpan.getStart() > 0)
    );
  }

  /**
   * Checks if rule is just either 24/7 or open/closed/off.
   * If the rule is just a modifier such as "by appointment", we don't consider it to be open 24/7.
   */
  private boolean is247Rule(Rule rule) {
    return (
      (isOpenRule(rule) || isClosedRule(rule)) &&
      (rule.isTwentyfourseven() ||
        (rule.getHolidays() == null &&
          rule.getYears() == null &&
          rule.getDays() == null &&
          rule.getTimes() == null &&
          rule.getDates() == null &&
          rule.getWeeks() == null &&
          rule.getComment() == null))
    );
  }

  /**
   * Logs unhandled rule either with {@link Logger} or stores it in {@link DataImportIssueStore}.
   */
  private void logUnhandled(Rule rule, String ohTag, String id, String link) {
    var message = link != null
      ? String.format(
        "Rule %s is unhandled in the opening hours definition %s for %s (%s)",
        rule,
        ohTag,
        id,
        link
      )
      : String.format(
        "Rule %s is unhandled in the opening hours definition %s for %s",
        rule,
        ohTag,
        id
      );
    if (issueStore != null) {
      issueStore.add("UnhandledOHRule", message);
    } else {
      LOG.info(message);
    }
  }
}
