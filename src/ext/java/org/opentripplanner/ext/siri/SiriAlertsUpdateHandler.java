package org.opentripplanner.ext.siri;

import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.*;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.*;

import java.io.Serializable;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This updater applies the equivalent of GTFS Alerts, but from SIRI Situation Exchange feeds.
 * NOTE this cannot handle situations where there are multiple feeds with different IDs (for now it may only work in
 * single-feed regions).
 */
public class SiriAlertsUpdateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SiriAlertsUpdateHandler.class);

    private AlertPatchService alertPatchService;

    /** How long before the posted start of an event it should be displayed to users */
    private long earlyStart;

    private SiriFuzzyTripMatcher siriFuzzyTripMatcher;

    private String feedId;

    public SiriAlertsUpdateHandler(String feedId) {
        this.feedId = feedId;
    }

    public void update(ServiceDelivery delivery) {
        for (SituationExchangeDeliveryStructure sxDelivery : delivery.getSituationExchangeDeliveries()) {
            SituationExchangeDeliveryStructure.Situations situations = sxDelivery.getSituations();
            if (situations != null) {
                long t1 = System.currentTimeMillis();
                Set<String> idsToExpire = new HashSet<>();
                Set<AlertPatch> alertPatches = new HashSet<>();

                for (PtSituationElement sxElement : situations.getPtSituationElements()) {
                    Pair<Set<String>, Set<AlertPatch>> alertPatchChangePair = handleAlert(sxElement);
                    if (alertPatchChangePair != null) {
                        idsToExpire.addAll(alertPatchChangePair.getLeft());
                        alertPatches.addAll(alertPatchChangePair.getRight());
                    }
                }

                alertPatchService.expire(idsToExpire);
                alertPatchService.applyAll(alertPatches);

                LOG.info("Added {} alerts, expired {} alerts based on {} situations, current alert-count: {}, elapsed time {}ms", alertPatches.size(), idsToExpire.size(), situations.getPtSituationElements().size(), alertPatchService.getAllAlertPatches().size(), (System.currentTimeMillis()-t1));
            }
        }
    }

    private Pair<Set<String>, Set<AlertPatch>> handleAlert(PtSituationElement situation) {

        Alert alert = createAlertWithTexts(situation);

        Set<String> idsToExpire = new HashSet<>();
        boolean expireSituation = (situation.getProgress() != null &&
                situation.getProgress().equals(WorkflowStatusEnumeration.CLOSED));

        //ROR-54
        if (!expireSituation && //If situation is closed, it must be allowed - it will remove already existing alerts
                areAllTextsEmpty(alert)) {
            LOG.debug("Empty Alert - ignoring situationNumber: {}", situation.getSituationNumber() != null ? situation.getSituationNumber().getValue():null);
            return null;
        }

        ArrayList<TimePeriod> periods = new ArrayList<>();
        if(situation.getValidityPeriods().size() > 0) {
            long bestStartTime = Long.MAX_VALUE;
            long bestEndTime = 0;
            for (HalfOpenTimestampOutputRangeStructure activePeriod : situation.getValidityPeriods()) {

                final long realStart = activePeriod.getStartTime() != null ? activePeriod.getStartTime().toInstant().toEpochMilli() : 0;
                final long start = activePeriod.getStartTime() != null? realStart - earlyStart : 0;
                if (realStart > 0 && realStart < bestStartTime) {
                    bestStartTime = realStart;
                }

                final long realEnd = activePeriod.getEndTime() != null ? activePeriod.getEndTime().toInstant().toEpochMilli() : 0;
                final long end = activePeriod.getEndTime() != null? realEnd  : 0;
                if (realEnd > 0 && realEnd > bestEndTime) {
                    bestEndTime = realEnd;
                }

                periods.add(new TimePeriod(start/1000, end/1000));
            }
            if (bestStartTime != Long.MAX_VALUE) {
                alert.effectiveStartDate = new Date(bestStartTime);
            }
            if (bestEndTime != 0) {
                alert.effectiveEndDate = new Date(bestEndTime);
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, Long.MAX_VALUE));
        }

        String situationNumber;

        if (situation.getSituationNumber() != null) {
            situationNumber = situation.getSituationNumber().getValue();
        } else {
            situationNumber = null;
        }

        String paddedSituationNumber = situationNumber + ":";

        Set<AlertPatch> patches = new HashSet<>();
        AffectsScopeStructure affectsStructure = situation.getAffects();

        if (affectsStructure != null) {

            final Pair<Set<String>, Set<AlertPatch>> operatorAlerts = createOperatorAlerts(paddedSituationNumber, expireSituation, periods, affectsStructure.getOperators());
            idsToExpire.addAll(operatorAlerts.getLeft());
            patches.addAll(operatorAlerts.getRight());

            AffectsScopeStructure.Networks networks = affectsStructure.getNetworks();
            Set<Route> stopRoutes = getRoutes(networks);

            final Pair<Set<String>, Set<AlertPatch>> stopAlerts = createStopAlerts(paddedSituationNumber, expireSituation, periods, stopRoutes, affectsStructure.getStopPoints(), affectsStructure.getStopPlaces());
            idsToExpire.addAll(stopAlerts.getLeft());
            patches.addAll(stopAlerts.getRight());

            final Pair<Set<String>, Set<AlertPatch>> networkAlerts = createNetworkAlerts(paddedSituationNumber, expireSituation, periods, alert, networks);
            idsToExpire.addAll(networkAlerts.getLeft());
            patches.addAll(networkAlerts.getRight());


            final Pair<Set<String>, Set<AlertPatch>> vehicleJourneyAlerts = createVehicleJourneyAlerts(paddedSituationNumber, expireSituation, alert, affectsStructure.getVehicleJourneys());
            idsToExpire.addAll(vehicleJourneyAlerts.getLeft());
            patches.addAll(vehicleJourneyAlerts.getRight());
        }


        // Alerts are not partially updated - cancel ALL current related alerts before adding updated.
        idsToExpire.addAll(alertPatchService.getAllAlertPatches()
                .stream()
                .filter(alertPatch -> alertPatch.getSituationNumber().equals(situationNumber))
                .map(AlertPatch::getId)
                .collect(Collectors.toList()));

        if (!patches.isEmpty() || !idsToExpire.isEmpty()) {

            for (AlertPatch patch : patches) {
                if (patch.getAlert() == null) {
                    patch.setAlert(alert);
                }

                if (patch.getAlert().effectiveStartDate == null) {
                    patch.getAlert().effectiveStartDate = alert.effectiveStartDate;
                }
                if (patch.getAlert().effectiveEndDate == null) {
                    patch.getAlert().effectiveEndDate = alert.effectiveEndDate;
                }

                patch.getAlert().alertType = situation.getReportType();

                if (situation.getPriority() != null && situation.getPriority().intValue() > 0) {
                    patch.getAlert().priority = situation.getPriority().intValue();
                }

                if (situation.getSeverity() != null) {
                    patch.getAlert().severity = situation.getSeverity().value();
                } else {
                    // When severity is not set - use default
                    patch.getAlert().severity = SeverityEnumeration.NORMAL.value();
                }

                if (situation.getParticipantRef() != null) {
                    String codespace = situation.getParticipantRef().getValue();
                    patch.setFeedId(codespace + ":Authority:" + codespace); //TODO: Should probably not assume this codespace -> authority rule
                }

                patch.setSituationNumber(situationNumber);
            }
        } else if (expireSituation) {
            LOG.debug("Expired non-existing alert - ignoring situation with situationNumber {}", situationNumber);
        } else {
            LOG.info("No match found for Alert - ignoring situation with situationNumber {}", situationNumber);
        }

        return Pair.of(idsToExpire, patches);
    }

    private boolean areAllTextsEmpty(Alert alert) {
        return (alert.alertHeaderText == null || alert.alertHeaderText.toString().isEmpty()) &&
                (alert.alertDescriptionText == null || alert.alertDescriptionText.toString().isEmpty()) &&
                (alert.alertDetailText == null || alert.alertDetailText.toString().isEmpty());
    }

    private Set<Route> getRoutes(AffectsScopeStructure.Networks networks) {
        Set<Route> stopRoutes = new HashSet<>();
        if (networks != null) {
            for (AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork : networks.getAffectedNetworks()) {
                List<AffectedLineStructure> affectedLines = affectedNetwork.getAffectedLines();
                if (!isListNullOrEmpty(affectedLines)) {
                    for (AffectedLineStructure line : affectedLines) {

                        LineRef lineRef = line.getLineRef();

                        if (lineRef != null && lineRef.getValue() != null) {
                            stopRoutes.addAll(siriFuzzyTripMatcher.getRoutes(lineRef.getValue()));
                        }
                    }
                }
            }
        }
        return stopRoutes;
    }

    private Pair<Set<String>, Set<AlertPatch>> createVehicleJourneyAlerts(String paddedSituationNumber, boolean expireSituation, Alert alert, AffectsScopeStructure.VehicleJourneys vjs) {
        Set<String> idsToExpire = new HashSet<>();
        Set<AlertPatch> patches = new HashSet<>();

        if (vjs != null && !isListNullOrEmpty(vjs.getAffectedVehicleJourneies())) {
            for (AffectedVehicleJourneyStructure affectedVehicleJourney :  vjs.getAffectedVehicleJourneies()) {

                List<AffectedStopPointStructure> affectedStops = getAffectedStopPoints(affectedVehicleJourney);

                List<VehicleJourneyRef> vehicleJourneyReves = affectedVehicleJourney.getVehicleJourneyReves();

                if (!isListNullOrEmpty(vehicleJourneyReves)) {
                    for (VehicleJourneyRef vehicleJourneyRef : vehicleJourneyReves) {

                        List<FeedScopedId> tripIds = new ArrayList<>();

                        FeedScopedId tripIdFromVehicleJourney = siriFuzzyTripMatcher.getTripId(vehicleJourneyRef.getValue());
                        Date effectiveStartDate = null;
                        Date effectiveEndDate = null;

                        // Need to know if validity is set explicitly or calculated based on ServiceDate
                        boolean effectiveValiditySetExplicitly = false;

                        ZonedDateTime originAimedDepartureTime = (affectedVehicleJourney.getOriginAimedDepartureTime() != null ? affectedVehicleJourney.getOriginAimedDepartureTime() : ZonedDateTime.now());
                        ServiceDate serviceDate = new ServiceDate(originAimedDepartureTime.getYear(), originAimedDepartureTime.getMonthValue(), originAimedDepartureTime.getDayOfMonth());

                        if (tripIdFromVehicleJourney != null) {

                            tripIds.add(tripIdFromVehicleJourney);

                            // ServiceJourneyId matches - use provided validity
                            effectiveStartDate = alert.effectiveStartDate;
                            effectiveEndDate = alert.effectiveEndDate;

                            effectiveValiditySetExplicitly = true;

                        }

                        for (FeedScopedId tripId : tripIds) {

                            if (!effectiveValiditySetExplicitly) {
                                // Effective validity is set based on ServiceDate - need to calculate correct validity based on departuretimes

                                // Calculate validity based on actual, planned departure/arrival for trip
                                int tripDepartureTime = siriFuzzyTripMatcher.getTripDepartureTime(tripId);
                                int tripArrivalTime = siriFuzzyTripMatcher.getTripArrivalTime(tripId);

                                // ServiceJourneyId does NOT match - calculate validity based on serviceDate (calculated from originalAimedDepartureTime)
                                effectiveStartDate = new Date(serviceDate.getAsDate().getTime() + tripDepartureTime * 1000);

                                // Appending 6 hours to end-validity in case of delays.
                                effectiveEndDate = new Date(effectiveStartDate.getTime() + (tripArrivalTime - tripDepartureTime + 6 * 3600) * 1000);

                                // Verify that calculated validity does not exceed explicitly set validity
                                if (effectiveStartDate.before(alert.effectiveStartDate)) {
                                    effectiveStartDate = alert.effectiveStartDate;
                                }
                                if (effectiveEndDate.after(alert.effectiveEndDate)) {
                                    effectiveEndDate = alert.effectiveEndDate;
                                }

                                if (effectiveStartDate.after(effectiveEndDate) || effectiveStartDate.equals(effectiveEndDate)) {
                                    //Ignore this as situation is no longer valid
                                    continue;
                                }
                            }
                            if (effectiveEndDate == null) {
                                effectiveEndDate = new Date(Long.MAX_VALUE);
                            }
                            if (!affectedStops.isEmpty()) {
                                for (AffectedStopPointStructure affectedStop : affectedStops) {
                                    FeedScopedId stop = siriFuzzyTripMatcher.getStop(affectedStop.getStopPointRef().getValue());
                                    if (stop == null) {
                                        continue;
                                    }
                                    // Creating unique, deterministic id for the alert
                                    String id = paddedSituationNumber + tripId.getId() + "-" + new ServiceDate(effectiveStartDate).asCompactString() + "-" + stop.getId();
                                    if (expireSituation) {
                                        idsToExpire.add(id);
                                    } else {
                                        //  A tripId for a given date may be reused for other dates not affected by this alert.
                                        List<TimePeriod> timePeriodList = new ArrayList<>();
                                        timePeriodList.add(new TimePeriod(effectiveStartDate.getTime() / 1000, effectiveEndDate.getTime() / 1000));

                                        AlertPatch alertPatch = createAlertPatch(id, timePeriodList, affectedStop.getStopConditions());
                                        alertPatch.setTrip(tripId);
                                        alertPatch.setStop(stop);

                                        Alert vehicleJourneyAlert = cloneAlertTexts(alert);
                                        vehicleJourneyAlert.effectiveStartDate = effectiveStartDate;
                                        vehicleJourneyAlert.effectiveEndDate = effectiveEndDate;

                                        alertPatch.setAlert(vehicleJourneyAlert);

                                        patches.add(alertPatch);
                                    }
                                }
                            } else {
                                String id = paddedSituationNumber + tripId.getId();
                                if (expireSituation) {
                                    idsToExpire.add(id);
                                } else {
                                    //  A tripId for a given date may be reused for other dates not affected by this alert.
                                    List<TimePeriod> timePeriodList = new ArrayList<>();
                                    timePeriodList.add(new TimePeriod(effectiveStartDate.getTime() / 1000, effectiveEndDate.getTime() / 1000));

                                    AlertPatch alertPatch = createAlertPatch(id, timePeriodList, null);
                                    alertPatch.setTrip(tripId);

                                    Alert vehicleJourneyAlert = cloneAlertTexts(alert);
                                    vehicleJourneyAlert.effectiveStartDate = effectiveStartDate;
                                    vehicleJourneyAlert.effectiveEndDate = effectiveEndDate;

                                    alertPatch.setAlert(vehicleJourneyAlert);

                                    patches.add(alertPatch);
                                }
                            }
                        }
                    }
                }

                final FramedVehicleJourneyRefStructure framedVehicleJourneyRef = affectedVehicleJourney.getFramedVehicleJourneyRef();
                if (framedVehicleJourneyRef != null) {
                    final DataFrameRefStructure dataFrameRef = framedVehicleJourneyRef.getDataFrameRef();
                    final String datedVehicleJourneyRef = framedVehicleJourneyRef.getDatedVehicleJourneyRef();

                    FeedScopedId tripId = siriFuzzyTripMatcher.getTripId(datedVehicleJourneyRef);
                    ServiceDate serviceDate = null;
                    if (dataFrameRef != null && dataFrameRef.getValue() != null) {
                        try {
                            serviceDate = ServiceDate.parseString(dataFrameRef.getValue().replace("-", ""));
                        } catch (ParseException e) {
                            // Ignore field when not valid
                        }
                    }
                    Date effectiveStartDate;
                    Date effectiveEndDate;
                    if (serviceDate != null) {
                        // Calculate exact from-validity based on actual departure
                        int tripDepartureTime = siriFuzzyTripMatcher.getTripDepartureTime(tripId);
                        int tripArrivalTime = siriFuzzyTripMatcher.getTripArrivalTime(tripId);

                        // ServiceJourneyId does NOT match - calculate validity based on serviceDate (calculated from originalAimedDepartureTime)
                        effectiveStartDate = new Date(serviceDate.getAsDate().getTime() + tripDepartureTime * 1000);

                        // Appending 6 hours to end-validity in case of delays.
                        effectiveEndDate = new Date(effectiveStartDate.getTime() + (tripArrivalTime - tripDepartureTime + 6 * 3600) * 1000);

                        // Verify that calculated validity does not exceed explicitly set validity
                        if (effectiveStartDate.before(alert.effectiveStartDate)) {
                            effectiveStartDate = alert.effectiveStartDate;
                        }
                        if (effectiveEndDate.after(alert.effectiveEndDate)) {
                            effectiveEndDate = alert.effectiveEndDate;
                        }

                        if (effectiveStartDate.after(effectiveEndDate) || effectiveStartDate.equals(effectiveEndDate)) {
                            //Ignore this as situation is no longer valid
                            continue;
                        }
                    } else {
                        effectiveStartDate = alert.effectiveStartDate;
                        effectiveEndDate = alert.effectiveEndDate;
                    }

                    if (effectiveEndDate == null) {
                        effectiveEndDate = new Date(Long.MAX_VALUE);
                    }

                    if (!affectedStops.isEmpty()) {
                        for (AffectedStopPointStructure affectedStop : affectedStops) {
                            FeedScopedId stop = siriFuzzyTripMatcher.getStop(affectedStop.getStopPointRef().getValue());
                            if (stop == null) {
                                continue;
                            }
                            // Creating unique, deterministic id for the alert
                            String id = paddedSituationNumber + tripId.getId() + "-" + new ServiceDate(effectiveStartDate).asCompactString() + "-" + stop.getId();
                            if (expireSituation) {
                                idsToExpire.add(id);
                            } else {
                                //  A tripId for a given date may be reused for other dates not affected by this alert.
                                List<TimePeriod> timePeriodList = new ArrayList<>();
                                timePeriodList.add(new TimePeriod(effectiveStartDate.getTime() / 1000, effectiveEndDate.getTime() / 1000));

                                AlertPatch alertPatch = createAlertPatch(id, timePeriodList, affectedStop.getStopConditions());
                                alertPatch.setTrip(tripId);
                                alertPatch.setStop(stop);

                                Alert vehicleJourneyAlert = cloneAlertTexts(alert);
                                vehicleJourneyAlert.effectiveStartDate = effectiveStartDate;
                                vehicleJourneyAlert.effectiveEndDate = effectiveEndDate;

                                alertPatch.setAlert(vehicleJourneyAlert);

                                patches.add(alertPatch);
                            }
                        }
                    } else {
                        String id = paddedSituationNumber + tripId.getId();
                        if (expireSituation) {
                            idsToExpire.add(id);
                        } else {
                            //  A tripId for a given date may be reused for other dates not affected by this alert.
                            List<TimePeriod> timePeriodList = new ArrayList<>();
                            timePeriodList.add(new TimePeriod(effectiveStartDate.getTime() / 1000, effectiveEndDate.getTime() / 1000));

                            AlertPatch alertPatch = createAlertPatch(id, timePeriodList, null);

                            alertPatch.setTrip(tripId);

                            Alert vehicleJourneyAlert = cloneAlertTexts(alert);
                            vehicleJourneyAlert.effectiveStartDate = effectiveStartDate;
                            vehicleJourneyAlert.effectiveEndDate = effectiveEndDate;

                            alertPatch.setAlert(vehicleJourneyAlert);

                            patches.add(alertPatch);
                        }
                    }

                }
            }
        }
        return Pair.of(idsToExpire, patches);
    }

    private List<AffectedStopPointStructure> getAffectedStopPoints(AffectedVehicleJourneyStructure affectedVehicleJourney) {
        List<AffectedStopPointStructure> affectedStops = new ArrayList<>();

        List<AffectedRouteStructure> routes = affectedVehicleJourney.getRoutes();
        // Resolve AffectedStop-ids
        if (routes != null) {
            for (AffectedRouteStructure route : routes) {
                if (route.getStopPoints() != null) {
                    List<Serializable> stopPointsList = route.getStopPoints().getAffectedStopPointsAndLinkProjectionToNextStopPoints();
                    for (Serializable serializable : stopPointsList) {
                        if (serializable instanceof AffectedStopPointStructure) {
                            AffectedStopPointStructure stopPointStructure = (AffectedStopPointStructure) serializable;
                            affectedStops.add(stopPointStructure);
                        }
                    }
                }
            }
        }
        return affectedStops;
    }

    private Pair<Set<String>, Set<AlertPatch>> createNetworkAlerts(String paddedSituationNumber, boolean expireSituation, ArrayList<TimePeriod> periods, Alert alert, AffectsScopeStructure.Networks networks) {
        Set<String> idsToExpire = new HashSet<>();
        Set<AlertPatch> patches = new HashSet<>();
        if (networks != null && !isListNullOrEmpty(networks.getAffectedNetworks())) {

            for (AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork : networks.getAffectedNetworks()) {

                List<AffectedLineStructure> affectedLines = affectedNetwork.getAffectedLines();
                if (affectedLines != null && !isListNullOrEmpty(affectedLines)) {
                    for (AffectedLineStructure line : affectedLines) {

                        LineRef lineRef = line.getLineRef();

                        if (lineRef == null || lineRef.getValue() == null) {
                            continue;
                        }


                        AffectedLineStructure.Routes routes = line.getRoutes();

                        List<AffectedStopPointStructure> affectedStops = getAffectedStopPointStructures(routes);

                        Set<Route> affectedRoutes = siriFuzzyTripMatcher.getRoutes(lineRef.getValue());

                        for (Route route : affectedRoutes) {
                            if (!affectedStops.isEmpty()) {
                                for (AffectedStopPointStructure affectedStop : affectedStops) {
                                    FeedScopedId stop = siriFuzzyTripMatcher.getStop(affectedStop.getStopPointRef().getValue());
                                    if (stop == null) {
                                        continue;
                                    }
                                    String id = paddedSituationNumber + route.getId() + "-" + stop.getId();
                                    if (expireSituation) {
                                        idsToExpire.add(id);
                                    } else {
                                        AlertPatch alertPatch = createAlertPatch(id, periods, affectedStop.getStopConditions());
                                        alertPatch.setRoute(route.getId());
                                        alertPatch.setStop(stop);

                                        Alert vehicleJourneyAlert = cloneAlertTexts(alert);

                                        alertPatch.setAlert(vehicleJourneyAlert);

                                        patches.add(alertPatch);
                                    }
                                }
                            } else {
                                String id = paddedSituationNumber + route.getId();
                                if (expireSituation) {
                                    idsToExpire.add(id);
                                } else {
                                    AlertPatch alertPatch = createAlertPatch(id, periods, null);
                                    alertPatch.setRoute(route.getId());
                                    patches.add(alertPatch);
                                }
                            }
                        }
                    }
                } else {
                    NetworkRefStructure networkRef = affectedNetwork.getNetworkRef();
                    if (networkRef == null || networkRef.getValue() == null) {
                        continue;
                    }
                    String networkId = networkRef.getValue();

                    String id = paddedSituationNumber + networkId;
                    if (expireSituation) {
                        idsToExpire.add(id);
                    } else {
                        AlertPatch alertPatch = createAlertPatch(id, periods, null);
                        // TODO: Add alert for entire network/Operator
                        patches.add(alertPatch);
                    }
                }
            }
        }
        return Pair.of(idsToExpire, patches);
    }

    private Pair<Set<String>, Set<AlertPatch>> createStopAlerts(String paddedSituationNumber, boolean expireSituation, ArrayList<TimePeriod> periods, Set<Route> stopRoutes, AffectsScopeStructure.StopPoints stopPoints, AffectsScopeStructure.StopPlaces stopPlaces) {

        Set<String> idsToExpire = new HashSet<>();
        Set<AlertPatch> patches = new HashSet<>();

        HashMap<String, FeedScopedId> situationStopIdMap = new HashMap<>();
        HashMap<String, List<RoutePointTypeEnumeration>> situationStopConditionsMap = new HashMap<>();

        if (stopPlaces != null && !isListNullOrEmpty(stopPlaces.getAffectedStopPlaces())) {

            for (AffectedStopPlaceStructure stopPoint : stopPlaces.getAffectedStopPlaces()) {
                StopPlaceRef stopPlace = stopPoint.getStopPlaceRef();
                if (stopPlace == null || stopPlace.getValue() == null) {
                    continue;
                }

                FeedScopedId stopId = siriFuzzyTripMatcher.getStop(stopPlace.getValue());

                String id = paddedSituationNumber + stopPlace.getValue();
                situationStopIdMap.put(id, stopId);
            }
        } else if (stopPoints != null && !isListNullOrEmpty(stopPoints.getAffectedStopPoints())) {

            for (AffectedStopPointStructure stopPoint : stopPoints.getAffectedStopPoints()) {
                StopPointRef stopPointRef = stopPoint.getStopPointRef();
                if (stopPointRef == null || stopPointRef.getValue() == null) {
                    continue;
                }

                FeedScopedId stopId = siriFuzzyTripMatcher.getStop(stopPointRef.getValue());

                String id = paddedSituationNumber + stopPointRef.getValue();
                situationStopIdMap.put(id, stopId);

                situationStopConditionsMap.put(id, stopPoint.getStopConditions());
            }
        }


        if (!situationStopIdMap.isEmpty()) {
            for (Map.Entry<String, FeedScopedId> FeedScopedIdEntry : situationStopIdMap.entrySet()) {

                String id = FeedScopedIdEntry.getKey();
                FeedScopedId stopId = FeedScopedIdEntry.getValue();

                List<RoutePointTypeEnumeration> stopConditions = null;
                if (situationStopConditionsMap.containsKey(id)) {
                    stopConditions = situationStopConditionsMap.get(id);
                }

                if (expireSituation) {
                    idsToExpire.add(id);
                } else {
                    if (stopId != null) {

                        if (stopRoutes.isEmpty()) {
                            AlertPatch stopOnlyAlertPatch = createAlertPatch(id, periods, stopConditions);
                            stopOnlyAlertPatch.setStop(stopId);
                            patches.add(stopOnlyAlertPatch);
                        } else {
                            //Adding combination of stop & route
                            for (Route route : stopRoutes) {
                                id = paddedSituationNumber + stopId.getId() + "-" + route.getId().getId();

                                AlertPatch alertPatch = createAlertPatch(id, periods, stopConditions);
                                alertPatch.setStop(stopId);
                                alertPatch.setRoute(route.getId());
                                patches.add(alertPatch);
                            }
                        }
                    }
                }
            }
        }
        return Pair.of(idsToExpire, patches);
    }

    private Pair<Set<String>, Set<AlertPatch>> createOperatorAlerts(String paddedSituationNumber, boolean expireSituation, ArrayList<TimePeriod> periods, AffectsScopeStructure.Operators operators) {
        Set<AlertPatch> operatorPatches = new HashSet<>();
        Set<String> operatorIdsToExpire = new HashSet<>();
        if (operators != null && !isListNullOrEmpty(operators.getAffectedOperators())) {
            for (AffectedOperatorStructure affectedOperator : operators.getAffectedOperators()) {

                OperatorRefStructure operatorRef = affectedOperator.getOperatorRef();
                if (operatorRef == null || operatorRef.getValue() == null) {
                    continue;
                }

                // SIRI Operators are mapped to OTP Agency, this i probably wrong - but
                // I leave this for now.
                String agencyId = operatorRef.getValue();

                String id = paddedSituationNumber + agencyId;
                if (expireSituation) {
                    operatorIdsToExpire.add(id);
                } else {
                    AlertPatch alertPatch = createAlertPatch(id, periods, null);
                    alertPatch.setFeedId(agencyId);
                    operatorPatches.add(alertPatch);
                }
            }
        }
        return  Pair.of(operatorIdsToExpire, operatorPatches);
    }

    private List<AffectedStopPointStructure> getAffectedStopPointStructures(AffectedLineStructure.Routes routes) {
        List<AffectedStopPointStructure> affectedStopsList = new ArrayList<>();
        if (routes != null) {
            for (AffectedRouteStructure route : routes.getAffectedRoutes()) {
                if (route.getStopPoints() != null) {
                    List<Serializable> stopPointsList = route.getStopPoints().getAffectedStopPointsAndLinkProjectionToNextStopPoints();
                    for (Serializable serializable : stopPointsList) {
                        if (serializable instanceof AffectedStopPointStructure) {
                            AffectedStopPointStructure stopPointStructure = (AffectedStopPointStructure) serializable;
                            affectedStopsList.add(stopPointStructure);
                        }
                    }
                }
            }
        }
        return affectedStopsList;
    }
    /*
     * Creates alert from PtSituation with all textual content
     */
    private Alert createAlertWithTexts(PtSituationElement situation) {
        Alert alert = new Alert();

        alert.alertDescriptionText = getTranslatedString(situation.getDescriptions());
        alert.alertDetailText = getTranslatedString(situation.getDetails());
        alert.alertAdviceText = getTranslatedString(situation.getAdvices());
        alert.alertHeaderText = getTranslatedString(situation.getSummaries());
        alert.alertUrl = getInfoLinkAsString(situation.getInfoLinks());
        alert.setAlertUrlList(getInfoLinks(situation.getInfoLinks()));

        return alert;
    }

    private AlertPatch createAlertPatch(String id, List<TimePeriod> periods, List<RoutePointTypeEnumeration> stopConditions) {
        AlertPatch alertPatch = new AlertPatch();
        alertPatch.setTimePeriods(periods);
        alertPatch.setId(id);
        alertPatch.getStopConditions().addAll(resolveStopConditions(stopConditions));
        return alertPatch;
    }

    private Set<StopCondition> resolveStopConditions(List<RoutePointTypeEnumeration> stopConditions) {
        Set<StopCondition> alertStopConditions = new HashSet<>();
        if (stopConditions != null) {
            for (RoutePointTypeEnumeration stopCondition : stopConditions) {
                switch (stopCondition) {
                    case EXCEPTIONAL_STOP:
                        alertStopConditions.add(StopCondition.EXCEPTIONAL_STOP);
                        break;
                    case DESTINATION:
                        alertStopConditions.add(StopCondition.DESTINATION);
                        break;
                    case NOT_STOPPING:
                        alertStopConditions.add(StopCondition.NOT_STOPPING);
                        break;
                    case REQUEST_STOP:
                        alertStopConditions.add(StopCondition.REQUEST_STOP);
                        break;
                    case START_POINT:
                        alertStopConditions.add(StopCondition.START_POINT);
                        break;
                    default:
                        // Ignore
                }
            }
        }
        if (alertStopConditions.isEmpty()) {
            //No StopConditions are set - set default
            alertStopConditions.add(StopCondition.START_POINT);
            alertStopConditions.add(StopCondition.DESTINATION);

        }
        return alertStopConditions;
    }

    /*
     * Clones alert with all textual content
     */
    private Alert cloneAlertTexts(Alert alert) {
        Alert vehicleJourneyAlert = new Alert();
        vehicleJourneyAlert.alertHeaderText = alert.alertHeaderText;
        vehicleJourneyAlert.alertDescriptionText = alert.alertDescriptionText;
        vehicleJourneyAlert.alertDetailText = alert.alertDetailText;
        vehicleJourneyAlert.alertAdviceText = alert.alertAdviceText;
        vehicleJourneyAlert.alertUrl = alert.alertUrl;
        vehicleJourneyAlert.setAlertUrlList(alert.getAlertUrlList());

        return vehicleJourneyAlert;
    }

    /*
     * Returns first InfoLink-uri as a String
     */
    private I18NString getInfoLinkAsString(PtSituationElement.InfoLinks infoLinks) {
        if (infoLinks != null) {
            if (!isListNullOrEmpty(infoLinks.getInfoLinks())) {
                InfoLinkStructure infoLinkStructure = infoLinks.getInfoLinks().get(0);
                if (infoLinkStructure != null && infoLinkStructure.getUri() != null) {
                    return new NonLocalizedString(infoLinkStructure.getUri());
                }
            }
        }
        return null;
    }

    /*
     * Returns all InfoLinks
     */
    private List<AlertUrl> getInfoLinks(PtSituationElement.InfoLinks infoLinks) {
        List<AlertUrl> alertUrls = new ArrayList<>();
        if (infoLinks != null) {
            if (!isListNullOrEmpty(infoLinks.getInfoLinks())) {
                for (InfoLinkStructure infoLink : infoLinks.getInfoLinks()) {
                    AlertUrl alertUrl = new AlertUrl();

                    List<NaturalLanguageStringStructure> labels = infoLink.getLabels();
                    if (labels != null && !labels.isEmpty()) {
                        NaturalLanguageStringStructure label = labels.get(0);
                        alertUrl.label = label.getValue();
                    }

                    alertUrl.uri = infoLink.getUri();
                    alertUrls.add(alertUrl);
                }
            }
        }
        return alertUrls;
    }

    private void updateStopConditions(AlertPatch alertPatch, List<RoutePointTypeEnumeration> stopConditions) {
        Set<StopCondition> alertStopConditions = new HashSet<>();
        if (stopConditions != null) {
            for (RoutePointTypeEnumeration stopCondition : stopConditions) {
                switch (stopCondition) {
                    case EXCEPTIONAL_STOP:
                        alertStopConditions.add(StopCondition.EXCEPTIONAL_STOP);
                        break;
                    case DESTINATION:
                        alertStopConditions.add(StopCondition.DESTINATION);
                        break;
                    case NOT_STOPPING:
                        alertStopConditions.add(StopCondition.NOT_STOPPING);
                        break;
                    case REQUEST_STOP:
                        alertStopConditions.add(StopCondition.REQUEST_STOP);
                        break;
                    case START_POINT:
                        alertStopConditions.add(StopCondition.START_POINT);
                        break;
                }
            }
        }
        if (alertStopConditions.isEmpty()) {
            //No StopConditions are set - set default
            alertStopConditions.add(StopCondition.START_POINT);
            alertStopConditions.add(StopCondition.DESTINATION);

        }
        alertPatch.getStopConditions().addAll(alertStopConditions);
    }

    private boolean isListNullOrEmpty(List list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * convert a SIRI DefaultedTextStructure to a OTP TranslatedString
     *
     * @return A TranslatedString containing the same information as the input
     * @param input
     */
    private I18NString getTranslatedString(List<DefaultedTextStructure> input) {
        Map<String, String> translations = new HashMap<>();
        if (input != null && input.size() > 0) {
            for (DefaultedTextStructure textStructure : input) {
                String language = "";
                String value = "";
                if (textStructure.getLang() != null) {
                    language = textStructure.getLang();
                }
                if (textStructure.getValue() != null) {
                    value = textStructure.getValue();
                }
                translations.put(language, value);
            }
        } else {
            translations.put("", "");
        }

        return translations.isEmpty() ? null : TranslatedString.getI18NString(translations);
    }

    public void setAlertPatchService(AlertPatchService alertPatchService) {
        this.alertPatchService = alertPatchService;
    }

    public long getEarlyStart() {
        return earlyStart;
    }

    public void setEarlyStart(long earlyStart) {
        this.earlyStart = earlyStart;
    }

    public void setSiriFuzzyTripMatcher(SiriFuzzyTripMatcher siriFuzzyTripMatcher) {
        this.siriFuzzyTripMatcher = siriFuzzyTripMatcher;
    }
}
