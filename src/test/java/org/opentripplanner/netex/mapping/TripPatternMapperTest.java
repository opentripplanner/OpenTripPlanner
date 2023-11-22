package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ArrayListMultimap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.OperatingDay;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (29.11.2017)
 */
public class TripPatternMapperTest {

  private static final FeedScopedId SERVICE_ID = TransitModelForTest.id("S01");

  @Test
  public void testMapTripPattern() {
    NetexTestDataSample sample = new NetexTestDataSample();

    TripPatternMapper tripPatternMapper = new TripPatternMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      new DefaultEntityById<>(),
      sample.getStopsById(),
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      sample.getOtpRouteByid(),
      sample.getRouteById(),
      sample.getJourneyPatternById(),
      sample.getQuayIdByStopPointRef(),
      new HierarchicalMap<>(),
      new HierarchicalMapById<>(),
      sample.getServiceJourneyById(),
      new HierarchicalMapById<>(),
      new HierarchicalMapById<>(),
      new HierarchicalMapById<>(),
      new HierarchicalMapById<>(),
      ArrayListMultimap.create(),
      Map.of(NetexTestDataSample.SERVICE_JOURNEY_ID, SERVICE_ID),
      new Deduplicator(),
      150
    );

    TripPatternMapperResult r = tripPatternMapper.mapTripPattern(sample.getJourneyPattern());

    assertEquals(1, r.tripPatterns.size());

    TripPattern tripPattern = r.tripPatterns.values().stream().findFirst().orElseThrow();

    assertEquals(4, tripPattern.numberOfStops());
    assertEquals(1, tripPattern.scheduledTripsAsStream().count());

    Trip trip = tripPattern.scheduledTripsAsStream().findFirst().get();

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
  public void testMapTripPattern_datedServiceJourney() {
    NetexTestDataSample sample = new NetexTestDataSample();

    HierarchicalMapById<DatedServiceJourney> datedServiceJourneys = new HierarchicalMapById<>();
    datedServiceJourneys.addAll(sample.getDatedServiceJourneyBySjId().values());

    TripPatternMapper tripPatternMapper = new TripPatternMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      new DefaultEntityById<>(),
      sample.getStopsById(),
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
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
    Trip trip = tripPattern.scheduledTripsAsStream().findFirst().get();

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
}
