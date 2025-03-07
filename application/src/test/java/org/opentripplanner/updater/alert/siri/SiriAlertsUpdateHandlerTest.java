package org.opentripplanner.updater.alert.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.StopConditionsHelper;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.RealTimeUpdateContext;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.AffectedLineStructure;
import uk.org.siri.siri20.AffectedRouteStructure;
import uk.org.siri.siri20.AffectedStopPlaceStructure;
import uk.org.siri.siri20.AffectedStopPointStructure;
import uk.org.siri.siri20.AffectedVehicleJourneyStructure;
import uk.org.siri.siri20.AffectsScopeStructure;
import uk.org.siri.siri20.DataFrameRefStructure;
import uk.org.siri.siri20.DatedVehicleJourneyRef;
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

  TransitService transitService;

  private RealTimeUpdateContext realTimeUpdateContext;

  @Override
  public String getFeedName() {
    return "gtfs/interlining";
  }

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();

    realTimeUpdateContext = new DefaultRealTimeUpdateContext(graph, timetableRepository);
    if (transitService == null) {
      transitService = new DefaultTransitService(timetableRepository);
      timetableRepository.setUpdaterManager(
        new GraphUpdaterManager(realTimeUpdateContext, List.of())
      );
    } else {
      transitAlertService.getAllAlerts().clear();
    }
    if (alertsUpdateHandler == null) {
      transitAlertService = new TransitAlertServiceImpl(timetableRepository);
      alertsUpdateHandler = new SiriAlertsUpdateHandler(
        FEED_ID,
        transitAlertService,
        Duration.ZERO
      );
    }
  }

  @Test
  public void testSiriSxUpdateForStop() {
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final FeedScopedId stopId = new FeedScopedId(FEED_ID, "stop0");
    List<RoutePointTypeEnumeration> stopConditions = Arrays.asList(
      RoutePointTypeEnumeration.DESTINATION,
      RoutePointTypeEnumeration.NOT_STOPPING,
      RoutePointTypeEnumeration.REQUEST_STOP
    );

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
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
    ptSituation.setSeverity(SeverityEnumeration.SEVERE);

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final Collection<TransitAlert> stopPatches = transitAlertService.getStopAlerts(stopId);

    assertNotNull(stopPatches);
    assertEquals(1, stopPatches.size());
    final TransitAlert transitAlert = stopPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId().getId());
    assertEquals(reportType, transitAlert.type());
    assertEquals(AlertSeverity.SEVERE, transitAlert.severity());
    assertEquals(priorityValue, transitAlert.priority());

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Stop.class));
    assertTrue(matchesEntity(transitAlert, stopId));

    assertEquals(1, transitAlert.entities().size());
    EntitySelector entitySelector = transitAlert.entities().iterator().next();
    assertTrue(
      ((EntitySelector.Stop) entitySelector).stopConditions().contains(StopCondition.DESTINATION)
    );
    assertTrue(
      StopConditionsHelper.matchesStopCondition(entitySelector, Set.of(StopCondition.DESTINATION))
    );

    assertTrue(
      ((EntitySelector.Stop) entitySelector).stopConditions().contains(StopCondition.NOT_STOPPING)
    );
    assertTrue(
      StopConditionsHelper.matchesStopCondition(entitySelector, Set.of(StopCondition.NOT_STOPPING))
    );

    assertTrue(
      ((EntitySelector.Stop) entitySelector).stopConditions().contains(StopCondition.REQUEST_STOP)
    );
    assertTrue(
      StopConditionsHelper.matchesStopCondition(entitySelector, Set.of(StopCondition.REQUEST_STOP))
    );

    // The following StopConditions are not added to the EntitySelector, and should not match
    assertFalse(
      ((EntitySelector.Stop) entitySelector).stopConditions().contains(StopCondition.START_POINT)
    );
    assertFalse(
      StopConditionsHelper.matchesStopCondition(entitySelector, Set.of(StopCondition.START_POINT))
    );

    assertFalse(
      ((EntitySelector.Stop) entitySelector).stopConditions()
        .contains(StopCondition.EXCEPTIONAL_STOP)
    );
    assertFalse(
      StopConditionsHelper.matchesStopCondition(
        entitySelector,
        Set.of(StopCondition.EXCEPTIONAL_STOP)
      )
    );

    assertNotNull(transitAlert.siriUrls());
    assertFalse(transitAlert.siriUrls().isEmpty());

    final List<AlertUrl> alertUrlList = transitAlert.siriUrls();
    AlertUrl alertUrl = alertUrlList.get(0);
    assertEquals(infoLinkUri, alertUrl.uri());
    assertEquals(infoLinkLabel, alertUrl.label());
  }

  @Test
  public void testSiriSxUpdateForStopMultipleValidityPeriods() {
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";

    final FeedScopedId stopId = new FeedScopedId(FEED_ID, "stop0");

    List<RoutePointTypeEnumeration> stopConditions = Arrays.asList(
      RoutePointTypeEnumeration.DESTINATION,
      RoutePointTypeEnumeration.NOT_STOPPING,
      RoutePointTypeEnumeration.REQUEST_STOP
    );

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
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
    ptSituation.setSeverity(SeverityEnumeration.SEVERE);

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final Collection<TransitAlert> stopPatches = transitAlertService.getStopAlerts(stopId);

    assertNotNull(stopPatches);
    assertEquals(1, stopPatches.size());
    final TransitAlert transitAlert = stopPatches.iterator().next();

    assertTrue(matchesEntity(transitAlert, stopId));

    assertValidity("period 1", transitAlert, startTimePeriod_1, endTimePeriod_1);

    assertValidity("period 2", transitAlert, startTimePeriod_2, endTimePeriod_2);
  }

  @Test
  public void testSiriSxUpdateForMultipleStops() {
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";

    final FeedScopedId stopId0 = new FeedScopedId(FEED_ID, "stop0");
    final FeedScopedId stopId1 = new FeedScopedId(FEED_ID, "stop1");

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
      ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
      createAffectsStop(null, stopId0.getId(), stopId1.getId())
    );

    final String reportType = "incident";
    ptSituation.setReportType(reportType);

    final SeverityEnumeration severity = SeverityEnumeration.SEVERE;
    ptSituation.setSeverity(severity);

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    Collection<TransitAlert> stopPatches = transitAlertService.getStopAlerts(stopId0);

    assertNotNull(stopPatches);
    assertEquals(1, stopPatches.size());
    TransitAlert transitAlert = stopPatches.iterator().next();
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Stop.class));
    assertTrue(matchesEntity(transitAlert, stopId0));

    boolean foundStartPointStopCondition = false;
    boolean foundDestinationStopCondition = false;
    for (EntitySelector entity : transitAlert.entities()) {
      if (((EntitySelector.Stop) entity).stopConditions().contains(StopCondition.START_POINT)) {
        foundStartPointStopCondition = true;
      }
      if (((EntitySelector.Stop) entity).stopConditions().contains(StopCondition.DESTINATION)) {
        foundDestinationStopCondition = true;
      }
    }
    assertTrue(
      foundStartPointStopCondition,
      "Alert does not contain default condition START_POINT"
    );
    assertTrue(
      foundDestinationStopCondition,
      "Alert does not contain default condition DESTINATION"
    );

    stopPatches = transitAlertService.getStopAlerts(stopId1);

    assertNotNull(stopPatches);
    assertEquals(1, stopPatches.size());
    transitAlert = stopPatches.iterator().next();

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Stop.class));
    assertTrue(matchesEntity(transitAlert, stopId1));

    foundStartPointStopCondition = false;
    foundDestinationStopCondition = false;
    for (EntitySelector entity : transitAlert.entities()) {
      if (((EntitySelector.Stop) entity).stopConditions().contains(StopCondition.START_POINT)) {
        foundStartPointStopCondition = true;
      }
      if (((EntitySelector.Stop) entity).stopConditions().contains(StopCondition.DESTINATION)) {
        foundDestinationStopCondition = true;
      }
    }

    assertTrue(
      foundStartPointStopCondition,
      "Alert does not contain default condition START_POINT"
    );
    assertTrue(
      foundDestinationStopCondition,
      "Alert does not contain default condition DESTINATION"
    );
  }

  @Test
  public void testSiriSxUpdateForTrip() {
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      startTime,
      endTime,
      createAffectsFramedVehicleJourney(tripId.getId(), "2014-01-01", null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation), realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    LocalDate serviceDate = LocalDate.of(2014, 1, 1);
    final Collection<TransitAlert> tripPatches = transitAlertService.getTripAlerts(
      tripId,
      serviceDate
    );

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Trip.class));
    assertTrue(matchesEntity(transitAlert, tripId));

    assertEquals(situationNumber, transitAlert.getId().getId());

    // Effective validity should be calculated based on the actual departures when Operating dat/service date is provided
    final ZonedDateTime effectiveStartDate = ZonedDateTime.ofInstant(
      transitAlert.getEffectiveStartDate(),
      startTime.getZone()
    );
    final ZonedDateTime effectiveEndDate = ZonedDateTime.ofInstant(
      transitAlert.getEffectiveEndDate(),
      endTime.getZone()
    );

    assertEquals(effectiveStartDate, startTime);
    assertEquals(effectiveEndDate, endTime);
  }

  @Test
  public void testSiriSxUpdateForTripWithoutSpecificDate() {
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      startTime,
      endTime,
      createAffectsFramedVehicleJourney(tripId.getId(), null, null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation), realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    // Verify that requesting specific date does include alert for all dates
    LocalDate serviceDate = LocalDate.of(2014, 1, 1);
    Collection<TransitAlert> tripPatches = transitAlertService.getTripAlerts(tripId, serviceDate);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert datedTransitAlert = tripPatches.iterator().next();

    // Verify that NOT requesting specific date includes alert for all dates
    serviceDate = null;
    tripPatches = transitAlertService.getTripAlerts(tripId, serviceDate);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());

    final TransitAlert transitAlert = tripPatches.iterator().next();

    assertEquals(transitAlert, datedTransitAlert);

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Trip.class));
    assertTrue(matchesEntity(transitAlert, tripId));

    assertEquals(situationNumber, transitAlert.getId().getId());

    // Effective validity should be calculated based on the actual departures when Operating dat/service date is provided
    final ZonedDateTime effectiveStartDate = ZonedDateTime.ofInstant(
      transitAlert.getEffectiveStartDate(),
      startTime.getZone()
    );
    final ZonedDateTime effectiveEndDate = ZonedDateTime.ofInstant(
      transitAlert.getEffectiveEndDate(),
      endTime.getZone()
    );

    assertEquals(effectiveStartDate, startTime);
    assertEquals(effectiveEndDate, endTime);
  }

  @Test
  public void testSiriSxUpdateForTripByVehicleJourney() {
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    var modelZoneId = timetableRepository.getTimeZone();
    var situationNumber = "TST:SituationNumber:1234";
    var startTime = LocalDateTime.parse("2014-01-01T00:00:00").atZone(modelZoneId);
    var endTime = LocalDateTime.parse("2014-01-01T23:59:59").atZone(modelZoneId);

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      startTime,
      endTime,
      createAffectsVehicleJourney(tripId.getId(), startTime, null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation), realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    LocalDate serviceDate = LocalDate.of(2014, 1, 1);
    final Collection<TransitAlert> tripPatches = transitAlertService.getTripAlerts(
      tripId,
      serviceDate
    );

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Trip.class));
    assertTrue(matchesEntity(transitAlert, tripId));
  }

  @Test
  public void testSiriSxUpdateForTripAndStopByVehicleJourney() {
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");
    final FeedScopedId stopId0 = new FeedScopedId(FEED_ID, "stop0");
    final FeedScopedId stopId1 = new FeedScopedId(FEED_ID, "stop1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    ZoneId zoneId = timetableRepository.getTimeZone();
    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = LocalDateTime.parse("2014-01-01T00:00:00").atZone(zoneId);
    final ZonedDateTime endTime = LocalDateTime.parse("2014-01-01T23:59:59").atZone(zoneId);

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      startTime,
      endTime,
      createAffectsVehicleJourney(tripId.getId(), startTime, stopId0.getId(), stopId1.getId())
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation), realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final LocalDate serviceDate = LocalDate.of(2014, 1, 1);

    Collection<TransitAlert> tripPatches = transitAlertService.getStopAndTripAlerts(
      stopId0,
      tripId,
      serviceDate
    );

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(matchesEntity(transitAlert, stopId0, tripId, serviceDate));

    tripPatches = transitAlertService.getStopAndTripAlerts(stopId1, tripId, serviceDate);
    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndTrip.class));
    assertTrue(matchesEntity(transitAlert, stopId1, tripId, serviceDate));
  }

  @Test
  public void testSiriSxUpdateForTripByDatedVehicleJourney() {
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      startTime,
      endTime,
      createAffectsDatedVehicleJourney(tripId.getId(), null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation), realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    LocalDate serviceDate = LocalDate.of(2014, 1, 1);
    final Collection<TransitAlert> tripPatches = transitAlertService.getTripAlerts(
      tripId,
      serviceDate
    );

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Trip.class));
    assertTrue(matchesEntity(transitAlert, tripId));
  }

  @Test
  public void testSiriSxUpdateForLine() {
    final FeedScopedId lineRef = new FeedScopedId(FEED_ID, "route0");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      startTime,
      endTime,
      createAffectsLine(lineRef.getId(), null)
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final Collection<TransitAlert> tripPatches = transitAlertService.getRouteAlerts(lineRef);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Route.class));
    assertTrue(matchesEntity(transitAlert, lineRef));
    assertEquals(situationNumber, transitAlert.getId().getId());

    // Effective validity should be calculated based on the actual departures when Operating dat/service date is provided
    final ZonedDateTime effectiveStartDate = ZonedDateTime.ofInstant(
      transitAlert.getEffectiveStartDate(),
      startTime.getZone()
    );
    final ZonedDateTime effectiveEndDate = ZonedDateTime.ofInstant(
      transitAlert.getEffectiveEndDate(),
      endTime.getZone()
    );

    assertEquals(startTime, effectiveStartDate);
    assertEquals(endTime, effectiveEndDate);
  }

  @Test
  public void testSiriSxUpdateForLineThenExpiry() {
    final FeedScopedId lineRef = new FeedScopedId(FEED_ID, "route0");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
    final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      startTime,
      endTime,
      createAffectsLine(lineRef.getId(), null)
    );

    alertsUpdateHandler.update(createServiceDelivery(ptSituation), realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    Collection<TransitAlert> tripPatches = transitAlertService.getRouteAlerts(lineRef);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    final TransitAlert transitAlert = tripPatches.iterator().next();
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Route.class));
    assertTrue(matchesEntity(transitAlert, lineRef));
    assertEquals(situationNumber, transitAlert.getId().getId());

    ptSituation = createPtSituationElement(
      situationNumber,
      startTime,
      endTime,
      createAffectsLine(lineRef.getId(), null)
    );

    ptSituation.setProgress(WorkflowStatusEnumeration.CLOSED);

    alertsUpdateHandler.update(createServiceDelivery(ptSituation), realTimeUpdateContext);

    tripPatches = transitAlertService.getRouteAlerts(lineRef);

    assertNotNull(tripPatches);
    assertTrue(tripPatches.isEmpty());
  }

  @Test
  public void testSiriSxUpdateForTripAndStop() {
    final FeedScopedId tripId = new FeedScopedId(FEED_ID, "route0-trip1");

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final FeedScopedId stopId0 = new FeedScopedId(FEED_ID, "stop0");
    final FeedScopedId stopId1 = new FeedScopedId(FEED_ID, "stop1");

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
      ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
      createAffectsFramedVehicleJourney(
        tripId.getId(),
        "2014-01-01",
        stopId0.getId(),
        stopId1.getId()
      )
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    /*
     * Trip and stop-alerts should result in several TransitAlertes. One for each tripId/stop combination
     */

    final LocalDate serviceDate = LocalDate.of(2014, 1, 1);
    Collection<TransitAlert> tripPatches = transitAlertService.getStopAndTripAlerts(
      stopId0,
      tripId,
      serviceDate
    );

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndTrip.class));
    assertTrue(matchesEntity(transitAlert, stopId0, tripId, serviceDate));

    tripPatches = transitAlertService.getStopAndTripAlerts(stopId1, tripId, serviceDate);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndTrip.class));
    assertTrue(matchesEntity(transitAlert, stopId1, tripId, serviceDate));
  }

  @Test
  public void testSiriSxUpdateForLineAndStop() {
    final String routeId = "route0";

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final String stopId0 = "stop0";
    final String stopId1 = "stop1";
    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
      ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
      createAffectsLine(routeId, stopId0, stopId1)
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    /*
     * Line and stop-alerts should result in several TransitAlertes. One for each routeId/stop combination
     */

    assertLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);
  }

  @Test
  public void testSiriSxUpdateForLineAndExternallyDefinedStopPoint() {
    final String routeId = "route0";

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final String stopId0 = "stop0";
    final String stopId1 = "stop1";
    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
      ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
      createAffectsLineWithExternallyDefinedStopPoints(routeId, stopId0, stopId1)
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    assertSeparateLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);
  }

  @Test
  public void testSiriSxWithOpenEndedValidity() {
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final String stopId0 = "stop0";

    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
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
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());

    final FeedScopedId stopId = new FeedScopedId(FEED_ID, stopId0);

    Collection<TransitAlert> tripPatches = transitAlertService.getStopAlerts(stopId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId().getId());

    assertNotNull(transitAlert.getEffectiveStartDate());

    assertEquals(period_1.getStartTime().toInstant(), transitAlert.getEffectiveStartDate());

    assertNull(transitAlert.getEffectiveEndDate());
  }

  @Test
  public void testSiriSxUpdateForLineAndExternallyDefinedStopPlace() {
    final String routeId = "route0";

    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    final String stopId0 = "stop0";
    final String stopId1 = "stop1";
    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
      ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
      createAffectsLineWithExternallyDefinedStopPlaces(routeId, stopId0, stopId1)
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    assertFalse(transitAlertService.getAllAlerts().isEmpty());
    assertSeparateLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);
  }

  @Test
  public void testSiriSxUpdateForUnknownEntity() {
    assertTrue(transitAlertService.getAllAlerts().isEmpty());

    final String situationNumber = "TST:SituationNumber:1234";
    PtSituationElement ptSituation = createPtSituationElement(
      situationNumber,
      ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
      ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
      null
    );

    final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
    alertsUpdateHandler.update(serviceDelivery, realTimeUpdateContext);

    Collection<TransitAlert> alerts = transitAlertService.getAllAlerts();
    assertEquals(1, alerts.size());
    TransitAlert transitAlert = alerts.iterator().next();
    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.Unknown.class));
  }

  private PtSituationElement createPtSituationElement(
    String situationNumber,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    AffectsScopeStructure affects
  ) {
    PtSituationElement element = new PtSituationElement();
    element.setCreationTime(ZonedDateTime.now());
    element.setProgress(WorkflowStatusEnumeration.OPEN);
    if ((startTime != null) | (endTime != null)) {
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
    List<RoutePointTypeEnumeration> stopConditions,
    String... stopIds
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
    return createServiceDelivery(List.of(situationElement));
  }

  private boolean containsOnlyEntitiesOfClass(TransitAlert transitAlert, Class<?> cls) {
    long totalEntityCount = transitAlert.entities().size();
    long clsEntityCount = transitAlert.entities().stream().filter(cls::isInstance).count();
    return clsEntityCount > 0 && clsEntityCount == totalEntityCount;
  }

  private boolean matchesEntity(TransitAlert transitAlert, FeedScopedId feedScopedEntityId) {
    boolean foundMatch = false;
    for (EntitySelector entity : transitAlert.entities()) {
      if (!foundMatch) {
        if (entity instanceof EntitySelector.Stop entitySelector) {
          foundMatch = entitySelector.stopId().equals(feedScopedEntityId);
        } else if (entity instanceof EntitySelector.Trip entitySelector) {
          foundMatch = entitySelector.tripId().equals(feedScopedEntityId);
        } else if (entity instanceof EntitySelector.Route entitySelector) {
          foundMatch = entitySelector.routeId().equals(feedScopedEntityId);
        }
      }
    }
    return foundMatch;
  }

  private ServiceDelivery createServiceDelivery(List<PtSituationElement> situationElement) {
    ServiceDelivery delivery = new ServiceDelivery();
    SituationExchangeDeliveryStructure sxDeliveries = new SituationExchangeDeliveryStructure();
    SituationExchangeDeliveryStructure.Situations situations =
      new SituationExchangeDeliveryStructure.Situations();
    situations.getPtSituationElements().addAll(situationElement);
    sxDeliveries.setSituations(situations);
    delivery.getSituationExchangeDeliveries().add(sxDeliveries);

    return delivery;
  }

  private void assertValidity(
    String label,
    TransitAlert transitAlert,
    ZonedDateTime startTimePeriod_1,
    ZonedDateTime endTimePeriod_1
  ) {
    // TimePeriod ends BEFORE first validityPeriod starts
    assertFalse(
      transitAlert.displayDuring(
        startTimePeriod_1.toEpochSecond() - 200,
        startTimePeriod_1.toEpochSecond() - 100
      ),
      "TimePeriod ends BEFORE first validityPeriod starts: " + label
    );

    // TimePeriod ends AFTER first validityPeriod starts, BEFORE it ends
    assertTrue(
      transitAlert.displayDuring(
        startTimePeriod_1.toEpochSecond() - 1000,
        endTimePeriod_1.toEpochSecond() - 100
      ),
      "TimePeriod ends AFTER first validityPeriod starts, BEFORE it ends: " + label
    );

    // TimePeriod starts AFTER first validityPeriod starts, BEFORE it ends
    assertTrue(
      transitAlert.displayDuring(
        startTimePeriod_1.toEpochSecond() + 100,
        endTimePeriod_1.toEpochSecond() - 100
      ),
      "TimePeriod starts AFTER first validityPeriod starts, BEFORE it ends: " + label
    );

    // TimePeriod starts AFTER first validityPeriod starts, ends AFTER it ends
    assertTrue(
      transitAlert.displayDuring(
        startTimePeriod_1.toEpochSecond() + 100,
        endTimePeriod_1.toEpochSecond() + 100
      ),
      "TimePeriod starts AFTER first validityPeriod starts, ends AFTER it ends: " + label
    );

    // TimePeriod starts AFTER first validityPeriod ends
    assertFalse(
      transitAlert.displayDuring(
        endTimePeriod_1.toEpochSecond() + 100,
        endTimePeriod_1.toEpochSecond() + 200
      ),
      "TimePeriod starts AFTER first validityPeriod ends: " + label
    );
  }

  private AffectsScopeStructure createAffectsFramedVehicleJourney(
    String datedVehicleJourney,
    String dataFrameValue,
    String... stopIds
  ) {
    AffectsScopeStructure affects = new AffectsScopeStructure();
    AffectsScopeStructure.VehicleJourneys vehicleJourneys =
      new AffectsScopeStructure.VehicleJourneys();
    AffectedVehicleJourneyStructure affectedVehicleJourney = new AffectedVehicleJourneyStructure();
    FramedVehicleJourneyRefStructure framedVehicleJourneyRef =
      new FramedVehicleJourneyRefStructure();
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

  private AffectsScopeStructure createAffectsVehicleJourney(
    String vehicleJourneyRef,
    ZonedDateTime originAimedDepartureTime,
    String... stopIds
  ) {
    AffectsScopeStructure affects = new AffectsScopeStructure();
    AffectsScopeStructure.VehicleJourneys vehicleJourneys =
      new AffectsScopeStructure.VehicleJourneys();
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

  private AffectsScopeStructure createAffectsDatedVehicleJourney(
    String datedVehicleJourneyRef,
    String... stopIds
  ) {
    AffectsScopeStructure affects = new AffectsScopeStructure();
    AffectsScopeStructure.VehicleJourneys vehicleJourneys =
      new AffectsScopeStructure.VehicleJourneys();
    AffectedVehicleJourneyStructure affectedVehicleJourney = new AffectedVehicleJourneyStructure();

    DatedVehicleJourneyRef datedVehicleJourney = new DatedVehicleJourneyRef();
    datedVehicleJourney.setValue(datedVehicleJourneyRef);
    affectedVehicleJourney.getDatedVehicleJourneyReves().add(datedVehicleJourney);

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

  private boolean matchesEntity(
    TransitAlert transitAlert,
    FeedScopedId stopId,
    FeedScopedId routeOrTripId
  ) {
    return matchesEntity(transitAlert, stopId, routeOrTripId, null);
  }

  private boolean matchesEntity(
    TransitAlert transitAlert,
    FeedScopedId stopId,
    FeedScopedId routeOrTripId,
    LocalDate serviceDate
  ) {
    boolean foundMatch = false;
    for (EntitySelector entity : transitAlert.entities()) {
      if (!foundMatch) {
        if (entity.key() instanceof EntityKey.StopAndRoute stopAndRoute) {
          foundMatch = stopAndRoute.equals((new EntityKey.StopAndRoute(stopId, routeOrTripId)));
        } else if (entity instanceof EntitySelector.StopAndTrip stopAndTrip) {
          foundMatch =
            stopAndTrip.key().equals(new EntityKey.StopAndTrip(stopId, routeOrTripId)) &&
            stopAndTrip.serviceDate().equals(serviceDate);
        }
      }
    }
    return foundMatch;
  }

  private AffectsScopeStructure createAffectsLine(String line, String... stopIds) {
    AffectsScopeStructure affects = new AffectsScopeStructure();

    AffectsScopeStructure.Networks networks = new AffectsScopeStructure.Networks();
    AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork =
      new AffectsScopeStructure.Networks.AffectedNetwork();
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

  private void assertLineAndStopAlerts(
    String situationNumber,
    String routeId,
    String stopId0,
    String stopId1
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

    assertEquals(situationNumber, transitAlert.getId().getId());

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndRoute.class));
    assertTrue(matchesEntity(transitAlert, feedStop_0_id, feedRouteId));

    final FeedScopedId feedStop_1_id = new FeedScopedId(FEED_ID, stopId1);
    tripPatches = transitAlertService.getStopAndRouteAlerts(feedStop_1_id, feedRouteId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();

    assertEquals(situationNumber, transitAlert.getId().getId());

    assertTrue(containsOnlyEntitiesOfClass(transitAlert, EntitySelector.StopAndRoute.class));
    assertTrue(matchesEntity(transitAlert, feedStop_1_id, feedRouteId));
  }

  private AffectsScopeStructure createAffectsLineWithExternallyDefinedStopPoints(
    String line,
    String... stopIds
  ) {
    AffectsScopeStructure affects = new AffectsScopeStructure();

    AffectsScopeStructure.Networks networks = new AffectsScopeStructure.Networks();
    AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork =
      new AffectsScopeStructure.Networks.AffectedNetwork();
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
    String situationNumber,
    String routeId,
    String stopId0,
    String stopId1
  ) {
    /*
     * Line and external stop-alerts should result in several AlertPatches. One for each routeId AND for each stop
     */

    final FeedScopedId feedRouteId = new FeedScopedId(FEED_ID, routeId);
    Collection<TransitAlert> tripPatches = transitAlertService.getRouteAlerts(feedRouteId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    TransitAlert transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(matchesEntity(transitAlert, feedRouteId));

    FeedScopedId feedStopId = new FeedScopedId(FEED_ID, stopId0);
    tripPatches = transitAlertService.getStopAlerts(feedStopId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(matchesEntity(transitAlert, feedStopId));

    feedStopId = new FeedScopedId(FEED_ID, stopId1);
    tripPatches = transitAlertService.getStopAlerts(feedStopId);

    assertNotNull(tripPatches);
    assertEquals(1, tripPatches.size());
    transitAlert = tripPatches.iterator().next();
    assertEquals(situationNumber, transitAlert.getId().getId());
    assertTrue(matchesEntity(transitAlert, feedStopId));
  }

  private AffectsScopeStructure createAffectsLineWithExternallyDefinedStopPlaces(
    String line,
    String... stopIds
  ) {
    AffectsScopeStructure affects = new AffectsScopeStructure();

    AffectsScopeStructure.Networks networks = new AffectsScopeStructure.Networks();
    AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork =
      new AffectsScopeStructure.Networks.AffectedNetwork();
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
}
