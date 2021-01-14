package org.opentripplanner.ext.siri;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.AffectedLineStructure;
import uk.org.siri.siri20.AffectedOperatorStructure;
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
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.NetworkRefStructure;
import uk.org.siri.siri20.OperatorRefStructure;
import uk.org.siri.siri20.PtSituationElement;
import uk.org.siri.siri20.RoutePointTypeEnumeration;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.SeverityEnumeration;
import uk.org.siri.siri20.SituationExchangeDeliveryStructure;
import uk.org.siri.siri20.StopPointRef;
import uk.org.siri.siri20.VehicleJourneyRef;
import uk.org.siri.siri20.WorkflowStatusEnumeration;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

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

    private final String feedId;

    private final Graph graph;

    private final Set<TransitAlert> alerts = new HashSet<>();

    public SiriAlertsUpdateHandler(String feedId, Graph graph) {
        this.feedId = feedId;
        this.graph = graph;
    }

    public void update(ServiceDelivery delivery) {
        for (SituationExchangeDeliveryStructure sxDelivery : delivery.getSituationExchangeDeliveries()) {
            SituationExchangeDeliveryStructure.Situations situations = sxDelivery.getSituations();
            if (situations != null) {
                long t1 = System.currentTimeMillis();
                int addedCounter = 0;
                int expiredCounter = 0;
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
                        expiredCounter++;
                    } else {
                        TransitAlert alert = handleAlert(sxElement);
                        addedCounter++;
                        if (alert != null) {
                            alert.setId(situationNumber);
                            alerts.removeIf(transitAlert -> transitAlert.getId().equals(situationNumber));
                            alerts.add(alert);
                            if (alert.getEntities().isEmpty()) {
                                LOG.info("No match found for Alert - ignoring situation with situationNumber {}", situationNumber);
                            }
                        }
                    }
                }

                transitAlertService.setAlerts(alerts);

                LOG.info("Added {} alerts, expired {} alerts based on {} situations, current alert-count: {}, elapsed time {}ms",
                        addedCounter, expiredCounter, situations.getPtSituationElements().size(), transitAlertService.getAllAlerts().size(), System.currentTimeMillis()-t1);
            }
        }
    }

    private TransitAlert handleAlert(PtSituationElement situation) {

        TransitAlert alert = createAlertWithTexts(situation);

        if ((alert.alertHeaderText == null      || alert.alertHeaderText.toString().isEmpty()) &&
            (alert.alertDescriptionText == null || alert.alertDescriptionText.toString().isEmpty()) &&
            (alert.alertDetailText == null      || alert.alertDetailText.toString().isEmpty())) {
            LOG.debug("Empty Alert - ignoring situationNumber: {}", situation.getSituationNumber() != null ? situation.getSituationNumber().getValue():null);
            return null;
        }

        ArrayList<TimePeriod> periods = new ArrayList<>();
        if(situation.getValidityPeriods().size() > 0) {
            for (HalfOpenTimestampOutputRangeStructure activePeriod : situation.getValidityPeriods()) {

                final long realStart = activePeriod.getStartTime() != null ? getEpochSecond(activePeriod.getStartTime()) : 0;
                final long start = activePeriod.getStartTime() != null? realStart - earlyStart : 0;

                final long realEnd = activePeriod.getEndTime() != null ? getEpochSecond(activePeriod.getEndTime()) : TimePeriod.OPEN_ENDED;
                final long end = activePeriod.getEndTime() != null? realEnd  : TimePeriod.OPEN_ENDED;

                periods.add(new TimePeriod(start, end));
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, TimePeriod.OPEN_ENDED));
        }

        alert.setTimePeriods(periods);

        if (situation.getPriority() != null) {
            alert.priority = situation.getPriority().intValue();
        }

        AffectsScopeStructure affectsStructure = situation.getAffects();

        if (affectsStructure != null) {

            AffectsScopeStructure.Operators operators = affectsStructure.getOperators();

            if (operators != null && isNotEmpty(operators.getAffectedOperators())) {
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

            AffectsScopeStructure.StopPoints stopPoints = affectsStructure.getStopPoints();
            AffectsScopeStructure.StopPlaces stopPlaces = affectsStructure.getStopPlaces();

            if (stopPoints != null && isNotEmpty(stopPoints.getAffectedStopPoints())) {

                for (AffectedStopPointStructure stopPoint : stopPoints.getAffectedStopPoints()) {
                    StopPointRef stopPointRef = stopPoint.getStopPointRef();
                    if (stopPointRef == null || stopPointRef.getValue() == null) {
                        continue;
                    }

                    FeedScopedId stopId = siriFuzzyTripMatcher.getStop(stopPointRef.getValue());

                    if (stopId == null) {
                        stopId = new FeedScopedId(feedId, stopPointRef.getValue());
                    }

                    alert.addEntity(new EntitySelector.Stop(stopId));
                    // TODO: is this correct? Should the stop conditions be in the entity selector?
                    updateStopConditions(alert, stopPoint.getStopConditions());
                }
            } else if (stopPlaces != null && isNotEmpty(stopPlaces.getAffectedStopPlaces())) {

                for (AffectedStopPlaceStructure stopPoint : stopPlaces.getAffectedStopPlaces()) {
                    StopPlaceRef stopPlace = stopPoint.getStopPlaceRef();
                    if (stopPlace == null || stopPlace.getValue() == null) {
                        continue;
                    }

                    FeedScopedId stopId = siriFuzzyTripMatcher.getStop(stopPlace.getValue());

                    if (stopId == null) {
                        stopId = new FeedScopedId(feedId, stopPlace.getValue());
                    }

                    alert.addEntity(new EntitySelector.Stop(stopId));
                }
            }

            AffectsScopeStructure.Networks networks = affectsStructure.getNetworks();
            if (networks != null && isNotEmpty(networks.getAffectedNetworks())) {

                for (AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork : networks.getAffectedNetworks()) {
                    List<AffectedLineStructure> affectedLines = affectedNetwork.getAffectedLines();
                    if (isNotEmpty(affectedLines)) {
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
                            FeedScopedId affectedRoute = new FeedScopedId(feedId, lineRef.getValue());

                            if (! affectedStops.isEmpty()) {
                                for (AffectedStopPointStructure affectedStop : affectedStops) {
                                    FeedScopedId stop = siriFuzzyTripMatcher.getStop(affectedStop.getStopPointRef().getValue());
                                    if (stop == null) {
                                        stop = new FeedScopedId(feedId, affectedStop.getStopPointRef().getValue());
                                    }
                                    alert.addEntity(new EntitySelector.StopAndRoute(stop, affectedRoute));
                                    // TODO: is this correct? Should the stop conditions be in the entity selector?
                                    updateStopConditions(alert, affectedStop.getStopConditions());
                                }
                            } else {
                                alert.addEntity(new EntitySelector.Route(affectedRoute));
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
            if (vjs != null && isNotEmpty(vjs.getAffectedVehicleJourneies())) {

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

                    if (isNotEmpty(vehicleJourneyReves)) {
                        for (VehicleJourneyRef vehicleJourneyRef : vehicleJourneyReves) {

                            List<FeedScopedId> tripIds = new ArrayList<>();

                            FeedScopedId tripIdFromVehicleJourney = siriFuzzyTripMatcher.getTripId(vehicleJourneyRef.getValue());

                            // Need to know if validity is set explicitly or calculated based on ServiceDate
                            boolean effectiveValiditySetExplicitly = false;

                            ZonedDateTime originAimedDepartureTime = (affectedVehicleJourney.getOriginAimedDepartureTime() != null ? affectedVehicleJourney.getOriginAimedDepartureTime():ZonedDateTime.now());
                            ZonedDateTime startOfService = DateMapper.asStartOfService(originAimedDepartureTime);

                            ServiceDate serviceDate = new ServiceDate(startOfService.getYear(), startOfService.getMonthValue(), startOfService.getDayOfMonth());

                            if (tripIdFromVehicleJourney != null) {

                                tripIds.add(tripIdFromVehicleJourney);

                                effectiveValiditySetExplicitly = true;

                            } else {

//                                Commented out for now
//
//                                // TODO - SIRI: Support submode when fuzzy-searching for trips
//                                tripIds = siriFuzzyTripMatcher.getTripIdForTripShortNameServiceDateAndMode(vehicleJourneyRef.getValue(),
//                                        serviceDate, TraverseMode.RAIL/*, TransmodelTransportSubmode.RAIL_REPLACEMENT_BUS*/);
                            }

                            for (FeedScopedId tripId : tripIds) {

                                if (!effectiveValiditySetExplicitly) {
                                    // Effective validity is set based on ServiceDate - need to calculate correct validity based on departuretimes

                                    final TimePeriod timePeriod = calculateTimePeriodForTrip(alert,
                                        tripId,
                                        serviceDate,
                                        startOfService, 6 * 3600
                                    );

                                    // TODO: Make it possible to add time periods for trip selectors
                                    alert.setTimePeriods(Arrays.asList(timePeriod));
                                }

                                if (! affectedStops.isEmpty()) {
                                    for (AffectedStopPointStructure affectedStop : affectedStops) {
                                        FeedScopedId stop = siriFuzzyTripMatcher.getStop(affectedStop.getStopPointRef().getValue());
                                        if (stop == null) {
                                            stop = new FeedScopedId(feedId, affectedStop.getStopPointRef().getValue());
                                        }
                                        // Creating unique, deterministic id for the alert
                                        alert.addEntity(new EntitySelector.StopAndTrip(stop, tripId));

                                        // TODO: is this correct? Should the stop conditions be in the entity selector?
                                        updateStopConditions(alert, affectedStop.getStopConditions());

                                    }
                                } else {
                                    alert.addEntity(new EntitySelector.Trip(tripId));

                                }
                            }
                        }
                    }

                    final FramedVehicleJourneyRefStructure framedVehicleJourneyRef = affectedVehicleJourney.getFramedVehicleJourneyRef();
                    if (framedVehicleJourneyRef != null) {
                        final DataFrameRefStructure dataFrameRef = framedVehicleJourneyRef.getDataFrameRef();
                        final String datedVehicleJourneyRef = framedVehicleJourneyRef.getDatedVehicleJourneyRef();

                        FeedScopedId tripId = siriFuzzyTripMatcher.getTripId(datedVehicleJourneyRef);

                        if (tripId != null) {
                            ServiceDate serviceDate = null;
                            ZonedDateTime startOfService = null;
                            if (dataFrameRef != null && dataFrameRef.getValue() != null) {
                                startOfService = DateMapper.asStartOfService(LocalDate.parse(
                                    dataFrameRef.getValue()), graph.getTimeZone().toZoneId());

                                serviceDate = new ServiceDate(startOfService.getYear(),
                                    startOfService.getMonthValue(),
                                    startOfService.getDayOfMonth()
                                );

                            }
                            final TimePeriod timePeriod = calculateTimePeriodForTrip(alert,
                                tripId,
                                serviceDate,
                                startOfService,
                                6 * 3600
                            );

                            //  A tripId for a given date may be reused for other dates not affected by this alert.

                            // TODO: Make it possible to add time periods for trip selectors
                            alert.setTimePeriods(Arrays.asList(timePeriod));

                            if (!affectedStops.isEmpty()) {
                                for (AffectedStopPointStructure affectedStop : affectedStops) {
                                    FeedScopedId stop = siriFuzzyTripMatcher.getStop(affectedStop
                                        .getStopPointRef()
                                        .getValue());
                                    if (stop == null) {
                                        stop = new FeedScopedId(feedId,
                                            affectedStop.getStopPointRef().getValue()
                                        );
                                    }

                                    alert.addEntity(new EntitySelector.StopAndTrip(stop, tripId));
                                }
                            }
                            else {
                                alert.addEntity(new EntitySelector.Trip(tripId));
                            }
                        }
                    }

                    if (lineRef != null) {

                        Set<Route> affectedRoutes = siriFuzzyTripMatcher.getRoutes(lineRef);
                        if (affectedRoutes != null) {
                            for (Route route : affectedRoutes) {
                                alert.addEntity(new EntitySelector.Route(route.getId()));
                            }
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

    /**
     * @return A TimePeriod for the provided Trip. The period will start at scheduled
     * departure, and end <code>secondsToAppend</code> seconds after scheduled arrival.
     */
    private TimePeriod calculateTimePeriodForTrip(
        TransitAlert alert, FeedScopedId tripId, ServiceDate serviceDate,
        ZonedDateTime startOfService, int secondsToAppend
    ) {
        Long validFrom;
        Long validTo;
        if (serviceDate != null && startOfService != null) {
            // Calculate exact from-validity based on actual departure
            int tripDepartureTime = siriFuzzyTripMatcher.getTripDepartureTime(tripId);
            int tripArrivalTime = siriFuzzyTripMatcher.getTripArrivalTime(tripId);

            // ServiceJourneyId does NOT match - calculate validity based on serviceDate (calculated from originalAimedDepartureTime)
            validFrom = startOfService.toEpochSecond() + tripDepartureTime;

            // Appending 6 hours to end-validity in case of delays.
            final int tripDuration = tripArrivalTime - tripDepartureTime;
            validTo = validFrom + (tripDuration + secondsToAppend);

        } else {
            validFrom = alert.getEffectiveStartDate().getTime()/1000;
            validTo = alert.getEffectiveEndDate().getTime()/1000;
        }

        if (validTo == null) {
            validTo = Long.MAX_VALUE;
        }

        if (alert.getEffectiveStartDate() != null) {
            // Verify that calculated validity does not exceed explicitly set validity
            if (validFrom < alert.getEffectiveStartDate().getTime() / 1000) {
                validFrom = alert.getEffectiveStartDate().getTime() / 1000;
            }
        }
        if (alert.getEffectiveEndDate() != null) {
            if (validTo > alert.getEffectiveEndDate().getTime() / 1000) {
                validTo = alert.getEffectiveEndDate().getTime() / 1000;
            }
        }

        final TimePeriod timePeriod = new TimePeriod(
            validFrom,
            validTo
        );
        return timePeriod;
    }

    private long getEpochSecond(ZonedDateTime startTime) {
        return startTime.toEpochSecond();
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
            if (isNotEmpty(infoLinks.getInfoLinks())) {
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
            if (isNotEmpty(infoLinks.getInfoLinks())) {
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


    /**
     * @return True if list have at least one element. {@code false} is returned if the given list
     * is empty or {@code null}.
     */
    private boolean isNotEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    /**
     * convert a SIRI DefaultedTextStructure to a OTP TranslatedString
     *
     * @return A TranslatedString containing the same information as the input
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
