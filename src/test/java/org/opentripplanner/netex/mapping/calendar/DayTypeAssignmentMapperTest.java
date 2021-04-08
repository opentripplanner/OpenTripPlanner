package org.opentripplanner.netex.mapping.calendar;

import org.junit.Test;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMultimap;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayType;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayTypeAssignment;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayTypeAssignmentWithOpDay;
import static org.opentripplanner.netex.NetexTestDataSupport.createDayTypeAssignmentWithPeriod;
import static org.opentripplanner.netex.NetexTestDataSupport.createOperatingDay;
import static org.opentripplanner.netex.NetexTestDataSupport.createOperatingPeriod;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.EVERYDAY;
import static org.rutebanken.netex.model.DayOfWeekEnumeration.WEEKDAYS;

public class DayTypeAssignmentMapperTest {
  private static final LocalDate D2020_10_21 = LocalDate.of(2020, 10, 21);
  private static final LocalDate D2020_11_01 = LocalDate.of(2020, 11, 1);
  private static final LocalDate D2020_11_03 = LocalDate.of(2020, 11, 3);
  private static final LocalDate D2020_11_27 = LocalDate.of(2020, 11, 27);
  private static final LocalDate D2020_11_30 = LocalDate.of(2020, 11, 30);
  private static final LocalDate D2020_12_22 = LocalDate.of(2020, 12, 22);
  private static final LocalDate D2020_12_24 = LocalDate.of(2020, 12, 24);
  private static final LocalDate D2020_12_31 = LocalDate.of(2020, 12, 31);

  private static final String OP_1 = "OP-1";
  private static final String OP_2 = "OP-2";
  private static final String OP_3 = "OP-3";

  private static final String OP_DAY_1 = "OD-1";

  private static final String DAY_TYPE_1 = "DT-1";
  private static final String DAY_TYPE_2 = "DT-2";

  private static final Boolean AVAILABLE = TRUE;
  private static final Boolean NOT_AVAILABLE = FALSE;

  private static final HierarchicalMapById<OperatingDay> EMPTY_OPERATING_DAYS = new HierarchicalMapById<>();
  private static final HierarchicalMapById<OperatingPeriod> EMPTY_PERIODS = new HierarchicalMapById<>();

  @Test
  public void mapDayTypesToLocalDatesForAGivenDate() {
    // GIVEN
    var dayTypes = new HierarchicalMapById<DayType>();
    var assignments = new HierarchicalMultimap<String, DayTypeAssignment>();

    // Simple assignments
    {
      // on 21.10.2020
      dayTypes.add(createDayType(DAY_TYPE_1));
      assignments.add(DAY_TYPE_1, createDayTypeAssignment(DAY_TYPE_1, D2020_10_21, AVAILABLE));
      // and 01.03.2020
      dayTypes.add(createDayType(DAY_TYPE_2));
      assignments.add(DAY_TYPE_2, createDayTypeAssignment(DAY_TYPE_2, D2020_11_01, AVAILABLE));
    }

    // WHEN - create calendar
    Map<String, Set<ServiceDate>> result = DayTypeAssignmentMapper.mapDayTypes(
        dayTypes,
        assignments,
        EMPTY_OPERATING_DAYS,
        EMPTY_PERIODS,
        null
    );

    // THEN - verify
    assertEquals("[2020-10-21]", toStr(result, DAY_TYPE_1));
    assertEquals("[2020-11-01]", toStr(result, DAY_TYPE_2));
  }

  @Test
  public void mapDayTypesToLocalDatesForSetOfDaysExceptSomeDays() {
    // GIVEN
    var dayTypes = new HierarchicalMapById<DayType>();
    var assignments = new HierarchicalMultimap<String, DayTypeAssignment>();

    dayTypes.add(createDayType(DAY_TYPE_1));

    // Add 01.11 and 03.11
    assignments.add(DAY_TYPE_1, createDayTypeAssignment(DAY_TYPE_1, D2020_11_01, AVAILABLE));
    assignments.add(DAY_TYPE_1, createDayTypeAssignment(DAY_TYPE_1, D2020_11_03, AVAILABLE));

    // Remove a day(30.11) that does not exist should not cause any problems
    assignments.add(DAY_TYPE_1, createDayTypeAssignment(DAY_TYPE_1, D2020_11_30, NOT_AVAILABLE));

    // Remove 03.11
    assignments.add(DAY_TYPE_1, createDayTypeAssignment(DAY_TYPE_1, D2020_11_03, NOT_AVAILABLE));


    // WHEN - create calendar
    Map<String, Set<ServiceDate>> result = DayTypeAssignmentMapper.mapDayTypes(
        dayTypes,
        assignments,
        EMPTY_OPERATING_DAYS,
        EMPTY_PERIODS,
        null
    );

    // THEN - verify
    assertEquals("[2020-11-01]", toStr(result, DAY_TYPE_1));
  }


