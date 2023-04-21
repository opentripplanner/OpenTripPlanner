package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ArrayListMultimap;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NegativeDwellTime;
import org.opentripplanner.graph_builder.issues.NegativeHopTime;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (29.11.2017)
 */
class TripPatternMapperTest {

  private static final FeedScopedId SERVICE_ID = TransitModelForTest.id("S01");

  @Test
  void testMapTripPattern() {
    NetexTestDataSample sample = new NetexTestDataSample();

    TripPatternMapper tripPatternMapper = tripPatternMapper(
      sample,
      sample.getServiceJourneyById(),
      DataImportIssueStore.NOOP
    );

    TripPatternMapperResult r = tripPatternMapper.mapTripPattern(sample.getJourneyPattern());

    assertEquals(1, r.tripPatterns.size());

    TripPattern tripPattern = r.tripPatterns.values().stream().findFirst().orElseThrow();

    assertEquals(4, tripPattern.numberOfStops());
    assertEquals(1, tripPattern.scheduledTripsAsStream().count());

    Trip trip = tripPattern.scheduledTripsAsStream().findFirst().orElseThrow();

    assertEquals("RUT:ServiceJourney:1", trip.getId().getId());
    assertEquals("NSR:Quay:1", tripPattern.getStop(0).getId().getId());
    assertEquals("NSR:Quay:2", tripPattern.getStop(1).getId().getId());
    assertEquals("NSR:Quay:3", tripPattern.getStop(2).getId().getId());
    assertEquals("NSR:Quay:4", tripPattern.getStop(3).getId().getId());

    assertEquals(1, tripPattern.getScheduledTimetable().getTripTimes().size());

    TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes().get(0);

    assertEquals(4, tripTimes.getNumStops());

    assertEquals(18000, tripTimes.getDepartureTime(0));
    assertEquals(18240, tripTimes.getDepartureTime(1));
    assertEquals(18600, tripTimes.getDepartureTime(2));
    assertEquals(18900, tripTimes.getDepartureTime(3));
  }

  @Test
  void testMapTripPatternWithNonIncreasingDepartureTime() {
    NetexTestDataSample sample = new NetexTestDataSample();

    HierarchicalMapById<ServiceJourney> serviceJourneyById = sample.getServiceJourneyById();
    ServiceJourney serviceJourney = serviceJourneyById
      .localValues()
      .stream()
      .findFirst()
      .orElseThrow();
    // set the last departure time back to midnight
    serviceJourney
      .getPassingTimes()
      .getTimetabledPassingTime()
      .get(serviceJourney.getPassingTimes().getTimetabledPassingTime().size() - 1)
      .withDepartureTime(LocalTime.MIDNIGHT);

    DataImportIssueStore issueStore = new DefaultDataImportIssueStore();
    TripPatternMapper tripPatternMapper = tripPatternMapper(sample, serviceJourneyById, issueStore);

    TripPatternMapperResult r = tripPatternMapper.mapTripPattern(sample.getJourneyPattern());

    assertEquals(1, r.tripPatterns.size());
    TripPattern tripPattern = r.tripPatterns.values().stream().findFirst().orElseThrow();
    assertEquals(4, tripPattern.numberOfStops());
    assertEquals(
      0,
      tripPattern.scheduledTripsAsStream().count(),
      "Trips with non-increasing stop times should be dropped"
    );
    assertTrue(issueStore.listIssues().stream().anyMatch(NegativeHopTime.class::isInstance));
  }

