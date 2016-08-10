/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.alerts;

import java.util.*;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.SiriFuzzyTripMatcher;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.TranslatedString;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.*;

/**
 * This updater only includes GTFS-Realtime Service Alert feeds.
 * @author novalis
 *
 */
public class AlertsUpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(AlertsUpdateHandler.class);

    private String feedId;

    private Set<String> patchIds = new HashSet<String>();

    private AlertPatchService alertPatchService;

    /** How long before the posted start of an event it should be displayed to users */
    private long earlyStart;

    /** Set only if we should attempt to match the trip_id from other data in TripDescriptor */
    private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;


    private SiriFuzzyTripMatcher siriFuzzyTripMatcher;

    public void update(FeedMessage message) {
        alertPatchService.expire(patchIds);
        patchIds.clear();

        for (FeedEntity entity : message.getEntityList()) {
            if (!entity.hasAlert()) {
                continue;
            }
            GtfsRealtime.Alert alert = entity.getAlert();
            String id = entity.getId();
            handleAlert(id, alert);
        }
    }

    public void update(ServiceDelivery delivery) {
        alertPatchService.expire(patchIds);
        patchIds.clear();

        for (SituationExchangeDeliveryStructure sxDelivery : delivery.getSituationExchangeDeliveries()) {
            SituationExchangeDeliveryStructure.Situations situations = sxDelivery.getSituations();
            for (PtSituationElement sxElement : situations.getPtSituationElements()) {
                String id = sxElement.getSituationNumber().getValue();
                handleAlert(id, sxElement);
            }
        }
    }

    private void handleAlert(String id, PtSituationElement situation) {
        Alert alert = new Alert();

        List<DefaultedTextStructure> descriptions = situation.getDescriptions();
        if (descriptions != null && descriptions.size() > 0) {
            alert.alertDescriptionText = locale -> (descriptions.get(0).getValue());
        }
        List<DefaultedTextStructure> summaries = situation.getSummaries();
        if (summaries != null && summaries.size() > 0) {
            alert.alertHeaderText = locale -> (summaries.get(0).getValue());
        }

        ArrayList<TimePeriod> periods = new ArrayList<TimePeriod>();
        if(situation.getValidityPeriods().size() > 0) {
            long bestStartTime = Long.MAX_VALUE;
            for (PtSituationElement.ValidityPeriod activePeriod : situation.getValidityPeriods()) {

                final long realStart = activePeriod.getStartTime() != null ? activePeriod.getStartTime().toInstant().toEpochMilli() : 0;
                final long start = activePeriod.getStartTime() != null? realStart - earlyStart : 0;
                if (realStart > 0 && realStart < bestStartTime) {
                    bestStartTime = realStart;
                }
                final long end = activePeriod.getEndTime() != null ? activePeriod.getEndTime().toInstant().toEpochMilli() : Long.MAX_VALUE;
                periods.add(new TimePeriod(start, end));
            }
            if (bestStartTime != Long.MAX_VALUE) {
                alert.effectiveStartDate = new Date(bestStartTime * 1000);
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, Long.MAX_VALUE));
        }

        //String patchId = id + "_" + situation.getSituationNumber().getValue();

        String situationNumber = null;
        if (situation.getSituationNumber() != null) {
            situationNumber = situation.getSituationNumber().getValue();
        }

        List<AlertPatch> patches = new ArrayList<>();
        AffectsScopeStructure affectsStructure = situation.getAffects();

        AffectsScopeStructure.Operators operators = affectsStructure.getOperators();

        if (operators != null && !isListNullOrEmpty(operators.getAffectedOperators())) {
            for (AffectedOperatorStructure affectedOperator : operators.getAffectedOperators()) {

                OperatorRefStructure operatorRef = affectedOperator.getOperatorRef();
                if (operatorRef == null || operatorRef.getValue() == null) {
                    continue;
                }

                String operator = operatorRef.getValue();

                AlertPatch alertPatch = new AlertPatch();
                alertPatch.setAgencyId(operator);
                alertPatch.setTimePeriods(periods);
                alertPatch.setAlert(alert);
                alertPatch.setId(situationNumber);
                patches.add(alertPatch);
            }
        }

        AffectsScopeStructure.StopPoints stopPoints = affectsStructure.getStopPoints();

        if (stopPoints != null && !isListNullOrEmpty(stopPoints.getAffectedStopPoints())) {

            for (AffectedStopPointStructure stopPoint : stopPoints.getAffectedStopPoints()) {
                StopPointRef stopPointRef = stopPoint.getStopPointRef();
                if (stopPointRef == null || stopPointRef.getValue() == null) {
                    continue;
                }
                String stopId = stopPointRef.getValue();

                AlertPatch alertPatch = new AlertPatch();
                alertPatch.setStop(new AgencyAndId(feedId, stopId));
                alertPatch.setId(situationNumber);
                patches.add(alertPatch);
            }
        }

        AffectsScopeStructure.Networks networks = affectsStructure.getNetworks();

        if (networks != null && !isListNullOrEmpty(networks.getAffectedNetworks())) {

            for (AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork : networks.getAffectedNetworks()) {
                List<AffectedLineStructure> affectedLines = affectedNetwork.getAffectedLines();
                if (affectedLines != null && !isListNullOrEmpty(affectedLines)) {
                    for (AffectedLineStructure line : affectedLines) {

                        List<LineRef> lineReves = line.getLineReves();
                        for (LineRef lineRef : lineReves) {

                            if (lineRef == null || lineRef.getValue() == null) {
                                continue;
                            }
                            String ref = lineRef.getValue();
                            AlertPatch alertPatch = new AlertPatch();
                            alertPatch.setRoute(new AgencyAndId(feedId, ref));
                            alertPatch.setId(situationNumber);
                            patches.add(alertPatch);
                        }
                    }
                }
                NetworkRefStructure networkRef = affectedNetwork.getNetworkRef();
                if (networkRef == null || networkRef.getValue() == null) {
                    continue;
                }
                String networkId = networkRef.getValue();

                AlertPatch alertPatch = new AlertPatch();
                alertPatch.setTrip(new AgencyAndId(feedId, networkId));
                alertPatch.setId(situationNumber);
                patches.add(alertPatch);
            }
        }

        AffectsScopeStructure.StopPlaces stopPlaces = affectsStructure.getStopPlaces();

        if (stopPlaces != null && !isListNullOrEmpty(stopPlaces.getAffectedStopPlaces())) {

            for (AffectedStopPlaceStructure stopPoint : stopPlaces.getAffectedStopPlaces()) {
                StopPlaceRef stopPlace = stopPoint.getStopPlaceRef();
                if (stopPlace == null || stopPlace.getValue() == null) {
                    continue;
                }
                String stopId = stopPlace.getValue();

                AlertPatch alertPatch = new AlertPatch();
                alertPatch.setStop(new AgencyAndId(feedId, stopId));
                alertPatch.setId(stopId);
                patches.add(alertPatch);
            }
        }

        AffectsScopeStructure.VehicleJourneys vjs = affectsStructure.getVehicleJourneys();
        if (vjs != null && !isListNullOrEmpty(vjs.getAffectedVehicleJourneies())) {

            for (AffectedVehicleJourneyStructure vj : vjs.getAffectedVehicleJourneies()) {

                String lineRef = null;
                if (vj.getLineRef() != null) {
                    lineRef = vj.getLineRef().getValue();
                }

                List<VehicleJourneyRef> tripRefs = vj.getVehicleJourneyReves();
                AffectedVehicleJourneyStructure.Calls stopRefs = vj.getCalls();

                boolean hasTripRefs = !isListNullOrEmpty(tripRefs);
                boolean hasStopRefs = stopRefs != null && !isListNullOrEmpty(stopRefs.getCalls());

                if (!(hasTripRefs || hasStopRefs)) {
                    if (lineRef != null) {
                        AlertPatch alertPatch = new AlertPatch();
                        alertPatch.setRoute(new AgencyAndId(feedId, lineRef));
                        patches.add(alertPatch);
                    }
                } else if (hasTripRefs && hasStopRefs) {
                    for (VehicleJourneyRef vjRef : vj.getVehicleJourneyReves()) {
                        String tripId = vjRef.getValue();


                        for (AffectedCallStructure call : stopRefs.getCalls()) {
                            String stopId = call.getStopPointRef().getValue();

                            AlertPatch alertPatch = new AlertPatch();
                            alertPatch.setRoute(new AgencyAndId(feedId, lineRef));
                            alertPatch.setTrip(new AgencyAndId(feedId, tripId));
                            alertPatch.setStop(new AgencyAndId(feedId, stopId));
                            alertPatch.setId("VehicleJourneyCall_" + stopId);
                            patches.add(alertPatch);
                        }
                    }
                } else if (hasTripRefs) {
                    for (VehicleJourneyRef vjRef : vj.getVehicleJourneyReves()) {
                        String tripId = vjRef.getValue();

                        AlertPatch alertPatch = new AlertPatch();
                        alertPatch.setRoute(new AgencyAndId(feedId, lineRef));
                        alertPatch.setTrip(new AgencyAndId(feedId, tripId));
                        alertPatch.setId("VehicleJourneyTrip_" + tripId);
                        patches.add(alertPatch);
                    }
                } else {
                    for (AffectedCallStructure call : stopRefs.getCalls()) {
                        String stopId = call.getStopPointRef().getValue();

                        AlertPatch alertPatch = new AlertPatch();
                        alertPatch.setRoute(new AgencyAndId(feedId, lineRef));
                        alertPatch.setStop(new AgencyAndId(feedId, stopId));
                        alertPatch.setId("VehicleJourneyStopPoint_" + stopId);
                        patches.add(alertPatch);
                    }
                }
            }
        }

        for (AlertPatch patch:patches) {
            patch.setTimePeriods(periods);
            patch.setAlert(alert);
            patchIds.add(patch.getId());
            alertPatchService.apply(patch);
        }

    }

    private boolean isListNullOrEmpty(List list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }


    private void handleAlert(String id, GtfsRealtime.Alert alert) {
        Alert alertText = new Alert();
        alertText.alertDescriptionText = deBuffer(alert.getDescriptionText());
        alertText.alertHeaderText = deBuffer(alert.getHeaderText());
        alertText.alertUrl = deBuffer(alert.getUrl());
        ArrayList<TimePeriod> periods = new ArrayList<TimePeriod>();
        if(alert.getActivePeriodCount() > 0) {
            long bestStartTime = Long.MAX_VALUE;
            long lastEndTime = Long.MIN_VALUE;
            for (TimeRange activePeriod : alert.getActivePeriodList()) {
                final long realStart = activePeriod.hasStart() ? activePeriod.getStart() : 0;
                final long start = activePeriod.hasStart() ? realStart - earlyStart : 0;
                if (realStart > 0 && realStart < bestStartTime) {
                    bestStartTime = realStart;
                }
                final long end = activePeriod.hasEnd() ? activePeriod.getEnd() : Long.MAX_VALUE;
                if (end > lastEndTime) {
                    lastEndTime = end;
                }
                periods.add(new TimePeriod(start, end));
            }
            if (bestStartTime != Long.MAX_VALUE) {
                alertText.effectiveStartDate = new Date(bestStartTime * 1000);
            }
            if (lastEndTime != Long.MIN_VALUE) {
                alertText.effectiveEndDate = new Date(lastEndTime * 1000);
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, Long.MAX_VALUE));
        }
        for (EntitySelector informed : alert.getInformedEntityList()) {
            if (fuzzyTripMatcher != null && informed.hasTrip()) {
                TripDescriptor trip = fuzzyTripMatcher.match(feedId, informed.getTrip());
                informed = informed.toBuilder().setTrip(trip).build();
            }
            String patchId = createId(id, informed);

            String routeId = null;
            if (informed.hasRouteId()) {
                routeId = informed.getRouteId();
            }

            int direction;
            if (informed.hasTrip() && informed.getTrip().hasDirectionId()) {
                direction = informed.getTrip().getDirectionId();
            } else {
                direction = -1;
            }

            // TODO: The other elements of a TripDescriptor are ignored...
            String tripId = null;
            if (informed.hasTrip() && informed.getTrip().hasTripId()) {
                tripId = informed.getTrip().getTripId();
            }
            String stopId = null;
            if (informed.hasStopId()) {
                stopId = informed.getStopId();
            }

            String agencyId = informed.getAgencyId();
            if (informed.hasAgencyId()) {
                agencyId = informed.getAgencyId().intern();
            }

            AlertPatch patch = new AlertPatch();
            patch.setFeedId(feedId);
            if (routeId != null) {
                patch.setRoute(new AgencyAndId(feedId, routeId));
                // Makes no sense to set direction if we don't have a route
                if (direction != -1) {
                    patch.setDirectionId(direction);
                }
            }
            if (tripId != null) {
                patch.setTrip(new AgencyAndId(feedId, tripId));
            }
            if (stopId != null) {
                patch.setStop(new AgencyAndId(feedId, stopId));
            }
            if (agencyId != null && routeId == null && tripId == null && stopId == null) {
                patch.setAgencyId(agencyId);
            }
            patch.setTimePeriods(periods);
            patch.setAlert(alertText);

            patch.setId(patchId);
            patchIds.add(patchId);

            alertPatchService.apply(patch);
        }
    }

    private String createId(String id, EntitySelector informed) {
        return id + " "
            + (informed.hasAgencyId  () ? informed.getAgencyId  () : " null ") + " "
            + (informed.hasRouteId   () ? informed.getRouteId   () : " null ") + " "
            + (informed.hasTrip() && informed.getTrip().hasDirectionId() ?
                informed.getTrip().hasDirectionId() : " null ") + " "
            + (informed.hasRouteType () ? informed.getRouteType () : " null ") + " "
            + (informed.hasStopId    () ? informed.getStopId    () : " null ") + " "
            + (informed.hasTrip() && informed.getTrip().hasTripId() ?
                informed.getTrip().getTripId() : " null ");
    }

    /**
     * convert a protobuf TranslatedString to a OTP TranslatedString
     *
     * @return A TranslatedString containing the same information as the input
     */
    private I18NString deBuffer(GtfsRealtime.TranslatedString input) {
        Map<String, String> translations = new HashMap<>();
        for (GtfsRealtime.TranslatedString.Translation translation : input.getTranslationList()) {
            String language = translation.getLanguage();
            String string = translation.getText();
            translations.put(language, string);
        }
        return translations.isEmpty() ? null : TranslatedString.getI18NString(translations);
    }

    public void setFeedId(String feedId) {
        if(feedId != null)
            this.feedId = feedId.intern();
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

    public void setFuzzyTripMatcher(GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher) {
        this.fuzzyTripMatcher = fuzzyTripMatcher;
    }

    public void setSiriFuzzyTripMatcher(SiriFuzzyTripMatcher siriFuzzyTripMatcher) {
        this.siriFuzzyTripMatcher = siriFuzzyTripMatcher;
    }
}