  @Test
  public void mapDayTypesToLocalDatesWithOperatingDay() {
    // GIVEN
    var dayTypes = new HierarchicalMapById<DayType>();
    var assignments = new HierarchicalMultimap<String, DayTypeAssignment>();
    var operatingDays = new HierarchicalMapById<OperatingDay>();

    // With operating day ref on 21.10.2020
    dayTypes.add(createDayType(DAY_TYPE_1));
    operatingDays.add(createOperatingDay(OP_DAY_1, D2020_10_21));
    assignments.add(DAY_TYPE_1, createDayTypeAssignmentWithOpDay(DAY_TYPE_1, OP_DAY_1, AVAILABLE));

    // WHEN - create calendar
    Map<String, Set<ServiceDate>> result = DayTypeAssignmentMapper.mapDayTypes(
        dayTypes,
        assignments,
        operatingDays,
        EMPTY_PERIODS,
        null
    );

    // THEN - verify
    assertEquals("[2020-10-21]", toStr(result, DAY_TYPE_1));
  }

  @Test
  public void mapDayTypesToLocalDatesWithPeriods() {
    // GIVEN
    var dayTypes = new HierarchicalMapById<DayType>();
    var assignments = new HierarchicalMultimap<String, DayTypeAssignment>();
    var periods = new HierarchicalMapById<OperatingPeriod>();

    // Schedule in November
    {
      // Every day in November, except 6. - 23
      dayTypes.add(createDayType(DAY_TYPE_1, EVERYDAY));
      periods.add(createOperatingPeriod(OP_1, D2020_11_01, D2020_11_30));
      assignments.add(DAY_TYPE_1, createDayTypeAssignmentWithPeriod(DAY_TYPE_1, OP_1, AVAILABLE));
      // Except 06.11.2020 to 23.11.2020
      periods.add(createOperatingPeriod(OP_2, D2020_11_03, D2020_11_27));
      assignments.add(DAY_TYPE_1, createDayTypeAssignmentWithPeriod(DAY_TYPE_1, OP_2, NOT_AVAILABLE));
    }

    // WHEN - create calendar
    Map<String, Set<ServiceDate>> result = DayTypeAssignmentMapper.mapDayTypes(
        dayTypes,
        assignments,
        EMPTY_OPERATING_DAYS,
        periods,
        null
    );

    // THEN - verify
    assertEquals(
        "[2020-11-01, 2020-11-02, 2020-11-28, 2020-11-29, 2020-11-30]",
        toStr(result, DAY_TYPE_1)
    );
  }

  @Test
  public void mapDayTypesToLocalDatesWithPeriodExceptAGivenDate() {
    // GIVEN
    var dayTypes = new HierarchicalMapById<DayType>();
    var assignments = new HierarchicalMultimap<String, DayTypeAssignment>();
    var operatingDays = new HierarchicalMapById<OperatingDay>();
    var periods = new HierarchicalMapById<OperatingPeriod>();

    // All weekdays in December from 22. to 31. except 24.12
    {
      Boolean availableDefault = null;
      dayTypes.add(createDayType(DAY_TYPE_1, WEEKDAYS));
      periods.add(createOperatingPeriod(OP_3, D2020_12_22, D2020_12_31));
      assignments.add(DAY_TYPE_1, createDayTypeAssignmentWithPeriod(DAY_TYPE_1, OP_3, availableDefault));
      // Do not run service on christmas eve
      assignments.add(DAY_TYPE_1, createDayTypeAssignment(DAY_TYPE_1, D2020_12_24, NOT_AVAILABLE));
    }

    // WHEN - create calendar
    Map<String, Set<ServiceDate>> result = DayTypeAssignmentMapper.mapDayTypes(
        dayTypes,
        assignments,
        operatingDays,
        periods,
        null
    );

    // THEN - verify
    assertEquals(
        "[2020-12-22, 2020-12-23, 2020-12-25, 2020-12-28, 2020-12-29, 2020-12-30, 2020-12-31]",
        toStr(result, DAY_TYPE_1)
    );
  }


  /* private helper methods */

  private String toStr(Map<String, Set<ServiceDate>> result, String key) {
    return result.get(key).stream().sorted().collect(Collectors.toList()).toString();
  }
}