package org.opentripplanner.ext.siri;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.updater.GraphUpdaterManager;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.AffectedLineStructure;
import uk.org.siri.siri20.AffectedRouteStructure;
import uk.org.siri.siri20.AffectedStopPlaceStructure;
import uk.org.siri.siri20.AffectedStopPointStructure;
import uk.org.siri.siri20.AffectedVehicleJourneyStructure;
import uk.org.siri.siri20.AffectsScopeStructure;
import uk.org.siri.siri20.DataFrameRefStructure;
import uk.org.siri.siri20.DefaultedTextStructure;
import uk.org.siri.siri20.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri20.HalfOpenTimestampOutputRangeStructure;
import uk.org.siri.siri20.InfoLinkStructure;
import uk.org.siri.siri20.LineRef;
import uk.org.siri.siri20.PtSituationElement;
import uk.org.siri.siri20.RoutePointTypeEnumeration;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.SeverityEnumeration;
import uk.org.siri.siri20.SituationExchangeDeliveryStructure;
import uk.org.siri.siri20.SituationNumber;
import uk.org.siri.siri20.StopPointRef;
import uk.org.siri.siri20.VehicleJourneyRef;
import uk.org.siri.siri20.WorkflowStatusEnumeration;

public class SiriAlertsUpdateHandlerTest extends GtfsTest {
  private static final String FEED_ID = "FEED";

  SiriAlertsUpdateHandler alertsUpdateHandler;

  TransitAlertServiceImpl transitAlertService;

  RoutingService routingService;

  @Test
  public void testSiriSxUpdateForStop() {
    init();
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final FeedScopedId stopId = new FeedScopedId(FEED_ID, "stop0");
    List<RoutePointTypeEnumeration> stopConditions = Arrays.asList(
        RoutePointTypeEnumeration.DESTINATION,
        RoutePointTypeEnumeration.NOT_STOPPING,
        RoutePointTypeEnumeration.REQUEST_STOP,
        RoutePointTypeEnumeration.EXCEPTIONAL_STOP,
        RoutePointTypeEnumeration.START_POINT
    );

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
        ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
        createAffectsStop(stopConditions, stopId.getId())
    );

    Integer priorityValue = 3;
    ptSituation.setPriority(BigInteger.valueOf(priorityValue));

    InfoLinkStructure infoLink = new InfoLinkStructure();
    final String infoLinkUri = "http://www.test.com";
    final String infoLinkLabel = "testlabel";

    infoLink.setUri(infoLinkUri);
    infoLink.getLabels().add(createDefaultedTextStructure(infoLinkLabel));

    ptSituation.setInfoLinks(new PtSituationElement.InfoLinks());
    ptSituation.getInfoLinks().getInfoLinks().add(infoLink);

    final String reportType = "incident";
    ptSituation.setReportType(reportType);

    final SeverityEnumeration severity = SeverityEnumeration.SEVERE;
    ptSituation.setSeverity(SeverityEnumeration.SEVERE);

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final Collection<TransitAlert> stopPatches = transitAlertService.getStopAlerts(stopId);

