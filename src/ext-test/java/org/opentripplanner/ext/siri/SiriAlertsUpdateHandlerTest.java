package org.opentripplanner.ext.siri;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
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

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SiriAlertsUpdateHandlerTest  extends GtfsTest {

        SiriAlertsUpdateHandler alertsUpdateHandler;

        RoutingService routingService;

        public void init() {
            if (routingService == null) {
                routingService = new RoutingService(graph);
            } else {
                routingService.getSiriAlertPatchService().expireAll();
            }
            if (alertsUpdateHandler == null) {
                alertsUpdateHandler = new SiriAlertsUpdateHandler("TEST");
                alertsUpdateHandler.setAlertPatchService(routingService.getSiriAlertPatchService());
                alertsUpdateHandler.setSiriFuzzyTripMatcher(new SiriFuzzyTripMatcher(routingService));
            }
        }

        @Test
        public void testSiriSxUpdateForStop() {
            init();
            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final String stopId = "stop0";
            List<RoutePointTypeEnumeration> stopConditions = Arrays.asList(RoutePointTypeEnumeration.DESTINATION, RoutePointTypeEnumeration.NOT_STOPPING, RoutePointTypeEnumeration.REQUEST_STOP, RoutePointTypeEnumeration.EXCEPTIONAL_STOP, RoutePointTypeEnumeration.START_POINT);

            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
                    ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
                    createAffectsStop(stopConditions, stopId));

            long priorityValue = 3;
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

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final Collection<AlertPatch> stopPatches = routingService.getSiriAlertPatchService().getStopPatches(new FeedScopedId("FEED", stopId));

            assertNotNull(stopPatches);
            assertEquals(1, stopPatches.size());
            final AlertPatch alertPatch = stopPatches.iterator().next();
            assertNull(alertPatch.getTrip());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertEquals(reportType, alertPatch.getAlert().alertType);
            assertEquals(severity.value(), alertPatch.getAlert().severity);
            assertEquals(priorityValue, alertPatch.getAlert().priority);
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId, alertPatch.getStop().getId());

            assertTrue(alertPatch.getStopConditions().contains(StopCondition.DESTINATION));
            assertTrue(alertPatch.getStopConditions().contains(StopCondition.NOT_STOPPING));
            assertTrue(alertPatch.getStopConditions().contains(StopCondition.REQUEST_STOP));

            assertNotNull(alertPatch.getAlert().getAlertUrlList());
            assertFalse(alertPatch.getAlert().getAlertUrlList().isEmpty());

            final List<AlertUrl> alertUrlList = alertPatch.getAlert().getAlertUrlList();
            AlertUrl alertUrl = alertUrlList.get(0);
            assertEquals(infoLinkUri, alertUrl.uri);
            assertEquals(infoLinkLabel, alertUrl.label);

        }

        @Test
        public void testSiriSxUpdateForStopMultipleValidityPeriods() {
            init();
            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final String stopId = "stop0";
            List<RoutePointTypeEnumeration> stopConditions = Arrays.asList(RoutePointTypeEnumeration.DESTINATION, RoutePointTypeEnumeration.NOT_STOPPING, RoutePointTypeEnumeration.REQUEST_STOP);

            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    null,
                    null,
                    createAffectsStop(stopConditions, stopId));

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

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final Collection<AlertPatch> stopPatches = routingService.getSiriAlertPatchService().getStopPatches(new FeedScopedId("FEED", stopId));

            assertNotNull(stopPatches);
            assertEquals(1, stopPatches.size());
            final AlertPatch alertPatch = stopPatches.iterator().next();
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId, alertPatch.getStop().getId());

            assertValidity("period 1", alertPatch, startTimePeriod_1, endTimePeriod_1);

            assertValidity("period 2", alertPatch, startTimePeriod_2, endTimePeriod_2);

        }

        private void assertValidity(String label, AlertPatch alertPatch, ZonedDateTime startTimePeriod_1, ZonedDateTime endTimePeriod_1) {
            // TimePeriod ends BEFORE first validityPeriod starts
            assertFalse("TimePeriod ends BEFORE first validityPeriod starts: " + label, alertPatch.displayDuring(startTimePeriod_1.toEpochSecond()-200, startTimePeriod_1.toEpochSecond()-100));

            // TimePeriod ends AFTER first validityPeriod starts, BEFORE it ends
            assertTrue("TimePeriod ends AFTER first validityPeriod starts, BEFORE it ends: " + label, alertPatch.displayDuring(startTimePeriod_1.toEpochSecond()-1000, endTimePeriod_1.toEpochSecond()-100));

            // TimePeriod starts AFTER first validityPeriod starts, BEFORE it ends
            assertTrue("TimePeriod starts AFTER first validityPeriod starts, BEFORE it ends: " + label, alertPatch.displayDuring(startTimePeriod_1.toEpochSecond()+100, endTimePeriod_1.toEpochSecond()-100));

            // TimePeriod starts AFTER first validityPeriod starts, ends AFTER it ends
            assertTrue("TimePeriod starts AFTER first validityPeriod starts, ends AFTER it ends: " + label, alertPatch.displayDuring(startTimePeriod_1.toEpochSecond()+100, endTimePeriod_1.toEpochSecond()+100));

            // TimePeriod starts AFTER first validityPeriod ends
            assertFalse("TimePeriod starts AFTER first validityPeriod ends: " + label, alertPatch.displayDuring(endTimePeriod_1.toEpochSecond()+100, endTimePeriod_1.toEpochSecond()+200));
        }


        @Test
        public void testSiriSxUpdateForMultipleStops() {
            init();
            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final String stopId0 = "stop0";
            final String stopId1 = "stop1";
            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
                    ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
                    createAffectsStop(null, stopId0, stopId1));

            final String reportType = "incident";
            ptSituation.setReportType(reportType);

            final SeverityEnumeration severity = SeverityEnumeration.SEVERE;
            ptSituation.setSeverity(SeverityEnumeration.SEVERE);

            final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
            alertsUpdateHandler.update(serviceDelivery);

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            Collection<AlertPatch> stopPatches = routingService.getSiriAlertPatchService().getStopPatches(new FeedScopedId("FEED", stopId0));

            assertNotNull(stopPatches);
            assertEquals(1, stopPatches.size());
            AlertPatch alertPatch = stopPatches.iterator().next();
            assertNull(alertPatch.getTrip());
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId0, alertPatch.getStop().getId());

            assertTrue("Alert does not contain default condition START_POINT", alertPatch.getStopConditions().contains(StopCondition.START_POINT));
            assertTrue("Alert does not contain default condition DESTINATION", alertPatch.getStopConditions().contains(StopCondition.DESTINATION));

            stopPatches = routingService.getSiriAlertPatchService().getStopPatches(new FeedScopedId("FEED", stopId1));

            assertNotNull(stopPatches);
            assertEquals(1, stopPatches.size());
            alertPatch = stopPatches.iterator().next();
            assertNull(alertPatch.getTrip());
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId1, alertPatch.getStop().getId());

            assertTrue("Alert does not contain default condition START_POINT", alertPatch.getStopConditions().contains(StopCondition.START_POINT));
            assertTrue("Alert does not contain default condition DESTINATION", alertPatch.getStopConditions().contains(StopCondition.DESTINATION));
        }

        @Test
        public void testSiriSxUpdateForTrip() {
            init();
            final String tripId = "route0-trip1";

            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
            final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    startTime,
                    endTime,
                    createAffectsFramedVehicleJourney(tripId, "2014-01-01", null));

            alertsUpdateHandler.update(createServiceDelivery(ptSituation));

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final Collection<AlertPatch> tripPatches = routingService.getSiriAlertPatchService().getTripPatches(new FeedScopedId("FEED", tripId));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            final AlertPatch alertPatch = tripPatches.iterator().next();
            assertEquals(tripId, alertPatch.getTrip().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNull(alertPatch.getStop());
            assertNull(alertPatch.getRoute());


            // Effective validity should be calculated based on the actual departures when Operating dat/service date is provided
            final ZonedDateTime effectiveStartDate = ZonedDateTime.ofInstant(alertPatch.getAlert().effectiveStartDate.toInstant(), startTime.getZone());
            final ZonedDateTime effectiveEndDate = ZonedDateTime.ofInstant(alertPatch.getAlert().effectiveEndDate.toInstant(), endTime.getZone());

            assertTrue(effectiveStartDate.isAfter(startTime));
            assertTrue(effectiveEndDate.isBefore(endTime));

        }

        @Test
        public void testSiriSxUpdateForTripByVehicleJourney() {
            init();
            final String tripId = "route0-trip1";

            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
            final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    startTime,
                    endTime,
                    createAffectsVehicleJourney(tripId, startTime, null));

            alertsUpdateHandler.update(createServiceDelivery(ptSituation));

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final Collection<AlertPatch> tripPatches = routingService.getSiriAlertPatchService().getTripPatches(new FeedScopedId("FEED", tripId));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            final AlertPatch alertPatch = tripPatches.iterator().next();
            assertEquals(tripId, alertPatch.getTrip().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNull(alertPatch.getStop());
            assertNull(alertPatch.getRoute());
        }


        @Test
        public void testSiriSxUpdateForTripAndStopByVehicleJourney() {
            init();
            final String tripId = "route0-trip1";

            final String stopId0 = "stop0";
            final String stopId1 = "stop1";
            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
            final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    startTime,
                    endTime,
                    createAffectsVehicleJourney(tripId, startTime, stopId0, stopId1));

            alertsUpdateHandler.update(createServiceDelivery(ptSituation));

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            Collection<AlertPatch> tripPatches = routingService.getSiriAlertPatchService().getStopAndTripPatches(new FeedScopedId("FEED", stopId0), new FeedScopedId("FEED", tripId));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            AlertPatch alertPatch = tripPatches.iterator().next();
            assertEquals(tripId, alertPatch.getTrip().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId0, alertPatch.getStop().getId());

            tripPatches = routingService.getSiriAlertPatchService().getStopAndTripPatches(new FeedScopedId("FEED", stopId1), new FeedScopedId("FEED", tripId));
            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            alertPatch = tripPatches.iterator().next();
            assertEquals(tripId, alertPatch.getTrip().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId1, alertPatch.getStop().getId());

        }

        @Test
        public void testSiriSxUpdateForLine() {
            init();
            final String lineRef = "route0";

            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
            final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    startTime,
                    endTime,
                    createAffectsLine(lineRef, null));

            final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
            alertsUpdateHandler.update(serviceDelivery);

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final Collection<AlertPatch> tripPatches = routingService.getSiriAlertPatchService().getRoutePatches(new FeedScopedId("FEED", lineRef));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            final AlertPatch alertPatch = tripPatches.iterator().next();
            assertEquals(lineRef, alertPatch.getRoute().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNull(alertPatch.getStop());
            assertNull(alertPatch.getTrip());


            // Effective validity should be calculated based on the actual departures when Operating dat/service date is provided
            final ZonedDateTime effectiveStartDate = ZonedDateTime.ofInstant(alertPatch.getAlert().effectiveStartDate.toInstant(), startTime.getZone());
            final ZonedDateTime effectiveEndDate = ZonedDateTime.ofInstant(alertPatch.getAlert().effectiveEndDate.toInstant(), endTime.getZone());

            assertEquals(startTime, effectiveStartDate);
            assertEquals(endTime, effectiveEndDate);

        }



        @Test
        public void testSiriSxUpdateForLineThenExpiry() {
            init();
            final String lineRef = "route0";

            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final ZonedDateTime startTime = ZonedDateTime.parse("2014-01-01T00:00:00+01:00");
            final ZonedDateTime endTime = ZonedDateTime.parse("2014-01-01T23:59:59+01:00");

            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    startTime,
                    endTime,
                    createAffectsLine(lineRef, null));

            alertsUpdateHandler.update(createServiceDelivery(ptSituation));

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            Collection<AlertPatch> tripPatches = routingService.getSiriAlertPatchService().getRoutePatches(new FeedScopedId("FEED", lineRef));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            final AlertPatch alertPatch = tripPatches.iterator().next();
            assertEquals(lineRef, alertPatch.getRoute().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());

            ptSituation = createPtSituationElement(
                    situationNumber,
                    startTime,
                    endTime,
                    createAffectsLine(lineRef, null));

            ptSituation.setProgress(WorkflowStatusEnumeration.CLOSED);

            alertsUpdateHandler.update(createServiceDelivery(ptSituation));

            tripPatches = routingService.getSiriAlertPatchService().getRoutePatches(new FeedScopedId("FEED", lineRef));

            assertNotNull(tripPatches);
            assertTrue(tripPatches.isEmpty());
        }

        @Test
        public void testSiriSxUpdateForTripAndStop() {
            init();
            final String tripId = "route0-trip1";

            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final String stopId0 = "stop0";
            final String stopId1 = "stop1";
            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
                    ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
                    createAffectsFramedVehicleJourney(tripId, "2014-01-01", stopId0, stopId1));

            final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
            alertsUpdateHandler.update(serviceDelivery);

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            /*
             * Trip and stop-alerts should result in several AlertPatches. One for each tripId/stop combination
             */

            Collection<AlertPatch> tripPatches = routingService.getSiriAlertPatchService().getStopAndTripPatches(new FeedScopedId("FEED", stopId0), new FeedScopedId("FEED", tripId));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            AlertPatch alertPatch = tripPatches.iterator().next();
            assertEquals(tripId, alertPatch.getTrip().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId0, alertPatch.getStop().getId());


            tripPatches = routingService.getSiriAlertPatchService().getStopAndTripPatches(new FeedScopedId("FEED", stopId1), new FeedScopedId("FEED", tripId));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            alertPatch = tripPatches.iterator().next();
            assertEquals(tripId, alertPatch.getTrip().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId1, alertPatch.getStop().getId());
        }

        @Test
        public void testSiriSxUpdateForLineAndStop() {
            init();
            final String routeId = "route0";

            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final String stopId0 = "stop0";
            final String stopId1 = "stop1";
            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
                    ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
                    createAffectsLine(routeId, stopId0, stopId1));

            final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
            alertsUpdateHandler.update(serviceDelivery);

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            /*
             * Line and stop-alerts should result in several AlertPatches. One for each routeId/stop combination
             */

            assertLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);
        }

        @Test
        public void testSiriSxUpdateForLineAndExternallyDefinedStopPoint() {
            init();
            final String routeId = "route0";

            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final String stopId0 = "stop0";
            final String stopId1 = "stop1";
            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
                    ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
                    createAffectsLineWithExternallyDefinedStopPoints(routeId, stopId0, stopId1));

            final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
            alertsUpdateHandler.update(serviceDelivery);

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            /*
             * Line and stop-alerts should result in several AlertPatches. One for each routeId/stop combination
             */

            assertLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);
        }

        @Test
        public void testSiriSxUpdateForLineAndExternallyDefinedStopPlace() {
            init();
            final String routeId = "route0";

            assertTrue(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());

            final String situationNumber = "TST:SituationNumber:1234";
            final String stopId0 = "stop0";
            final String stopId1 = "stop1";
            PtSituationElement ptSituation = createPtSituationElement(
                    situationNumber,
                    ZonedDateTime.parse("2014-01-01T00:00:00+01:00"),
                    ZonedDateTime.parse("2014-01-01T23:59:59+01:00"),
                    createAffectsLineWithExternallyDefinedStopPlaces(routeId, stopId0, stopId1));

            final ServiceDelivery serviceDelivery = createServiceDelivery(ptSituation);
            alertsUpdateHandler.update(serviceDelivery);

            assertFalse(routingService.getSiriAlertPatchService().getAllAlertPatches().isEmpty());
            assertLineAndStopAlerts(situationNumber, routeId, stopId0, stopId1);

        }

        private void assertLineAndStopAlerts(String situationNumber, String routeId, String stopId0, String stopId1) {
            /*
             * Line and stop-alerts should result in several AlertPatches. One for each routeId/stop combination
             */

            Collection<AlertPatch> tripPatches = routingService.getSiriAlertPatchService().getStopAndRoutePatches(new FeedScopedId("FEED", stopId0), new FeedScopedId("FEED", routeId));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            AlertPatch alertPatch = tripPatches.iterator().next();
            assertEquals(routeId, alertPatch.getRoute().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId0, alertPatch.getStop().getId());


            tripPatches = routingService.getSiriAlertPatchService().getStopAndRoutePatches(new FeedScopedId("FEED", stopId1), new FeedScopedId("FEED", routeId));

            assertNotNull(tripPatches);
            assertEquals(1, tripPatches.size());
            alertPatch = tripPatches.iterator().next();
            assertEquals(routeId, alertPatch.getRoute().getId());
            assertEquals(situationNumber, alertPatch.getSituationNumber());
            assertNotNull(alertPatch.getStop());
            assertEquals(stopId1, alertPatch.getStop().getId());
        }

        private ServiceDelivery createServiceDelivery(PtSituationElement situationElement) {
            return createServiceDelivery(Arrays.asList(situationElement));
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



        private AffectsScopeStructure createAffectsVehicleJourney(String vehicleJourneyRef, ZonedDateTime originAimedDepartureTime, String... stopIds) {
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
        private AffectsScopeStructure createAffectsFramedVehicleJourney(String datedVehicleJourney, String dataFrameValue, String... stopIds) {
            AffectsScopeStructure affects = new AffectsScopeStructure();
            AffectsScopeStructure.VehicleJourneys vehicleJourneys = new AffectsScopeStructure.VehicleJourneys();
            AffectedVehicleJourneyStructure affectedVehicleJourney = new AffectedVehicleJourneyStructure();
            FramedVehicleJourneyRefStructure framedVehicleJourneyRef = new FramedVehicleJourneyRefStructure();
            framedVehicleJourneyRef.setDatedVehicleJourneyRef(datedVehicleJourney);
            DataFrameRefStructure dataFrameRef = new DataFrameRefStructure();
            dataFrameRef.setValue(dataFrameValue);
            framedVehicleJourneyRef.setDataFrameRef(dataFrameRef);
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

        private AffectsScopeStructure createAffectsLineWithExternallyDefinedStopPoints(String line, String... stopIds) {

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

        private AffectsScopeStructure createAffectsLineWithExternallyDefinedStopPlaces(String line, String... stopIds) {

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

        private AffectsScopeStructure createAffectsStop(List<RoutePointTypeEnumeration> stopConditions, String... stopIds) {

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

        private PtSituationElement createPtSituationElement(String situationNumber, ZonedDateTime startTime, ZonedDateTime endTime, AffectsScopeStructure affects) {
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

        private DefaultedTextStructure createDefaultedTextStructure(String value) {
            DefaultedTextStructure textStructure = new DefaultedTextStructure();
            textStructure.setValue(value);
            return textStructure;
        }


        @Override
        public String getFeedName() {
            return "gtfs/interlining";
        }
    }

