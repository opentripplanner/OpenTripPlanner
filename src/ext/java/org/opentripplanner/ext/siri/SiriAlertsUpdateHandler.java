package org.opentripplanner.ext.siri;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This updater applies the equivalent of GTFS Alerts, but from SIRI Situation Exchange feeds.
 * NOTE this cannot handle situations where there are multiple feeds with different IDs (for now it may only work in
 * single-feed regions).
 */
public class SiriAlertsUpdateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SiriAlertsUpdateHandler.class);

    private TransitAlertService transitAlertService;

    /** How long before the posted start of an event it should be displayed to users */
    private long earlyStart;

    private SiriFuzzyTripMatcher siriFuzzyTripMatcher;

    private String feedId;

    private final Set<TransitAlert> alerts = new HashSet<>();

    public SiriAlertsUpdateHandler(String feedId) {
        this.feedId = feedId;
    }

    public void update(ServiceDelivery delivery) {
        for (SituationExchangeDeliveryStructure sxDelivery : delivery.getSituationExchangeDeliveries()) {
            SituationExchangeDeliveryStructure.Situations situations = sxDelivery.getSituations();
            if (situations != null) {
                long t1 = System.currentTimeMillis();
                Set<String> idsToExpire = new HashSet<>();

                for (PtSituationElement sxElement : situations.getPtSituationElements()) {
                    boolean expireSituation = (sxElement.getProgress() != null &&
                        sxElement.getProgress().equals(WorkflowStatusEnumeration.CLOSED));

                    String situationNumber;
                    if (sxElement.getSituationNumber() != null) {
                        situationNumber = sxElement.getSituationNumber().getValue();
                    } else {
                        situationNumber = null;
                    }

                    if (expireSituation) {
                        alerts.removeIf(transitAlert -> transitAlert.getId().equals(situationNumber));
                    } else {
                        TransitAlert alert = handleAlert(sxElement);
                        if (alert != null) {
                            alerts.removeIf(transitAlert -> transitAlert.getId().equals(situationNumber));
                            alerts.add(alert);
                            if (alert.getEntities().isEmpty()) {
                                LOG.info("No match found for Alert - ignoring situation with situationNumber {}", situationNumber);
                            }
                        }
                    }
                }

                transitAlertService.setAlerts(alerts);

                LOG.info("Added {} alerts, expired {} alerts based on {} situations, current alert-count: {}, elapsed time {}ms", alerts.size(), idsToExpire.size(), situations.getPtSituationElements().size(), transitAlertService
                    .getAllAlerts().size(), (System.currentTimeMillis()-t1));
            }
        }
    }

    private TransitAlert handleAlert(PtSituationElement situation) {

        TransitAlert alert = createAlertWithTexts(situation);

        //ROR-54
        //If situation is closed, it must be allowed - it will remove already existing alerts
        if ((alert.alertHeaderText == null || alert.alertHeaderText.toString().isEmpty()) && (
            alert.alertDescriptionText == null || alert.alertDescriptionText.toString().isEmpty()
        ) && (alert.alertDetailText == null || alert.alertDetailText.toString().isEmpty())) {
            LOG.debug("Empty Alert - ignoring situationNumber: {}", situation.getSituationNumber() != null ? situation.getSituationNumber().getValue():null);
            return null;
        }

        ArrayList<TimePeriod> periods = new ArrayList<>();
        if(situation.getValidityPeriods().size() > 0) {
            for (HalfOpenTimestampOutputRangeStructure activePeriod : situation.getValidityPeriods()) {

                final long realStart = activePeriod.getStartTime() != null ? activePeriod.getStartTime().toInstant().toEpochMilli() : 0;
                final long start = activePeriod.getStartTime() != null? realStart - earlyStart : 0;

                final long realEnd = activePeriod.getEndTime() != null ? activePeriod.getEndTime().toInstant().toEpochMilli() : 0;
                final long end = activePeriod.getEndTime() != null? realEnd  : 0;
                periods.add(new TimePeriod(start/1000, end/1000));
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, Long.MAX_VALUE));
        }

        alert.setTimePeriods(periods);

        AffectsScopeStructure affectsStructure = situation.getAffects();

        if (affectsStructure != null) {

            AffectsScopeStructure.Operators operators = affectsStructure.getOperators();

            if (operators != null && !isListNullOrEmpty(operators.getAffectedOperators())) {
                for (AffectedOperatorStructure affectedOperator : operators.getAffectedOperators()) {

                    OperatorRefStructure operatorRef = affectedOperator.getOperatorRef();
                    if (operatorRef == null || operatorRef.getValue() == null) {
                        continue;
                    }

                    // SIRI Operators are mapped to OTP Agency, this i probably wrong - but
                    // I leave this for now.
                    String agencyId = operatorRef.getValue();

                    alert.addEntity(new EntitySelector.Agency(new FeedScopedId(feedId, agencyId)));
                }
            }

            AffectsScopeStructure.Networks networks = affectsStructure.getNetworks();
            Set<Route> stopRoutes = new HashSet<>();
            if (networks != null) {
                for (AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork : networks.getAffectedNetworks()) {
                    List<AffectedLineStructure> affectedLines = affectedNetwork.getAffectedLines();
                    if (affectedLines != null && !isListNullOrEmpty(affectedLines)) {
                        for (AffectedLineStructure line : affectedLines) {

                            LineRef lineRef = line.getLineRef();

                            if (lineRef == null || lineRef.getValue() == null) {
                                continue;
                            }

                            stopRoutes.addAll(siriFuzzyTripMatcher.getRoutes(lineRef.getValue()));
                        }
                    }
                }
            }

            AffectsScopeStructure.StopPoints stopPoints = affectsStructure.getStopPoints();
            AffectsScopeStructure.StopPlaces stopPlaces = affectsStructure.getStopPlaces();

            if (stopPoints != null && !isListNullOrEmpty(stopPoints.getAffectedStopPoints())) {

                for (AffectedStopPointStructure stopPoint : stopPoints.getAffectedStopPoints()) {
                    StopPointRef stopPointRef = stopPoint.getStopPointRef();
                    if (stopPointRef == null || stopPointRef.getValue() == null) {
                        continue;
                    }

                    FeedScopedId stopId = siriFuzzyTripMatcher.getStop(stopPointRef.getValue());

                    if (stopId != null) {
                        if (stopRoutes.isEmpty()) {
                            alert.addEntity(new EntitySelector.Stop(stopId));
                            // TODO: is this correct? Should the stop conditions be in the entity selector?
                            updateStopConditions(alert, stopPoint.getStopConditions());

                        } else {
                            //Adding combination of stop & route
                            for (Route route : stopRoutes) {
                                alert.addEntity(new EntitySelector.StopAndRoute(stopId, route.getId()));
                                // TODO: is this correct? Should the stop conditions be in the entity selector?
                                updateStopConditions(alert, stopPoint.getStopConditions());

                            }
                        }
                    }
                }
            } else if (stopPlaces != null && !isListNullOrEmpty(stopPlaces.getAffectedStopPlaces())) {

                for (AffectedStopPlaceStructure stopPoint : stopPlaces.getAffectedStopPlaces()) {
                    StopPlaceRef stopPlace = stopPoint.getStopPlaceRef();
                    if (stopPlace == null || stopPlace.getValue() == null) {
                        continue;
                    }

                    FeedScopedId stopId = siriFuzzyTripMatcher.getStop(stopPlace.getValue());

                    if (stopId != null) {

                        if (stopRoutes.isEmpty()) {
                            alert.addEntity(new EntitySelector.Stop(stopId));
                        } else {
                            //Adding combination of stop & route
                            for (Route route : stopRoutes) {
                                alert.addEntity(new EntitySelector.StopAndRoute(stopId, route.getId()));
                            }
                        }
                    }
                }
            } else if (networks != null && !isListNullOrEmpty(networks.getAffectedNetworks())) {

                for (AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork : networks.getAffectedNetworks()) {
                    List<AffectedLineStructure> affectedLines = affectedNetwork.getAffectedLines();
                    if (affectedLines != null && !isListNullOrEmpty(affectedLines)) {
                        for (AffectedLineStructure line : affectedLines) {

                            LineRef lineRef = line.getLineRef();

                            if (lineRef == null || lineRef.getValue() == null) {
                                continue;
                            }

                            List<AffectedStopPointStructure> affectedStops = new ArrayList<>();

                            AffectedLineStructure.Routes routes = line.getRoutes();

                            // Resolve AffectedStop-ids
                            if (routes != null) {
                                for (AffectedRouteStructure route : routes.getAffectedRoutes()) {
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
                            Set<Route> affectedRoutes = siriFuzzyTripMatcher.getRoutes(lineRef.getValue());

                            for (Route route : affectedRoutes) {
                                if (! affectedStops.isEmpty()) {
                                    for (AffectedStopPointStructure affectedStop : affectedStops) {
                                        FeedScopedId stop = siriFuzzyTripMatcher.getStop(affectedStop.getStopPointRef().getValue());
                                        if (stop == null) {
                                            continue;
                                        }
                                        alert.addEntity(new EntitySelector.StopAndRoute(stop, route.getId()));
                                        // TODO: is this correct? Should the stop conditions be in the entity selector?
                                        updateStopConditions(alert, affectedStop.getStopConditions());
                                    }
                                } else {
                                    alert.addEntity(new EntitySelector.Route(route.getId()));
                                }
                            }
                        }
                    } else {
                        NetworkRefStructure networkRef = affectedNetwork.getNetworkRef();
                        if (networkRef == null || networkRef.getValue() == null) {
                            continue;
                        }
                        String networkId = networkRef.getValue();

                        // TODO: What to do here?
                    }
                }
            }

            AffectsScopeStructure.VehicleJourneys vjs = affectsStructure.getVehicleJourneys();
            if (vjs != null && !isListNullOrEmpty(vjs.getAffectedVehicleJourneies())) {

                for (AffectedVehicleJourneyStructure affectedVehicleJourney : vjs.getAffectedVehicleJourneies()) {

                    String lineRef = null;
                    if (affectedVehicleJourney.getLineRef() != null) {
                        lineRef = affectedVehicleJourney.getLineRef().getValue();
                    }

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

                    List<VehicleJourneyRef> vehicleJourneyReves = affectedVehicleJourney.getVehicleJourneyReves();


                    if (!isListNullOrEmpty(vehicleJourneyReves)) {
                        for (VehicleJourneyRef vehicleJourneyRef : vehicleJourneyReves) {

                            List<FeedScopedId> tripIds = new ArrayList<>();

                            FeedScopedId tripIdFromVehicleJourney = siriFuzzyTripMatcher.getTripId(vehicleJourneyRef.getValue());
                            Date effectiveStartDate;
                            Date effectiveEndDate;

                            // Need to know if validity is set explicitly or calculated based on ServiceDate
                            boolean effectiveValiditySetExplicitly = false;

                            ZonedDateTime originAimedDepartureTime = (affectedVehicleJourney.getOriginAimedDepartureTime() != null ? affectedVehicleJourney.getOriginAimedDepartureTime():ZonedDateTime.now());
                            ServiceDate serviceDate = new ServiceDate(originAimedDepartureTime.getYear(), originAimedDepartureTime.getMonthValue(), originAimedDepartureTime.getDayOfMonth());

                            if (tripIdFromVehicleJourney != null) {

                                tripIds.add(tripIdFromVehicleJourney);

                                // ServiceJourneyId matches - use provided validity
                                effectiveStartDate = alert.getEffectiveStartDate();
                                effectiveEndDate = alert.getEffectiveEndDate();

                                effectiveValiditySetExplicitly = true;

                            } else {
                                // TODO - SIRI: Support submode when fuzzy-searching for trips
                                tripIds = siriFuzzyTripMatcher.getTripIdForTripShortNameServiceDateAndMode(vehicleJourneyRef.getValue(),
                                        serviceDate, TraverseMode.RAIL/*, TransmodelTransportSubmode.RAIL_REPLACEMENT_BUS*/);

                                // ServiceJourneyId does NOT match - calculate validity based on originAimedDepartureTime
                                effectiveStartDate = serviceDate.getAsDate();
                                effectiveEndDate = serviceDate.next().getAsDate();

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
                                    if (effectiveStartDate.before(alert.getEffectiveStartDate())) {
                                        effectiveStartDate = alert.getEffectiveStartDate();
                                    }
                                    if (effectiveEndDate.after(alert.getEffectiveEndDate())) {
                                        effectiveEndDate = alert.getEffectiveEndDate();
                                    }

                                    if (effectiveStartDate.after(effectiveEndDate) | effectiveStartDate.equals(effectiveEndDate)) {
                                        //Ignore this as situation is no longer valid
                                        continue;
                                    }
                                }
                                if (effectiveEndDate == null) {
                                    effectiveEndDate = new Date(Long.MAX_VALUE);
                                }
                                if (! affectedStops.isEmpty()) {
                                    for (AffectedStopPointStructure affectedStop : affectedStops) {
                                        FeedScopedId stop = siriFuzzyTripMatcher.getStop(affectedStop.getStopPointRef().getValue());
                                        if (stop == null) {
                                            continue;
                                        }
                                        // Creating unique, deterministic id for the alert
                                        alert.addEntity(new EntitySelector.StopAndTrip(stop, tripId));

                                        // TODO: is this correct? Should the stop conditions be in the entity selector?
                                        updateStopConditions(alert, affectedStop.getStopConditions());

                                        //  A tripId for a given date may be reused for other dates not affected by this alert.
                                        List<TimePeriod> timePeriodList = new ArrayList<>();
                                        timePeriodList.add(new TimePeriod(effectiveStartDate.getTime()/1000, effectiveEndDate.getTime()/1000));
                                        // TODO: Make it possible to add time periods for trip selectors
                                        // alert.setTimePeriods(timePeriodList);
                                    }
                                } else {
                                    alert.addEntity(new EntitySelector.Trip(tripId));

                                    //  A tripId for a given date may be reused for other dates not affected by this alert.
                                    List<TimePeriod> timePeriodList = new ArrayList<>();
                                    timePeriodList.add(new TimePeriod(effectiveStartDate.getTime()/1000, effectiveEndDate.getTime()/1000));
                                    // TODO: Make it possible to add time periods for trip selectors
                                    // alert.setTimePeriods(timePeriodList);
                                }
                            }
                        }
                    }
                    if (lineRef != null) {

                        Set<Route> affectedRoutes = siriFuzzyTripMatcher.getRoutes(lineRef);
                        for (Route route : affectedRoutes) {
                            alert.addEntity(new EntitySelector.Route(route.getId()));
                        }
                    }
                }
            }
        }

        if (alert.getStopConditions().isEmpty()) {
            updateStopConditions(alert, null);
        }

        alert.alertType = situation.getReportType();

        if (situation.getSeverity() != null) {
            alert.severity = situation.getSeverity().value();
        } else {
            // When severity is not set - use default
            alert.severity = SeverityEnumeration.NORMAL.value();
        }

        if (situation.getParticipantRef() != null) {
            String codespace = situation.getParticipantRef().getValue();
            alert.setFeedId(codespace + ":Authority:" + codespace); //TODO - SIRI: Should probably not assume this codespace -> authority rule
        }

        return alert;
    }


    /*
     * Creates alert from PtSituation with all textual content
     */
    private TransitAlert createAlertWithTexts(PtSituationElement situation) {
        TransitAlert alert = new TransitAlert();

        alert.alertDescriptionText = getTranslatedString(situation.getDescriptions());
        alert.alertDetailText = getTranslatedString(situation.getDetails());
        alert.alertAdviceText = getTranslatedString(situation.getAdvices());
        alert.alertHeaderText = getTranslatedString(situation.getSummaries());
        alert.alertUrl = getInfoLinkAsString(situation.getInfoLinks());
        alert.setAlertUrlList(getInfoLinks(situation.getInfoLinks()));

        return alert;
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

    private void updateStopConditions(TransitAlert alertPatch, List<RoutePointTypeEnumeration> stopConditions) {
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

    public void setTransitAlertService(TransitAlertService transitAlertService) {
        this.transitAlertService = transitAlertService;
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
