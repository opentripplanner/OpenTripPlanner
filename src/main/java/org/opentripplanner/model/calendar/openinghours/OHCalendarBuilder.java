package org.opentripplanner.model.calendar.openinghours;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class OHCalendarBuilder {

  private final Deduplicator deduplicator;
  private final LocalDate startOfPeriod;
  private final LocalDate endOfPeriod;
  private final int daysInPeriod;
  private final ZoneId zoneId;
  private final List<OpeningHours> openingHours = new ArrayList<>();

  public OHCalendarBuilder(
    Deduplicator deduplicator,
    LocalDate startOfPeriod,
    int daysInPeriod,
    ZoneId zoneId
  ) {
    this.deduplicator = deduplicator;
    this.startOfPeriod = startOfPeriod;
    this.endOfPeriod = startOfPeriod.plusDays(daysInPeriod);
    this.daysInPeriod = daysInPeriod;
    this.zoneId = zoneId;
  }

  public OpeningHoursBuilder openingHours(
    String periodDescription,
    LocalTime startTime,
    LocalTime endTime
  ) {
    return new OpeningHoursBuilder(periodDescription, startTime, endTime, false);
  }

  public OpeningHoursBuilder openingHours(
    String periodDescription,
    LocalTime startTime,
    LocalTime endTime,
    boolean isAfterMidnight
  ) {
    return new OpeningHoursBuilder(periodDescription, startTime, endTime, isAfterMidnight);
  }

  public OHCalendar build() {
    // We sort the opening hours for the deduplicator to work a little better and to simplify
    // the check can Enter/Exit later. Even if the opening hours are not on the same dates they
    // will still be sorted in the right order after day filtering
    Collections.sort(openingHours);
    return new OHCalendar(
      startOfPeriod,
      endOfPeriod,
      zoneId,
      deduplicator.deduplicateImmutableList(OpeningHours.class, openingHours)
    );
  }

  /**
   * Record that can be used for builder methods that create new builders that should be returned
   * in addition to the original builder.
   */
  public record OpeningHoursBuilderAndNewBuilders(
    OpeningHoursBuilder originalBuilder,
    List<OpeningHoursBuilder> newBuilders
  ) {}

  public class OpeningHoursBuilder {

    private String periodDescription;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private boolean afterMidnight;

    private final BitSet openingDays = new BitSet(daysInPeriod);

    public OpeningHoursBuilder(
      String periodDescription,
      LocalTime startTime,
      LocalTime endTime,
      boolean afterMidnight
    ) {
      this.periodDescription = periodDescription;
      this.startTime = startTime;
      this.endTime = endTime;
      this.afterMidnight = afterMidnight;
    }

    public boolean isAfterMidnight() {
      return afterMidnight;
    }

    /**
     * @return if the builder has any days set as open
     */
    public boolean isEverOn() {
      return !openingDays.isEmpty();
    }

    /**
     * Sets the defined date as open if it's within the defined period. If the builder is set be
     * for times after midnight, the date is shifted one day forward.
     */
    public OpeningHoursBuilder on(LocalDate date) {
      var shiftedDate = date.plusDays(afterMidnight ? 1 : 0);
      if (shiftedDate.isBefore(startOfPeriod) || shiftedDate.isAfter(endOfPeriod)) {
        return this;
      }
      openingDays.set((int) ChronoUnit.DAYS.between(startOfPeriod, shiftedDate));
      return this;
    }

    /**
     * Sets the defined week day to be open on every instance it exists within the defined period.
     * If the builder is set be for times after midnight, the weekday is shifted one day forward.
     */
    public OpeningHoursBuilder on(DayOfWeek dayOfWeek) {
      var shiftedDayOfWeek = dayOfWeek.plus(afterMidnight ? 1 : 0);
      // This counts how many days there are in between the startOfPeriod and
      // when the specified dayOfWeek occurs for the first time.
      int rawWeekDayDifference =
        shiftedDayOfWeek.getValue() - startOfPeriod.getDayOfWeek().getValue();
      int firstOccurrenceDaysFromStart = rawWeekDayDifference >= 0
        ? rawWeekDayDifference
        : 7 - Math.abs(rawWeekDayDifference);

      for (int i = firstOccurrenceDaysFromStart; i < daysInPeriod; i += 7) {
        openingDays.set(i);
      }
      return this;
    }

    /**
     * Sets every weekday in the range to be open on every instance they exist within the defined period.
     * The range is inclusive in both ends.
     * If the builder is set be for times after midnight, the weekdays are shifted one day forward.
     */
    public OpeningHoursBuilder on(DayOfWeek fromDayOfWeek, DayOfWeek untilDayOfWeek) {
      if (fromDayOfWeek == null) {
        return this;
      }
      if (untilDayOfWeek == null) {
        on(fromDayOfWeek);
        return this;
      }

      int untilAdjusted = fromDayOfWeek.getValue() > untilDayOfWeek.getValue()
        ? untilDayOfWeek.getValue() + 7
        : untilDayOfWeek.getValue();
      for (int i = fromDayOfWeek.getValue(); i <= untilAdjusted; i++) {
        int dayValue = i > 7 ? i - 7 : i;
        on(DayOfWeek.of(dayValue));
      }
      return this;
    }

    /**
     * Sets every weekday in the range to be open on every instance they exist within the defined period
     * on the defined month range. Both ranges are inclusive in both ends.
     * If the builder is set be for times after midnight, the days are shifted one day forward
     * so that first day of the first month is never on but the first day after the last month can be.
     */
    public OpeningHoursBuilder on(
      Month fromMonth,
      Month untilMonth,
      DayOfWeek fromDayOfWeek,
      DayOfWeek untilDayOfWeek
    ) {
      if (fromMonth == null || fromDayOfWeek == null) {
        return this;
      }

      Set<Month> months = new HashSet<>();
      if (untilMonth == null) {
        months.add(fromMonth);
      } else {
        int untilMonthAdjusted = fromMonth.getValue() > untilMonth.getValue()
          ? untilMonth.getValue() + 12
          : untilMonth.getValue();
        for (int i = fromMonth.getValue(); i <= untilMonthAdjusted; i++) {
          int monthValue = i > 12 ? i - 12 : i;
          months.add(Month.of(monthValue));
        }
      }

      Set<DayOfWeek> daysOfWeek = new HashSet<>();
      if (untilDayOfWeek == null) {
        daysOfWeek.add(fromDayOfWeek);
      } else {
        int untilDayAdjusted = fromDayOfWeek.getValue() > untilDayOfWeek.getValue()
          ? untilDayOfWeek.getValue() + 7
          : untilDayOfWeek.getValue();
        for (int i = fromDayOfWeek.getValue(); i <= untilDayAdjusted; i++) {
          int dayValue = i > 7 ? i - 7 : i;
          daysOfWeek.add(DayOfWeek.of(dayValue));
        }
      }

      var dateToProcess = afterMidnight ? startOfPeriod.minusDays(1) : startOfPeriod;
      int i = 0;
      while (i < daysInPeriod) {
        if (months.contains(dateToProcess.getMonth())) {
          if (daysOfWeek.contains(dateToProcess.getDayOfWeek())) {
            openingDays.set(i);
          }
          dateToProcess = dateToProcess.plusDays(1);
          i += 1;
        } else {
          int daysToSkip =
            YearMonth.of(dateToProcess.getYear(), dateToProcess.getMonth()).lengthOfMonth() -
            dateToProcess.getDayOfMonth() +
            1;
          dateToProcess = dateToProcess.plusDays(daysToSkip);
          i += daysToSkip;
        }
      }
      return this;
    }

    /**
     * Sets every day to be on.
     */
    public OpeningHoursBuilder everyDay() {
      openingDays.set(0, daysInPeriod);
      return this;
    }

    /**
     * Sets the days that are on in the given {@link OpeningHoursBuilder} to be off in this builder
     * and updates this builder's description to reflect that. The provided builder is unmodified.
     * If the builder is set be for times after midnight, we check if the previous day is set
     * in the provided bitset.
     */
    public OpeningHoursBuilder offWithTimeShift(OpeningHoursBuilder otherBuilder) {
      BitSet daysOff = otherBuilder.getOpeningDays();
      String offDescription = otherBuilder.getPeriodDescription();
      if (afterMidnight) {
        boolean intersects = false;
        // Java doesn't seem to have operations for shifting bits
        for (int i = 1; i < daysInPeriod; i++) {
          if (openingDays.get(i) && daysOff.get(i - 1)) {
            intersects = true;
            openingDays.clear(i);
          }
          if (intersects) {
            appendDescription(" except " + offDescription);
          }
        }
      } else {
        off(daysOff, offDescription);
      }
      return this;
    }

    /**
     * Edits this builder and potentially creates one or two new {@link OpeningHoursBuilder} based
     * on the provided {@link OpeningHoursBuilder} according to the following rules:
     * 1. If time spans or days don't overlap with this builder, do nothing and return 0 new builders.
     * 2. if the provided builder covers the whole period from this builder's start time to end time,
     *    edit this builder to be off on the common days and return 0 new builders.
     * 3. if the provided builder covers only the beginning or end part of this builder's opening
     *    period, edit this builder to be off on the common days and return a new builder that is
     *    open on those common days for the remaining part not covered by the provided builder.
     * 4. if the provided builder covers a period in the middle of this builder's opening period,
     *    edit this builder to be off on the common days and return two new builders that are open
     *    on the common days, one for the beginning and one for the end part of this builder's
     *    opening period
     *
     * @return a list of new {@link org.opentripplanner.model.calendar.openinghours.OHCalendarBuilder.OpeningHoursBuilder} created while
     * splitting existing builders.
     */
    public OpeningHoursBuilderAndNewBuilders createBuildersForRelativeComplement(
      OpeningHoursBuilder otherBuilder
    ) {
      LocalTime otherStartTime = otherBuilder.getStartTime();
      LocalTime otherEndTime = otherBuilder.getEndTime();
      if (
        otherEndTime.equals(startTime) ||
        otherEndTime.isBefore(startTime) ||
        endTime.equals(otherStartTime) ||
        endTime.isBefore(otherStartTime)
      ) {
        return new OpeningHoursBuilderAndNewBuilders(this, List.of());
      }
      String offDescription = otherBuilder.getPeriodDescription();
      if (
        (otherStartTime.isBefore(startTime) || otherStartTime.equals(startTime)) &&
        ((endTime.isBefore(otherEndTime) || endTime.equals(otherEndTime)))
      ) {
        off(otherBuilder.getOpeningDays(), offDescription);
        return new OpeningHoursBuilderAndNewBuilders(this, List.of());
      }
      BitSet commonDays = this.getCommonDays(otherBuilder);
      if (commonDays.isEmpty()) {
        return new OpeningHoursBuilderAndNewBuilders(this, List.of());
      }
      String newDescription = String.format(
        "Days overlapping between %s and %s",
        getPeriodDescription(),
        otherBuilder.getPeriodDescription()
      );
      if (otherStartTime.equals(startTime) || otherStartTime.isBefore(startTime)) {
        var newOpeningHoursBuilder = openingHours(newDescription, otherEndTime, endTime);
        newOpeningHoursBuilder.on(commonDays);
        off(commonDays, offDescription);
        return new OpeningHoursBuilderAndNewBuilders(this, List.of(newOpeningHoursBuilder));
      }
      if (endTime.equals(otherEndTime) || endTime.isBefore(otherEndTime)) {
        var newOpeningHoursBuilder = openingHours(newDescription, startTime, otherStartTime);
        newOpeningHoursBuilder.on(commonDays);
        off(commonDays, offDescription);
        return new OpeningHoursBuilderAndNewBuilders(this, List.of(newOpeningHoursBuilder));
      }
      var firstNewOpeningHoursBuilder = openingHours(newDescription, startTime, otherStartTime);
      firstNewOpeningHoursBuilder.on(commonDays);
      var secondNewOpeningHoursBuilder = openingHours(newDescription, otherEndTime, endTime);
      secondNewOpeningHoursBuilder.on(commonDays);
      off(commonDays, offDescription);
      return new OpeningHoursBuilderAndNewBuilders(
        this,
        List.of(firstNewOpeningHoursBuilder, secondNewOpeningHoursBuilder)
      );
    }

    /**
     * Adds the opening hours to the {@link OHCalendar} that is being build here.
     */
    public OHCalendarBuilder add() {
      var days = deduplicator.deduplicateBitSet(openingDays);
      var hours = deduplicator.deduplicateObject(
        OpeningHours.class,
        new OpeningHours(periodDescription, startTime, endTime, days)
      );
      openingHours.add(hours);
      return OHCalendarBuilder.this;
    }

    private BitSet getOpeningDays() {
      return openingDays;
    }

    private String getPeriodDescription() {
      return periodDescription;
    }

    private LocalTime getStartTime() {
      return startTime;
    }

    private LocalTime getEndTime() {
      return endTime;
    }

    private BitSet getCommonDays(OpeningHoursBuilder otherBuilder) {
      var openingDaysClone = (BitSet) openingDays.clone();
      openingDaysClone.and(otherBuilder.getOpeningDays());
      return openingDaysClone;
    }

    /**
     * Adds the given description addition to the end of the current description
     */
    private void appendDescription(String descriptionAddition) {
      periodDescription = periodDescription + descriptionAddition;
    }

    /**
     * Sets the days that are true in the given {@link BitSet} to be on without setting any days off.
     * If the builder is set be for times after midnight, the days are not shifted in this case.
     */
    private OpeningHoursBuilder on(BitSet days) {
      if (days.size() != openingDays.size()) {
        return this;
      }
      openingDays.or(days);
      return this;
    }

    /**
     * Sets the days that are true in the given {@link BitSet} to be off.
     * If the builder is set be for times after midnight, the days are not shifted in this case.
     */
    private OpeningHoursBuilder off(BitSet daysOff, String offDescription) {
      if (openingDays.intersects(daysOff)) {
        openingDays.andNot(daysOff);
        appendDescription(" except " + offDescription);
      }
      return this;
    }
  }
}
