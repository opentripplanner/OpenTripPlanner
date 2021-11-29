package org.opentripplanner.updater.alerts;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.TranslatedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This updater only includes GTFS-Realtime Service Alert feeds.
 * @author novalis
 *
 */
public class AlertsUpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(AlertsUpdateHandler.class);

    private String feedId;

    private TransitAlertService transitAlertService;

    /** How long before the posted start of an event it should be displayed to users */
    private long earlyStart;

    /** Set only if we should attempt to match the trip_id from other data in TripDescriptor */
    private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    public void update(FeedMessage message) {
        Collection<TransitAlert> alerts = new ArrayList<>();
        for (FeedEntity entity : message.getEntityList()) {
            if (!entity.hasAlert()) {
                continue;
            }
            GtfsRealtime.Alert alert = entity.getAlert();
            String id = entity.getId();
            alerts.add(mapAlert(id, alert));
        }
        transitAlertService.setAlerts(alerts);
    }

    private TransitAlert mapAlert(String id, GtfsRealtime.Alert alert) {
        TransitAlert alertText = new TransitAlert();
        alertText.setId(id);
        alertText.alertDescriptionText = deBuffer(alert.getDescriptionText());
        alertText.alertHeaderText = deBuffer(alert.getHeaderText());
        alertText.alertUrl = deBuffer(alert.getUrl());
        ArrayList<TimePeriod> periods = new ArrayList<TimePeriod>();
        if(alert.getActivePeriodCount() > 0) {
            for (TimeRange activePeriod : alert.getActivePeriodList()) {
                final long realStart = activePeriod.hasStart() ? activePeriod.getStart() : 0;
                final long start = activePeriod.hasStart() ? realStart - earlyStart : 0;
                final long end = activePeriod.hasEnd() ? activePeriod.getEnd() : TimePeriod.OPEN_ENDED;
                periods.add(new TimePeriod(start, end));
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, TimePeriod.OPEN_ENDED));
        }
        alertText.setTimePeriods(periods);
        alertText.setFeedId(feedId);
        for (GtfsRealtime.EntitySelector informed : alert.getInformedEntityList()) {
            if (fuzzyTripMatcher != null && informed.hasTrip()) {
                TripDescriptor trip = fuzzyTripMatcher.match(feedId, informed.getTrip());
                informed = informed.toBuilder().setTrip(trip).build();
            }

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

            if (tripId != null) {
                if (stopId != null) {
                    alertText.addEntity(new EntitySelector.StopAndTrip(
                        new FeedScopedId(feedId, stopId),
                        new FeedScopedId(feedId, tripId)
                    ));
                } else {
                    alertText.addEntity(new EntitySelector.Trip(new FeedScopedId(feedId, tripId)));
                }
            } else if (routeId != null) {
                // TODO: Handle direction
                if (stopId != null) {
                    alertText.addEntity(new EntitySelector.StopAndRoute(
                        new FeedScopedId(feedId, stopId),
                        new FeedScopedId(feedId, routeId)
                    ));
                } else {
                    alertText.addEntity(new EntitySelector.Route(new FeedScopedId(feedId, routeId)));
                }
            } else if (stopId != null) {
                alertText.addEntity(new EntitySelector.Stop(new FeedScopedId(feedId, stopId)));
            } else if (agencyId != null) {
                alertText.addEntity(new EntitySelector.Agency(new FeedScopedId(feedId, agencyId)));
            }
        }

        return alertText;
    }

    /**
     * Convert a GTFS-RT Protobuf TranslatedString to a OTP TranslatedString or NonLocalizedString.
     *
     * @return An OTP TranslatedString containing the same information as the input GTFS-RT Protobuf TranslatedString.
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

    public void setTransitAlertService(TransitAlertService transitAlertService) {
        this.transitAlertService = transitAlertService;
    }

    public void setEarlyStart(long earlyStart) {
        this.earlyStart = earlyStart;
    }

    public void setFuzzyTripMatcher(GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher) {
        this.fuzzyTripMatcher = fuzzyTripMatcher;
    }
}