  @Test
  void testMapTripPatternWithDepartureTimeBeforeArrivalTime() {
    NetexTestDataSample sample = new NetexTestDataSample();

    HierarchicalMapById<ServiceJourney> serviceJourneyById = sample.getServiceJourneyById();
    ServiceJourney serviceJourney = serviceJourneyById
      .localValues()
      .stream()
      .findFirst()
      .orElseThrow();
    // set the departure time before the arrival time
    TimetabledPassingTime timetabledPassingTime = serviceJourney
      .getPassingTimes()
      .getTimetabledPassingTime()
      .get(0);
    timetabledPassingTime.withArrivalTime(timetabledPassingTime.getDepartureTime().plusMinutes(1));

    DataImportIssueStore issueStore = new DefaultDataImportIssueStore();
    TripPatternMapper tripPatternMapper = tripPatternMapper(sample, serviceJourneyById, issueStore);

    TripPatternMapperResult r = tripPatternMapper.mapTripPattern(sample.getJourneyPattern());

    assertEquals(1, r.tripPatterns.size());
    TripPattern tripPattern = r.tripPatterns.values().stream().findFirst().orElseThrow();
    assertEquals(4, tripPattern.numberOfStops());
    assertEquals(
      0,
      tripPattern.scheduledTripsAsStream().count(),
      "Trips with departure time before arrival time should be dropped"
    );
    assertTrue(issueStore.listIssues().stream().anyMatch(NegativeDwellTime.class::isInstance));
  }

  @Test
  void testMapTripPatternWithDatedServiceJourney() {
    NetexTestDataSample sample = new NetexTestDataSample();

    HierarchicalMapById<DatedServiceJourney> datedServiceJourneys = new HierarchicalMapById<>();
    datedServiceJourneys.addAll(sample.getDatedServiceJourneyBySjId().values());

    TripPatternMapper tripPatternMapper = new TripPatternMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      new EntityById<>(),
      sample.getStopsById(),
      new EntityById<>(),
      new EntityById<>(),
      sample.getOtpRouteByid(),
      sample.getRouteById(),
      sample.getJourneyPatternById(),
      sample.getQuayIdByStopPointRef(),
      new HierarchicalMap<>(),
      new HierarchicalMapById<>(),
      sample.getServiceJourneyById(),
      new HierarchicalMapById<>(),
      new HierarchicalMapById<>(),
      sample.getOperatingDaysById(),
      datedServiceJourneys,
      sample.getDatedServiceJourneyBySjId(),
      Map.of(NetexTestDataSample.SERVICE_JOURNEY_ID, SERVICE_ID),
      new Deduplicator(),
      150
    );

    TripPatternMapperResult r = tripPatternMapper.mapTripPattern(sample.getJourneyPattern());

    assertEquals(1, r.tripPatterns.size());
    assertEquals(2, r.tripOnServiceDates.size());

    TripPattern tripPattern = r.tripPatterns.values().stream().findFirst().orElseThrow();
    Trip trip = tripPattern.scheduledTripsAsStream().findFirst().orElseThrow();

    for (TripOnServiceDate tripOnServiceDate : r.tripOnServiceDates) {
      assertEquals(trip, tripOnServiceDate.getTrip());
      assertEquals(TripAlteration.PLANNED, tripOnServiceDate.getTripAlteration());
      assertEquals(
        1,
        sample
          .getOperatingDaysById()
          .localValues()
          .stream()
          .map(OperatingDay::getId)
          .filter(id -> id.equals(tripOnServiceDate.getServiceDate().toString()))
          .count()
      );
    }
  }

  private static TripPatternMapper tripPatternMapper(
    NetexTestDataSample sample,
    HierarchicalMapById<ServiceJourney> serviceJourneyById,
    DataImportIssueStore issueStore
  ) {
    return new TripPatternMapper(
      issueStore,
      MappingSupport.ID_FACTORY,
      new EntityById<>(),
      sample.getStopsById(),
      new EntityById<>(),
      new EntityById<>(),
      sample.getOtpRouteByid(),
      sample.getRouteById(),
      sample.getJourneyPatternById(),
      sample.getQuayIdByStopPointRef(),
      new HierarchicalMap<>(),
      new HierarchicalMapById<>(),
      serviceJourneyById,
      new HierarchicalMapById<>(),
      new HierarchicalMapById<>(),
      new HierarchicalMapById<>(),
      new HierarchicalMapById<>(),
      ArrayListMultimap.create(),
      Map.of(NetexTestDataSample.SERVICE_JOURNEY_ID, SERVICE_ID),
      new Deduplicator(),
      150
    );
  }
}