    assertNotNull(stopPatches);
    assertEquals(1, stopPatches.size());
    final TransitAlert transitAlert = stopPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId());
    assertEquals(reportType, transitAlert.alertType);
    assertEquals(AlertSeverity.SEVERE, transitAlert.severity);
    assertEquals(priorityValue, transitAlert.priority);

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Stop.class));
    assertTrue(matchesEntity(transitAlert, stopId));

    assertTrue(transitAlert.getStopConditions().contains(StopCondition.DESTINATION));
    assertTrue(transitAlert.getStopConditions().contains(StopCondition.NOT_STOPPING));
    assertTrue(transitAlert.getStopConditions().contains(StopCondition.REQUEST_STOP));

    assertNotNull(transitAlert.getAlertUrlList());
    assertFalse(transitAlert.getAlertUrlList().isEmpty());

    final List<AlertUrl> alertUrlList = transitAlert.getAlertUrlList();
    AlertUrl alertUrl = alertUrlList.get(0);
    assertEquals(infoLinkUri, alertUrl.uri);
    assertEquals(infoLinkLabel, alertUrl.label);

  }

  public void init() {
    if (routingService == null) {
      routingService = new RoutingService(graph);
      graph.updaterManager = new GraphUpdaterManager(graph, List.of());

    }
    else {
      transitAlertService.getAllAlerts().clear();
    }
    if (alertsUpdateHandler == null) {
      alertsUpdateHandler = new SiriAlertsUpdateHandler(FEED_ID, graph);

      transitAlertService = new TransitAlertServiceImpl(graph);
      alertsUpdateHandler.setTransitAlertService(transitAlertService);

      alertsUpdateHandler.setSiriFuzzyTripMatcher(new SiriFuzzyTripMatcher(routingService));
    }
  }

  private PtSituationElement createPtSituationElement(
      String situationNumber, ZonedDateTime startTime, ZonedDateTime endTime,
      AffectsScopeStructure affects
  ) {
    PtSituationElement element = new PtSituationElement();
    element.setCreationTime(ZonedDateTime.now());
    element.setProgress(WorkflowStatusEnumeration.OPEN);
    if (startTime != null | endTime != null) {
      HalfOpenTimestampOutputRangeStructure period = new HalfOpenTimestampOutputRangeStructure();

      if (startTime != null) {
        period.setStartTime(startTime);
      }

      if (endTime != null) {
        period.setEndTime(endTime);
      }
      element.getValidityPeriods().add(period);
    }

    SituationNumber sn = new SituationNumber();
    sn.setValue(situationNumber);
    element.setSituationNumber(sn);

    element.setAffects(affects);

    element.getDescriptions().add(createDefaultedTextStructure("description"));
    element.getSummaries().add(createDefaultedTextStructure("summary"));

    return element;
  }

  private AffectsScopeStructure createAffectsStop(
      List<RoutePointTypeEnumeration> stopConditions, String... stopIds
  ) {

    AffectsScopeStructure affects = new AffectsScopeStructure();

    AffectsScopeStructure.StopPoints stopPoints = new AffectsScopeStructure.StopPoints();

    for (String stopId : stopIds) {
      StopPointRef stopPointRef = new StopPointRef();
      stopPointRef.setValue(stopId);
      AffectedStopPointStructure affectedStopPoint = new AffectedStopPointStructure();
      affectedStopPoint.setStopPointRef(stopPointRef);
      if (stopConditions != null) {
        affectedStopPoint.getStopConditions().addAll(stopConditions);
      }
      stopPoints.getAffectedStopPoints().add(affectedStopPoint);
    }

    affects.setStopPoints(stopPoints);

    return affects;
  }

  private DefaultedTextStructure createDefaultedTextStructure(String value) {
    DefaultedTextStructure textStructure = new DefaultedTextStructure();
    textStructure.setValue(value);
    return textStructure;
  }

  private ServiceDelivery createServiceDelivery(PtSituationElement situationElement) {
    return createServiceDelivery(Arrays.asList(situationElement));
  }

  private boolean containsOnlyEntitiesOfClass(TransitAlert transitAlert, Class cls) {
    long totalEntityCount = transitAlert.getEntities().size();
    long clsEntityCount = transitAlert.getEntities()
            .stream()
            .filter(entitySelector -> cls.isInstance(entitySelector))
            .count();
    return clsEntityCount > 0 && clsEntityCount == totalEntityCount;
  }

  private boolean matchesEntity(TransitAlert transitAlert, FeedScopedId feedScopedEntityId) {
    boolean foundMatch = false;
    for (EntitySelector entity : transitAlert.getEntities()) {
      if (!foundMatch) {
        if (entity instanceof EntitySelector.Stop) {
          foundMatch = ((EntitySelector.Stop) entity).stopId.equals(feedScopedEntityId);
        }
        else if (entity instanceof EntitySelector.Trip) {
          foundMatch = ((EntitySelector.Trip) entity).tripId.equals(feedScopedEntityId);
        }
        else if (entity instanceof EntitySelector.Route) {
          foundMatch = ((EntitySelector.Route) entity).routeId.equals(feedScopedEntityId);
        }
      }
    }
    return foundMatch;
  }

  private ServiceDelivery createServiceDelivery(List<PtSituationElement> situationElement) {
    ServiceDelivery delivery = new ServiceDelivery();
    SituationExchangeDeliveryStructure sxDeliveries = new SituationExchangeDeliveryStructure();
    SituationExchangeDeliveryStructure.Situations situations = new SituationExchangeDeliveryStructure.Situations();
    situations.getPtSituationElements().addAll(situationElement);
    sxDeliveries.setSituations(situations);
    delivery.getSituationExchangeDeliveries().add(sxDeliveries);

    return delivery;
  }

  @Test
  public void testSiriSxUpdateForStopMultipleValidityPeriods() {
    init();
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";

    final FeedScopedId stopId = new FeedScopedId(FEED_ID, "stop0");

    List<RoutePointTypeEnumeration> stopConditions = Arrays.asList(
        RoutePointTypeEnumeration.DESTINATION,
        RoutePointTypeEnumeration.NOT_STOPPING,
        RoutePointTypeEnumeration.REQUEST_STOP
    );

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        null,
        null,
        createAffectsStop(stopConditions, stopId.getId())
    );

    final ZonedDateTime startTimePeriod_1 = ZonedDateTime.parse("2014-01-01T10:00:00+01:00");
    final ZonedDateTime endTimePeriod_1 = ZonedDateTime.parse("2014-01-01T11:00:00+01:00");
    final ZonedDateTime startTimePeriod_2 = ZonedDateTime.parse("2014-01-02T10:00:00+01:00");
    final ZonedDateTime endTimePeriod_2 = ZonedDateTime.parse("2014-01-02T11:00:00+01:00");

    HalfOpenTimestampOutputRangeStructure period_1 = new HalfOpenTimestampOutputRangeStructure();
    period_1.setStartTime(startTimePeriod_1);
    period_1.setEndTime(endTimePeriod_1);
    ptSituation.getValidityPeriods().add(period_1);

    HalfOpenTimestampOutputRangeStructure period_2 = new HalfOpenTimestampOutputRangeStructure();
    period_2.setStartTime(startTimePeriod_2);
    period_2.setEndTime(endTimePeriod_2);
    ptSituation.getValidityPeriods().add(period_2);

    final String reportType = "incident";
    ptSituation.setReportType(reportType);

    final SeverityEnumeration severity = SeverityEnumeration.SEVERE;
    ptSituation.setSeverity(SeverityEnumeration.SEVERE);

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final Collection<TransitAlert> stopPatches = transitAlertService.getStopAlerts(stopId);

    assertNotNull(stopPatches);
    assertEquals(1, stopPatches.size());
    final TransitAlert transitAlert = stopPatches.iterator().next();

    assertTrue(matchesEntity(transitAlert, stopId));

    assertValidity("period 1", transitAlert, startTimePeriod_1, endTimePeriod_1);

    assertValidity("period 2", transitAlert, startTimePeriod_2, endTimePeriod_2);

  }

  private void assertValidity(
      String label, TransitAlert transitAlert, ZonedDateTime startTimePeriod_1,
      ZonedDateTime endTimePeriod_1
  ) {
    // TimePeriod ends BEFORE first validityPeriod starts
    assertFalse(
        "TimePeriod ends BEFORE first validityPeriod starts: " + label,
        transitAlert.displayDuring(
            startTimePeriod_1.toEpochSecond() - 200,
            startTimePeriod_1.toEpochSecond() - 100
        )
    );

    // TimePeriod ends AFTER first validityPeriod starts, BEFORE it ends
    assertTrue(
        "TimePeriod ends AFTER first validityPeriod starts, BEFORE it ends: " + label,
        transitAlert.displayDuring(
            startTimePeriod_1.toEpochSecond() - 1000,
            endTimePeriod_1.toEpochSecond() - 100
        )
    );

    // TimePeriod starts AFTER first validityPeriod starts, BEFORE it ends
    assertTrue(
        "TimePeriod starts AFTER first validityPeriod starts, BEFORE it ends: " + label,
        transitAlert.displayDuring(
            startTimePeriod_1.toEpochSecond() + 100,
            endTimePeriod_1.toEpochSecond() - 100
        )
    );

    // TimePeriod starts AFTER first validityPeriod starts, ends AFTER it ends
    assertTrue(
        "TimePeriod starts AFTER first validityPeriod starts, ends AFTER it ends: " + label,
        transitAlert.displayDuring(
            startTimePeriod_1.toEpochSecond() + 100,
            endTimePeriod_1.toEpochSecond() + 100
        )
    );

    // TimePeriod starts AFTER first validityPeriod ends
    assertFalse(
        "TimePeriod starts AFTER first validityPeriod ends: " + label,
        transitAlert.displayDuring(
            endTimePeriod_1.toEpochSecond() + 100,
            endTimePeriod_1.toEpochSecond() + 200
        )
    );
  }

  @Test
  public void testSiriSxUpdateForMultipleStops() {
    init();
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";

    final FeedScopedId stopId0 = new FeedScopedId(FEED_ID, "stop0");
    final FeedScopedId stopId1 = new FeedScopedId(FEED_ID, "stop1");

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
        ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
        createAffectsStop(null, stopId0.getId(), stopId1.getId())
    );

    final String reportType = "incident";
    ptSituation.setReportType(reportType);

    final SeverityEnumeration severity = SeverityEnumeration.SEVERE;
    ptSituation.setSeverity(SeverityEnumeration.SEVERE);

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    Collection<TransitAlert> stopPatches = transitAlertService.getStopAlerts(stopId0);

    assertNotNull(stopPatches);
    assertEquals(1, stopPatches.size());
    TransitAlert transitAlert = stopPatches.iterator().next();
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Stop.class));
    assertTrue(matchesEntity(transitAlert, stopId0));

    assertTrue(
        "Alert does not contain default condition START_POINT",
        transitAlert.getStopConditions().contains(StopCondition.START_POINT)
    );
    assertTrue(
        "Alert does not contain default condition DESTINATION",
        transitAlert.getStopConditions().contains(StopCondition.DESTINATION)
    );

    stopPatches = transitAlertService.getStopAlerts(stopId1);

    assertNotNull(stopPatches);
    assertEquals(1, stopPatches.size());
    transitAlert = stopPatches.iterator().next();

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Stop.class));
    assertTrue(matchesEntity(transitAlert, stopId1));
    ;

    assertTrue(
        "Alert does not contain default condition START_POINT",
        transitAlert.getStopConditions().contains(StopCondition.START_POINT)
    );
    assertTrue(
        "Alert does not contain default condition DESTINATION",
        transitAlert.getStopConditions().contains(StopCondition.DESTINATION)
    );
  }

  @Test
  public void testSiriSxUpdateForTrip() {
    init();
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        startTime,
        endTime,
        createAffectsFramedVehicleJourney(tripId.getId(), "2014-01-01", null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation));

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    ServiceDate serviceDate = new ServiceDate(2014, 1, 1);
    final Collection<TransitAlert> tripPatches = transitAlertService.getTripAlerts(tripId, serviceDate);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Trip.class));
    assertTrue(matchesEntity(transitAlert, tripId));

    assertEquals(situationNumber, transitAlert.getId());

    // Effective validity should be calculated based on the actual departures when Operating dat/service date is provided
    final ZonedDateTime effectiveStartDate = ZonedDateTime.ofInstant(transitAlert
        .getEffectiveStartDate()
        .toInstant(), startTime.getZone());
    final ZonedDateTime effectiveEndDate = ZonedDateTime.ofInstant(transitAlert
        .getEffectiveEndDate()
        .toInstant(), endTime.getZone());

    assertEquals(effectiveStartDate, startTime);
    assertEquals(effectiveEndDate, endTime);

  }

  @Test
  public void testSiriSxUpdateForTripWithoutSpecificDate() {
    init();
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        startTime,
        endTime,
        createAffectsFramedVehicleJourney(tripId.getId(), null, null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation));

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    // Verify that requesting specific date does not include alert for all dates
    ServiceDate serviceDate = new ServiceDate(2014, 1, 1);
    Collection<TransitAlert> tripPatches = transitAlertService.getTripAlerts(tripId, serviceDate);

    assertNotNull(tripPatches);
    assertEquals(0, tripPatches.size());

    // Verify that NOT requesting specific date includes alert for all dates
    serviceDate = null;
    tripPatches = transitAlertService.getTripAlerts(tripId, serviceDate);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());

    final TransitAlert transitAlert = tripPatches.iterator().next();

    final TransitAlert datedTransitAlert = tripPatches.iterator().next();

    assertEquals(transitAlert, datedTransitAlert);

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Trip.class));
    assertTrue(matchesEntity(transitAlert, tripId));

    assertEquals(situationNumber, transitAlert.getId());

    // Effective validity should be calculated based on the actual departures when Operating dat/service date is provided
    final ZonedDateTime effectiveStartDate = ZonedDateTime.ofInstant(transitAlert
        .getEffectiveStartDate()
        .toInstant(), startTime.getZone());
    final ZonedDateTime effectiveEndDate = ZonedDateTime.ofInstant(transitAlert
        .getEffectiveEndDate()
        .toInstant(), endTime.getZone());

    assertEquals(effectiveStartDate, startTime);
    assertEquals(effectiveEndDate, endTime);

  }

  private AffectsScopeStructure createAffectsFramedVehicleJourney(
      String datedVehicleJourney, String dataFrameValue, String... stopIds
  ) {
    AffectsScopeStructure affects = new AffectsScopeStructure();
    AffectsScopeStructure.VehicleJourneys vehicleJourneys = new AffectsScopeStructure.VehicleJourneys();
    AffectedVehicleJourneyStructure affectedVehicleJourney = new AffectedVehicleJourneyStructure();
    FramedVehicleJourneyRefStructure framedVehicleJourneyRef = new FramedVehicleJourneyRefStructure();
    framedVehicleJourneyRef.setDatedVehicleJourneyRef(datedVehicleJourney);
    if (dataFrameValue != null) {
      DataFrameRefStructure dataFrameRef = new DataFrameRefStructure();
      dataFrameRef.setValue(dataFrameValue);
      framedVehicleJourneyRef.setDataFrameRef(dataFrameRef);
    }
    affectedVehicleJourney.setFramedVehicleJourneyRef(framedVehicleJourneyRef);

    if (stopIds != null) {
      AffectedRouteStructure affectedRoute = new AffectedRouteStructure();
      AffectedRouteStructure.StopPoints stopPoints = createAffectedStopPoints(stopIds);
      affectedRoute.setStopPoints(stopPoints);
      affectedVehicleJourney.getRoutes().add(affectedRoute);
    }

    vehicleJourneys.getAffectedVehicleJourneies().add(affectedVehicleJourney);
    affects.setVehicleJourneys(vehicleJourneys);

    return affects;
  }

  private AffectedRouteStructure.StopPoints createAffectedStopPoints(String... stopIds) {
    AffectedRouteStructure.StopPoints stopPoints = new AffectedRouteStructure.StopPoints();
    for (String stopId : stopIds) {
      AffectedStopPointStructure affectedStopPoint = new AffectedStopPointStructure();
      StopPointRef stopPointRef = new StopPointRef();
      stopPointRef.setValue(stopId);
      affectedStopPoint.setStopPointRef(stopPointRef);
      stopPoints.getAffectedStopPointsAndLinkProjectionToNextStopPoints().add(affectedStopPoint);

    }
    return stopPoints;
  }

  @Test
  public void testSiriSxUpdateForTripByVehicleJourney() {
    init();
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        startTime,
        endTime,
        createAffectsVehicleJourney(tripId.getId(), startTime, null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation));

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    ServiceDate serviceDate = new ServiceDate(2014, 1, 1);
    final Collection<TransitAlert> tripPatches = transitAlertService.getTripAlerts(tripId, serviceDate);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Trip.class));
    assertTrue(matchesEntity(transitAlert, tripId));
  }

  private AffectsScopeStructure createAffectsVehicleJourney(
      String vehicleJourneyRef, ZonedDateTime originAimedDepartureTime, String... stopIds
  ) {
    AffectsScopeStructure affects = new AffectsScopeStructure();
    AffectsScopeStructure.VehicleJourneys vehicleJourneys = new AffectsScopeStructure.VehicleJourneys();
    AffectedVehicleJourneyStructure affectedVehicleJourney = new AffectedVehicleJourneyStructure();

    VehicleJourneyRef vehicleJourney = new VehicleJourneyRef();
    vehicleJourney.setValue(vehicleJourneyRef);
    affectedVehicleJourney.getVehicleJourneyReves().add(vehicleJourney);
    affectedVehicleJourney.setOriginAimedDepartureTime(originAimedDepartureTime);

    if (stopIds != null) {
      AffectedRouteStructure affectedRoute = new AffectedRouteStructure();
      AffectedRouteStructure.StopPoints stopPoints = createAffectedStopPoints(stopIds);
      affectedRoute.setStopPoints(stopPoints);
      affectedVehicleJourney.getRoutes().add(affectedRoute);
    }

    vehicleJourneys.getAffectedVehicleJourneies().add(affectedVehicleJourney);
    affects.setVehicleJourneys(vehicleJourneys);

    return affects;
  }

  @Test
  public void testSiriSxUpdateForTripAndStopByVehicleJourney() {
    init();

    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");
    final FeedScopedId stopId0 = new FeedScopedId(FEED_ID, "stop0");
    final FeedScopedId stopId1 = new FeedScopedId(FEED_ID, "stop1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        startTime,
        endTime,
        createAffectsVehicleJourney(tripId.getId(), startTime, stopId0.getId(), stopId1.getId())
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation));

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final ServiceDate serviceDate = new ServiceDate(2014, 1, 1);

    Collection<TransitAlert> tripPatches = transitAlertService.getStopAndTripAlerts(
        stopId0,
        tripId,
        serviceDate
    );

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId());
    assertTrue(matchesEntity(transitAlert, stopId0, tripId, serviceDate));

    tripPatches = transitAlertService.getStopAndTripAlerts(stopId1, tripId, serviceDate);
    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndTrip.class));
    assertTrue(matchesEntity(transitAlert, stopId1, tripId, serviceDate));

  }

  private boolean matchesEntity(
      TransitAlert transitAlert, FeedScopedId stopId, FeedScopedId routeOrTripId
  ) {
    return matchesEntity(transitAlert, stopId, routeOrTripId, null);
  }
  private boolean matchesEntity(
      TransitAlert transitAlert, FeedScopedId stopId, FeedScopedId routeOrTripId, ServiceDate serviceDate
  ) {
    boolean foundMatch = false;
    for (EntitySelector entity : transitAlert.getEntities()) {
      if (!foundMatch) {
        if (entity instanceof EntitySelector.StopAndRoute) {
          foundMatch = ((EntitySelector.StopAndRoute) entity).stopAndRoute.equals((
              new EntitySelector.StopAndRouteOrTripKey(
                  stopId,
                  routeOrTripId,
                  serviceDate
              )
          ));
        }
        if (entity instanceof EntitySelector.StopAndTrip) {
          foundMatch = ((EntitySelector.StopAndTrip) entity).stopAndTrip.equals((
              new EntitySelector.StopAndRouteOrTripKey(
                  stopId,
                  routeOrTripId,
                  serviceDate
              )
          ));
        }
      }
    }
    return foundMatch;
  }

  @Test
  public void testSiriSxUpdateForLine() {
    init();
    final FeedScopedId lineRef = new FeedScopedId(FEED_ID, "route0");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        startTime,
        endTime,
        createAffectsLine(lineRef.getId(), null)
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final Collection<TransitAlert> tripPatches = transitAlertService.getRouteAlerts(lineRef);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Route.class));
    assertTrue(matchesEntity(transitAlert, lineRef));
    assertEquals(situationNumber, transitAlert.getId());

    // Effective validity should be calculated based on the actual departures when Operating dat/service date is provided
    final ZonedDateTime effectiveStartDate = ZonedDateTime.ofInstant(transitAlert
        .getEffectiveStartDate()
        .toInstant(), startTime.getZone());
    final ZonedDateTime effectiveEndDate = ZonedDateTime.ofInstant(transitAlert
        .getEffectiveEndDate()
        .toInstant(), endTime.getZone());

    assertEquals(startTime, effectiveStartDate);
    assertEquals(endTime, effectiveEndDate);

  }

  private AffectsScopeStructure createAffectsLine(String line, String... stopIds) {

    AffectsScopeStructure affects = new AffectsScopeStructure();

    AffectsScopeStructure.Networks networks = new AffectsScopeStructure.Networks();
    AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork = new AffectsScopeStructure.Networks.AffectedNetwork();
    AffectedLineStructure affectedLine = new AffectedLineStructure();
    LineRef lineRef = new LineRef();
    lineRef.setValue(line);
    affectedLine.setLineRef(lineRef);
    affectedNetwork.getAffectedLines().add(affectedLine);

    if (stopIds != null) {
      AffectedLineStructure.Routes routes = new AffectedLineStructure.Routes();
      AffectedRouteStructure affectedRoute = new AffectedRouteStructure();

      AffectedRouteStructure.StopPoints stopPoints = createAffectedStopPoints(stopIds);

      affectedRoute.setStopPoints(stopPoints);
      routes.getAffectedRoutes().add(affectedRoute);
      affectedLine.setRoutes(routes);
    }

    networks.getAffectedNetworks().add(affectedNetwork);
    affects.setNetworks(networks);

    return affects;
  }

  @Test
  public void testSiriSxUpdateForLineThenExpiry() {
    init();
    final FeedScopedId lineRef = new FeedScopedId(FEED_ID, "route0");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        startTime,
        endTime,
        createAffectsLine(lineRef.getId(), null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation));

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    Collection<TransitAlert> tripPatches = transitAlertService.getRouteAlerts(lineRef);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Route.class));
    assertTrue(matchesEntity(transitAlert, lineRef));
    assertEquals(situationNumber, transitAlert.getId());

    ptSituation = createPtSituationElement(situationNumber,
        startTime,
        endTime,
        createAffectsLine(lineRef.getId(), null)
    );

    ptSituation.setProgress(WorkflowStatusEnumeration.CLOSED);

    alertsUpdateHandler.update(createServiceDelivery(ptSituation));

    tripPatches = transitAlertService.getRouteAlerts(lineRef);

    assertNotNull(tripPatches);
    assertTrue(tripPatches.isEmpty());
  }

  @Test
  public void testSiriSxUpdateForTripAndStop() {
    init();
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final FeedScopedId stopId0 = new FeedScopedId(FEED_ID, "stop0");
    final FeedScopedId stopId1 = new FeedScopedId(FEED_ID, "stop1");

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
        ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
        createAffectsFramedVehicleJourney(tripId.getId(),
            "2014-01-01",
            stopId0.getId(),
            stopId1.getId()
        )
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    /*
     * Trip and stop-alerts should result in several TransitAlertes. One for each tripId/stop combination
     */

    final ServiceDate serviceDate = new ServiceDate(2014, 1, 1);
    Collection<TransitAlert> tripPatches = transitAlertService.getStopAndTripAlerts(
        stopId0,
        tripId,
        serviceDate
    );

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndTrip.class));
    assertTrue(matchesEntity(transitAlert, stopId0, tripId, serviceDate));

    tripPatches = transitAlertService.getStopAndTripAlerts(stopId1, tripId, serviceDate);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndTrip.class));
    assertTrue(matchesEntity(transitAlert, stopId1, tripId, serviceDate));

  }

  @Test
  public void testSiriSxUpdateForLineAndStop() {
    init();
    final String routeId = "route0";

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final String stopId0 = "stop0";
    final String stopId1 = "stop1";
    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
        ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
        createAffectsLine(routeId, stopId0, stopId1)
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    /*
     * Line and stop-alerts should result in several TransitAlertes. One for each routeId/stop combination
     */

    assertLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);
  }

  private void assertLineAndStopAlerts(
      String situationNumber, String routeId, String stopId0, String stopId1
  ) {
    /*
     * Line and stop-alerts should result in several TransitAlertes. One for each routeId/stop combination
     */

    final FeedScopedId feedRouteId = new FeedScopedId(FEED_ID, routeId);
    final FeedScopedId feedStop_0_id = new FeedScopedId(FEED_ID, stopId0);
    Collection<TransitAlert> tripPatches = transitAlertService.getStopAndRouteAlerts(
        feedStop_0_id,
        feedRouteId
    );

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId());

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndRoute.class));
    assertTrue(matchesEntity(transitAlert, feedStop_0_id, feedRouteId));

    final FeedScopedId feedStop_1_id = new FeedScopedId(FEED_ID, stopId1);
    tripPatches = transitAlertService.getStopAndRouteAlerts(feedStop_1_id, feedRouteId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId());

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndRoute.class));
    assertTrue(matchesEntity(transitAlert, feedStop_1_id, feedRouteId));

  }

  @Test
  public void testSiriSxUpdateForLineAndExternallyDefinedStopPoint() {
    init();
    final String routeId = "route0";

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final String stopId0 = "stop0";
    final String stopId1 = "stop1";
    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
        ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
        createAffectsLineWithExternallyDefinedStopPoints(routeId, stopId0, stopId1)
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    assertSeparateLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);
  }



  @Test
  public void testSiriSxWithOpenEndedValidity() {
    init();

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final String stopId0 = "stop0";

    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        null,
        null,
        createAffectsStop(new ArrayList<>(), stopId0)
    );

    // Add period with start- and endtime
    HalfOpenTimestampOutputRangeStructure period_1 = new HalfOpenTimestampOutputRangeStructure();
    period_1.setStartTime(ZonedDateTime.parse("2020-01-01T10:00:00+01:00"));
    period_1.setEndTime(ZonedDateTime.parse("2020-02-01T11:00:00+01:00"));
    ptSituation.getValidityPeriods().add(period_1);


    // Add period with start-, but NO endtime - i.e. open-ended
    HalfOpenTimestampOutputRangeStructure period_2 = new HalfOpenTimestampOutputRangeStructure();
    period_2.setStartTime(ZonedDateTime.parse("2020-02-02T10:00:00+01:00"));
    period_2.setEndTime(null);
    ptSituation.getValidityPeriods().add(period_2);

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final FeedScopedId stopId = new FeedScopedId("FEED", stopId0);

    Collection<TransitAlert> tripPatches = transitAlertService.getStopAlerts(stopId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId());

    assertNotNull(transitAlert.getEffectiveStartDate());

    assertEquals(period_1.getStartTime().toEpochSecond(), (transitAlert.getEffectiveStartDate().getTime()/1000));

    assertNull(transitAlert.getEffectiveEndDate());
  }

  private AffectsScopeStructure createAffectsLineWithExternallyDefinedStopPoints(
      String line, String... stopIds
  ) {

    AffectsScopeStructure affects = new AffectsScopeStructure();

    AffectsScopeStructure.Networks networks = new AffectsScopeStructure.Networks();
    AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork = new AffectsScopeStructure.Networks.AffectedNetwork();
    AffectedLineStructure affectedLine = new AffectedLineStructure();
    LineRef lineRef = new LineRef();
    lineRef.setValue(line);
    affectedLine.setLineRef(lineRef);
    affectedNetwork.getAffectedLines().add(affectedLine);

    networks.getAffectedNetworks().add(affectedNetwork);
    affects.setNetworks(networks);
    if (stopIds != null) {
      AffectsScopeStructure.StopPoints stopPoints = new AffectsScopeStructure.StopPoints();
      for (String stopId : stopIds) {
        AffectedStopPointStructure affectedStopPoint = new AffectedStopPointStructure();
        StopPointRef stopPointRef = new StopPointRef();
        stopPointRef.setValue(stopId);
        affectedStopPoint.setStopPointRef(stopPointRef);
        stopPoints.getAffectedStopPoints().add(affectedStopPoint);
      }
      affects.setStopPoints(stopPoints);

    }

    return affects;
  }

  private void assertSeparateLineAndStopAlerts(
      String situationNumber, String routeId, String stopId0, String stopId1
  ) {
    /*
     * Line and external stop-alerts should result in several AlertPatches. One for each routeId AND for each stop
     */

    final FeedScopedId feedRouteId = new FeedScopedId("FEED", routeId);
    Collection<TransitAlert> tripPatches = transitAlertService.getRouteAlerts(feedRouteId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId());
    assertTrue(matchesEntity(transitAlert, feedRouteId));

    FeedScopedId feedStopId = new FeedScopedId("FEED", stopId0);
    tripPatches = transitAlertService.getStopAlerts(feedStopId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId());
    assertTrue(matchesEntity(transitAlert, feedStopId));

    feedStopId = new FeedScopedId("FEED", stopId1);
    tripPatches = transitAlertService.getStopAlerts(feedStopId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId());
    assertTrue(matchesEntity(transitAlert, feedStopId));
  }

  @Test
  public void testSiriSxUpdateForLineAndExternallyDefinedStopPlace() {
    init();
    final String routeId = "route0";

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final String stopId0 = "stop0";
    final String stopId1 = "stop1";
    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
        ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
        ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
        createAffectsLineWithExternallyDefinedStopPlaces(routeId, stopId0, stopId1)
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());
    assertSeparateLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);

  }

  @Test
  public void testSiriSxUpdateForUnknownEntity() {
    init();
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    PtSituationElement ptSituation = createPtSituationElement(situationNumber,
            ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
            ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
            null
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery);

    Collection<TransitAlert> alerts = transitAlertService.getAllAlerts();
    assertEquals(1, alerts.size());
    TransitAlert transitAlert = alerts.iterator().next();
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Unknown.class));
  }

  private AffectsScopeStructure createAffectsLineWithExternallyDefinedStopPlaces(
      String line, String... stopIds
  ) {

    AffectsScopeStructure affects = new AffectsScopeStructure();

    AffectsScopeStructure.Networks networks = new AffectsScopeStructure.Networks();
    AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork = new AffectsScopeStructure.Networks.AffectedNetwork();
    AffectedLineStructure affectedLine = new AffectedLineStructure();
    LineRef lineRef = new LineRef();
    lineRef.setValue(line);
    affectedLine.setLineRef(lineRef);
    affectedNetwork.getAffectedLines().add(affectedLine);

    networks.getAffectedNetworks().add(affectedNetwork);
    affects.setNetworks(networks);
    if (stopIds != null) {
      AffectsScopeStructure.StopPlaces stopPlaces = new AffectsScopeStructure.StopPlaces();
      for (String stopId : stopIds) {
        AffectedStopPlaceStructure affectedStopPlaceStructure = new AffectedStopPlaceStructure();
        StopPlaceRef stopPlaceRef = new StopPlaceRef();
        stopPlaceRef.setValue(stopId);
        affectedStopPlaceStructure.setStopPlaceRef(stopPlaceRef);
        stopPlaces.getAffectedStopPlaces().add(affectedStopPlaceStructure);
      }
      affects.setStopPlaces(stopPlaces);

    }

    return affects;
  }

  @Override
  public String getFeedName() {
    return "gtfs/interlining";
  }
}

