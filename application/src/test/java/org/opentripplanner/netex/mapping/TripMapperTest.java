package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;
import static org.opentripplanner.netex.mapping.MappingSupport.createWrappedRef;

import jakarta.xml.bind.JAXBElement;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ServiceJourney;

public class TripMapperTest {

  private static final String ROUTE_ID = "RUT:Route:1";
  private static final String SERVICE_JOURNEY_ID = NetexTestDataSample.SERVICE_JOURNEY_ID;
  private static final String JOURNEY_PATTERN_ID = "RUT:JourneyPattern:1";
  private static final FeedScopedId SERVICE_ID = TimetableRepositoryForTest.id("S001");
  private static final DataImportIssueStore issueStore = DataImportIssueStore.NOOP;

  private static final JAXBElement<LineRefStructure> LINE_REF = MappingSupport.createWrappedRef(
    ROUTE_ID,
    LineRefStructure.class
  );

  @Test
  public void mapTripWithWheelchairAccess() {
    var serviceJourney = createExampleServiceJourney();
    var wheelchairLimitation = LimitationStatusEnumeration.TRUE;
    var limitation = new AccessibilityLimitation();
    var limitations = new AccessibilityLimitations_RelStructure();
    var access = new AccessibilityAssessment();

    var transitBuilder = new OtpTransitServiceBuilder(new SiteRepository(), issueStore);
    transitBuilder.getRoutes().add(TimetableRepositoryForTest.route(ROUTE_ID).build());

    TripMapper tripMapper = new TripMapper(
      ID_FACTORY,
      issueStore,
      transitBuilder.getOperatorsById(),
      transitBuilder.getRoutes(),
      new HierarchicalMapById<>(),
      new HierarchicalMap<>(),
      Map.of(SERVICE_JOURNEY_ID, SERVICE_ID)
    );

    limitation.withWheelchairAccess(wheelchairLimitation);
    limitations.withAccessibilityLimitation(limitation);
    access.withLimitations(limitations);
    serviceJourney.withAccessibilityAssessment(access);
    serviceJourney.setLineRef(LINE_REF);
    var trip = tripMapper.mapServiceJourney(serviceJourney, this::headsign);
    assertNotNull(trip, "trip must not be null");
    assertEquals(
      trip.getWheelchairBoarding(),
      Accessibility.POSSIBLE,
      "Wheelchair accessibility not possible on trip"
    );
  }

  @Test
  public void mapTrip() {
    OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder(
      new SiteRepository(),
      issueStore
    );
    transitBuilder.getRoutes().add(TimetableRepositoryForTest.route(ROUTE_ID).build());

    TripMapper tripMapper = new TripMapper(
      ID_FACTORY,
      issueStore,
      transitBuilder.getOperatorsById(),
      transitBuilder.getRoutes(),
      new HierarchicalMapById<>(),
      new HierarchicalMap<>(),
      Map.of(SERVICE_JOURNEY_ID, SERVICE_ID)
    );

    ServiceJourney serviceJourney = createExampleServiceJourney();

    serviceJourney.setLineRef(LINE_REF);

    Trip trip = tripMapper.mapServiceJourney(serviceJourney, this::headsign);

    assertEquals(trip.getId(), ID_FACTORY.createId(SERVICE_JOURNEY_ID));
  }

  @Test
  public void mapTripWithRouteRefViaJourneyPattern() {
    OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder(
      new SiteRepository(),
      issueStore
    );
    transitBuilder.getRoutes().add(TimetableRepositoryForTest.route(ROUTE_ID).build());

    JourneyPattern journeyPattern = new JourneyPattern().withId(JOURNEY_PATTERN_ID);
    journeyPattern.setRouteRef(new RouteRefStructure().withRef(ROUTE_ID));

    ServiceJourney serviceJourney = createExampleServiceJourney();
    serviceJourney.setJourneyPatternRef(
      MappingSupport.createWrappedRef(JOURNEY_PATTERN_ID, JourneyPatternRefStructure.class)
    );

    org.rutebanken.netex.model.Route netexRoute = new org.rutebanken.netex.model.Route();
    netexRoute.setLineRef(LINE_REF);
    netexRoute.setId(ROUTE_ID);

    HierarchicalMapById<org.rutebanken.netex.model.Route> routeById = new HierarchicalMapById<>();
    routeById.add(netexRoute);
    HierarchicalMapById<JourneyPattern_VersionStructure> journeyPatternById =
      new HierarchicalMapById<>();
    journeyPatternById.add(journeyPattern);

    TripMapper tripMapper = new TripMapper(
      ID_FACTORY,
      issueStore,
      transitBuilder.getOperatorsById(),
      transitBuilder.getRoutes(),
      routeById,
      journeyPatternById,
      Map.of(SERVICE_JOURNEY_ID, SERVICE_ID)
    );

    Trip trip = tripMapper.mapServiceJourney(serviceJourney, this::headsign);

    assertEquals(trip.getId(), ID_FACTORY.createId("RUT:ServiceJourney:1"));
  }

  private ServiceJourney createExampleServiceJourney() {
    ServiceJourney serviceJourney = new ServiceJourney();
    serviceJourney.setId("RUT:ServiceJourney:1");
    serviceJourney.setDayTypes(NetexTestDataSample.createEveryDayRefs());
    serviceJourney.setJourneyPatternRef(
      createWrappedRef("RUT:JourneyPattern:1", JourneyPatternRefStructure.class)
    );
    return serviceJourney;
  }

  private String headsign() {
    return "To Destination";
  }
}
