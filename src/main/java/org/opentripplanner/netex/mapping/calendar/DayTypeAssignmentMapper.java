package org.opentripplanner.netex.mapping.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.issues.DayTypeScheduleIsEmpty;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PropertyOfDay;

/**
 * Map {@link DayTypeAssignment}s to set of {@link LocalDate}s.
 * <p>
 * Use the static {@code #mapDayTypes(...)} method to perform the mapping.
 * <p>
 * <b>DESIGN</b>
 * <p>
 * To simplify the logic in this class and avoid passing input parameters down the call chain this
 * class perform the mapping by first creating an instance with READ-ONLY input members. The result
 * is added to {@link #dates} and {@link #datesToRemove} during the mapping process. As a final step,
 * the to collections are merged (dates-datesToRemove) and then mapped to {@link LocalDate}s.
 * <p>
 * This class is THREAD-SAFE. A static mapping method is the single point of entry and a private
 * constructor ensure the instance is used in one thread only.
 */
public class DayTypeAssignmentMapper {

  // Input data
  private final DayType dayType;
  private final ReadOnlyHierarchicalMapById<OperatingDay> operatingDays;
  private final ReadOnlyHierarchicalMapById<OperatingPeriod> operatingPeriods;

  // Result data
  private final Set<LocalDate> dates = new HashSet<>();
  private final Set<LocalDate> datesToRemove = new HashSet<>();

  /**
   * This is private to block instantiating this class from outside. This enforce thread-safety
   * since this class is instantiated inside a static method. All input is READ-ONLY.
   */
  private DayTypeAssignmentMapper(
    DayType dayType,
    ReadOnlyHierarchicalMapById<OperatingDay> operatingDays,
    ReadOnlyHierarchicalMapById<OperatingPeriod> operatingPeriods
  ) {
    this.dayType = dayType;
    this.operatingDays = operatingDays;
    this.operatingPeriods = operatingPeriods;
  }

  /**
   * Map all given {@code dayTypeAssignments} into a map of {@link LocalDate} by {@code
   * dayTypeId}s.
   */
  public static Map<String, Set<LocalDate>> mapDayTypes(
    ReadOnlyHierarchicalMapById<DayType> dayTypes,
    ReadOnlyHierarchicalMap<String, Collection<DayTypeAssignment>> assignments,
    ReadOnlyHierarchicalMapById<OperatingDay> operatingDays,
    ReadOnlyHierarchicalMapById<OperatingPeriod> operatingPeriods,
    DataImportIssueStore issueStore
  ) {
    Map<String, Set<LocalDate>> result = new HashMap<>();

    for (var dayType : dayTypes.localValues()) {
      var mapper = new DayTypeAssignmentMapper(dayType, operatingDays, operatingPeriods);

      for (DayTypeAssignment it : assignments.lookup(dayType.getId())) {
        mapper.map(it);
      }
      Set<LocalDate> dates = mapper.mergeAndMapDates();

      if (dates.isEmpty()) {
        issueStore.add(new DayTypeScheduleIsEmpty(dayType.getId()));
      }

      result.put(dayType.getId(), dates);
    }
    return result;
  }

  /* private methods */

  private static boolean isDayTypeAvailableForAssigment(DayTypeAssignment dta) {
    if (dta.isIsAvailable() == null) {
      return true;
    }
    return dta.isIsAvailable();
  }

  private static Set<DayOfWeek> daysOfWeekForDayType(DayType dayType) {
    Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);

    if (dayType.getProperties() != null) {
      List<PropertyOfDay> propertyOfDays = dayType.getProperties().getPropertyOfDay();

      for (PropertyOfDay p : propertyOfDays) {
        result.addAll(DayOfWeekMapper.mapDayOfWeeks(p.getDaysOfWeek()));
      }
    }
    return result;
  }

  private void map(DayTypeAssignment dayTypeAssignment) {
    // Add or remove single days
    if (dayTypeAssignment.getDate() != null) {
      addSpecificDate(dayTypeAssignment);
    }
    // Add or remove periods
    else if (dayTypeAssignment.getOperatingPeriodRef() != null) {
      addOperationPeriod(dayTypeAssignment);
    } else if (dayTypeAssignment.getOperatingDayRef() != null) {
      var opd = operatingDays.lookup(dayTypeAssignment.getOperatingDayRef().getRef());
      addDate(true, opd.getCalendarDate());
    }
  }

  /**
   * When mapping two lists of dates are created internally in this class, these are merged as a
   * final step in the mapping process.
   * <p>
   * Do not call this method before you want to retrieve the result. Calling this method more than
   * once, may have unexpected effects.
   * <p>
   *
   * @return the list of service dates for all dayTypes mapped.
   */
  private Set<LocalDate> mergeAndMapDates() {
    dates.removeAll(datesToRemove);
    return new HashSet<>(dates);
  }

  private void addSpecificDate(DayTypeAssignment dayTypeAssignment) {
    addDate(isDayTypeAvailableForAssigment(dayTypeAssignment), dayTypeAssignment.getDate());
  }

  private void addOperationPeriod(DayTypeAssignment dayTypeAssignment) {
    boolean isAvailable = isDayTypeAvailableForAssigment(dayTypeAssignment);

    String ref = dayTypeAssignment.getOperatingPeriodRef().getRef();
    OperatingPeriod period = operatingPeriods.lookup(ref);

    if (period != null) {
      Set<DayOfWeek> daysOfWeek = daysOfWeekForDayType(dayType);

      // Plus 1 to make the end date exclusive - simplify the loop test
      LocalDateTime endDate = period.getToDate().plusDays(1);
      LocalDateTime date = period.getFromDate();

      for (; date.isBefore(endDate); date = date.plusDays(1)) {
        if (daysOfWeek.contains(date.getDayOfWeek())) {
          addDate(isAvailable, date);
        }
      }
    }
  }

  private void addDate(boolean isAvailable, LocalDateTime date) {
    if (isAvailable) {
      dates.add(date.toLocalDate());
    } else {
      datesToRemove.add(date.toLocalDate());
    }
  }
}
