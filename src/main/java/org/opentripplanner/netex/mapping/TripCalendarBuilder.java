package org.opentripplanner.netex.mapping;

import com.google.common.collect.Multimap;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.issues.ObjectNotFound;
import org.opentripplanner.netex.mapping.calendar.CalendarServiceBuilder;
import org.opentripplanner.netex.mapping.calendar.DatedServiceJourneyMapper;
import org.opentripplanner.netex.mapping.calendar.DayTypeAssignmentMapper;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.ServiceJourney;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TripCalendarBuilder {

  private final CalendarServiceBuilder calendarServiceBuilder;
  private final DataImportIssueStore issueStore;

  /** ServiceDates by dayType id */
  private HierarchicalMap<String, Set<ServiceDate>> dayTypeCalendars = new HierarchicalMap<>();
  private HierarchicalMap<String, Set<ServiceDate>> dsjBySJId = new HierarchicalMap<>();


  public TripCalendarBuilder(CalendarServiceBuilder calendarServiceBuilder, DataImportIssueStore issueStore) {
    this.calendarServiceBuilder = calendarServiceBuilder;
    this.issueStore = issueStore;
  }

  public void push() {
    this.dayTypeCalendars = new HierarchicalMap<>(dayTypeCalendars);
    this.dsjBySJId = new HierarchicalMap<>(dsjBySJId);
  }

  public void pop() {
    this.dayTypeCalendars = dayTypeCalendars.parent();
    this.dsjBySJId = dsjBySJId.parent();
  }

  /**
   * Map DayTypeAssignments and store them in a hierarchical map to be able to retrieve them later.
   */
  public void addDayTypeAssignments(
      ReadOnlyHierarchicalMapById<DayType> dayTypeById,
      ReadOnlyHierarchicalMap<String, Collection<DayTypeAssignment>> dayTypeAssignmentByDayTypeId,
      ReadOnlyHierarchicalMapById<OperatingDay> operatingDays,
      ReadOnlyHierarchicalMapById<OperatingPeriod> operatingPeriodById
  ) {
    dayTypeCalendars.addAll(
        DayTypeAssignmentMapper.mapDayTypes(
            dayTypeById,
            dayTypeAssignmentByDayTypeId,
            operatingDays,
            operatingPeriodById,
            issueStore
        )
    );
  }

  void addDatedServiceJourneys(
      ReadOnlyHierarchicalMapById<OperatingDay> operatingDayById,
      Multimap<String, DatedServiceJourney> datedServiceJourneyBySJId
  ) {
    for (String sjId : datedServiceJourneyBySJId.keySet()) {

      if (!dsjBySJId.containsKey(sjId)) { dsjBySJId.add(sjId, new HashSet<>()); }

      dsjBySJId.lookup(sjId).addAll(
          DatedServiceJourneyMapper.mapToServiceDates(
              datedServiceJourneyBySJId.get(sjId),
              operatingDayById
          )
      );
    }
  }


  Map<String, FeedScopedId> createTripCalendar(Iterable<ServiceJourney> serviceJourneys) {
    // Create a map to store the result
    Map<String, FeedScopedId> serviceIdsBySJId = new HashMap<>();

    for (ServiceJourney sj : serviceJourneys) {
      Set<ServiceDate> serviceDates;

      // Add scheduled dayTypes
      serviceDates = new HashSet<>(getServiceDatesForDayType(sj));

      // Add DatedServiceJourneys
      serviceDates.addAll(getDatesForDSJs(sj.getId()));

      // Add set of service-dates to service calendar. A serviceId for the set of days
      // is generated or fetched(if set already exist)
      FeedScopedId serviceId = calendarServiceBuilder.registerDatesAndGetServiceId(serviceDates);

      // Add service id to result
      serviceIdsBySJId.put(sj.getId(), serviceId);
    }
    return serviceIdsBySJId;
  }

  private Collection<ServiceDate> getServiceDatesForDayType(ServiceJourney sj) {
    DayTypeRefs_RelStructure dayTypes = sj.getDayTypes();

    if (dayTypes == null) { return List.of(); }

    List<ServiceDate> result = new ArrayList<>();

    for (JAXBElement<? extends DayTypeRefStructure> dt : dayTypes.getDayTypeRef()) {
      String dayTypeRef = dt.getValue().getRef();
      Set<ServiceDate> dates = dayTypeCalendars.lookup(dayTypeRef);
      if(dates != null) {
        result.addAll(dates);
      }
      else {
        reportSJDayTypeNotFound(sj, dayTypeRef);
      }
    }
    return result;
  }

  private Collection<ServiceDate> getDatesForDSJs(String sjId) {
    return dsjBySJId.containsKey(sjId) ? dsjBySJId.lookup(sjId) : List.of();
  }

  private void reportSJDayTypeNotFound(ServiceJourney sj, String dayTypeRef) {
    issueStore.add(
        new ObjectNotFound(
            "ServiceJourney", sj.getId(),
            "DayTypes", dayTypeRef
        )
    );
  }
}
