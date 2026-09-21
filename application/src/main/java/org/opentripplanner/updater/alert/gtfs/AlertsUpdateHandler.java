package org.opentripplanner.updater.alert.gtfs;

import static org.opentripplanner.updater.alert.gtfs.mapping.GtfsRealtimeCauseMapper.getAlertCauseForGtfsRtCause;
import static org.opentripplanner.updater.alert.gtfs.mapping.GtfsRealtimeEffectMapper.getAlertEffectForGtfsRtEffect;
import static org.opentripplanner.updater.alert.gtfs.mapping.GtfsRealtimeSeverityMapper.getAlertSeverityForGtfsRtSeverity;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.TranslatedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.mapping.DirectionMapper;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.alertpatch.TransitAlertBuilder;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This updater only includes GTFS-Realtime Service Alert feeds.
 *
 * @author novalis
 */
public class AlertsUpdateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AlertsUpdateHandler.class);
  private static final int MISSING_INT_FIELD_VALUE = -1;
  private String feedId;
  private TransitAlertService transitAlertService;

  /** How long before the posted start of an event it should be displayed to users */
  private Duration earlyStart = Duration.ZERO;

  /** Set only if we should attempt to match the trip_id from other data in TripDescriptor */
  private final boolean fuzzyTripMatching;

  // TODO: replace this with a runtime solution
  private final DirectionMapper directionMapper = new DirectionMapper(DataImportIssueStore.NOOP);

  public AlertsUpdateHandler(boolean fuzzyTripMatching) {
    this.fuzzyTripMatching = fuzzyTripMatching;
  }

  public void update(FeedMessage message, GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher) {
    Collection<TransitAlert> alerts = new ArrayList<>();
    for (FeedEntity entity : message.getEntityList()) {
      if (!entity.hasAlert()) {
        continue;
      }
      GtfsRealtime.Alert alert = entity.getAlert();
      String id = entity.getId();
      try {
        alerts.add(mapAlert(id, alert, fuzzyTripMatcher));
      } catch (Exception e) {
        LOG.warn("Failed to map GTFS-RT alert with id {}: {}", id, e.getMessage(), e);
      }
    }
    transitAlertService.setAlerts(alerts);
  }

  public void setFeedId(String feedId) {
    if (feedId != null) {
      this.feedId = feedId.intern();
    }
  }

  public void setTransitAlertService(TransitAlertService transitAlertService) {
    this.transitAlertService = transitAlertService;
  }

  public void setEarlyStart(Duration earlyStart) {
    this.earlyStart = earlyStart;
  }

  private TransitAlert mapAlert(
    String id,
    GtfsRealtime.Alert alert,
    GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher
  ) {
    TransitAlertBuilder alertBuilder = TransitAlert.of(new FeedScopedId(feedId, id))
      .withDescriptionText(deBuffer(alert.getDescriptionText()))
      .withHeaderText(deBuffer(alert.getHeaderText()))
      .withUrl(deBuffer(alert.getUrl()))
      .withSeverity(getAlertSeverityForGtfsRtSeverity(alert.getSeverityLevel()))
      .withCause(getAlertCauseForGtfsRtCause(alert.getCause()))
      .withEffect(getAlertEffectForGtfsRtEffect(alert.getEffect()));

    if (alert.getActivePeriodCount() > 0) {
      ArrayList<TimePeriod> periods = new ArrayList<>();
      for (TimeRange activePeriod : alert.getActivePeriodList()) {
        final Instant start = activePeriod.hasStart()
          ? Instant.ofEpochSecond(activePeriod.getStart()).minus(earlyStart)
          : null;
        final Instant end = activePeriod.hasEnd()
          ? Instant.ofEpochSecond(activePeriod.getEnd())
          : null;
        periods.add(TimePeriod.of(start, end));
      }
      alertBuilder.withCalendar(AlertCalendar.of(periods));
    } else {
      // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
      alertBuilder.withCalendar(AlertCalendar.ofAlwaysActive());
    }

    for (GtfsRealtime.EntitySelector informed : alert.getInformedEntityList()) {
      if (fuzzyTripMatching && informed.hasTrip()) {
        TripDescriptor trip = fuzzyTripMatcher.match(feedId, informed.getTrip());
        informed = informed.toBuilder().setTrip(trip).build();
      }

      String routeId = null;
      if (informed.hasRouteId()) {
        routeId = informed.getRouteId();
      }

      int directionId = MISSING_INT_FIELD_VALUE;
      if (informed.hasDirectionId()) {
        directionId = informed.getDirectionId();
      }

      String tripId = null;
      if (informed.hasTrip() && informed.getTrip().hasTripId()) {
        tripId = informed.getTrip().getTripId();
      }
      String stopId = null;
      if (informed.hasStopId()) {
        stopId = informed.getStopId();
      }

      String agencyId = null;
      if (informed.hasAgencyId()) {
        agencyId = informed.getAgencyId().intern();
      }

      int routeType = MISSING_INT_FIELD_VALUE;
      if (informed.hasRouteType()) {
        routeType = informed.getRouteType();
      }

      if (tripId != null) {
        if (stopId != null) {
          alertBuilder.addEntity(
            new EntitySelector.StopAndTrip(
              new FeedScopedId(feedId, stopId),
              new FeedScopedId(feedId, tripId)
            )
          );
        } else {
          alertBuilder.addEntity(new EntitySelector.Trip(new FeedScopedId(feedId, tripId)));
        }
      } else if (routeId != null) {
        if (stopId != null) {
          alertBuilder.addEntity(
            new EntitySelector.StopAndRoute(
              new FeedScopedId(feedId, stopId),
              new FeedScopedId(feedId, routeId)
            )
          );
        } else if (directionId != MISSING_INT_FIELD_VALUE) {
          alertBuilder.addEntity(
            new EntitySelector.DirectionAndRoute(
              new FeedScopedId(feedId, routeId),
              directionMapper.map(directionId)
            )
          );
        } else {
          alertBuilder.addEntity(new EntitySelector.Route(new FeedScopedId(feedId, routeId)));
        }
      } else if (stopId != null) {
        alertBuilder.addEntity(new EntitySelector.Stop(new FeedScopedId(feedId, stopId)));
      } else if (agencyId != null) {
        FeedScopedId feedScopedAgencyId = new FeedScopedId(feedId, agencyId);
        if (routeType != MISSING_INT_FIELD_VALUE) {
          alertBuilder.addEntity(
            new EntitySelector.RouteTypeAndAgency(feedScopedAgencyId, routeType)
          );
        } else {
          alertBuilder.addEntity(new EntitySelector.Agency(feedScopedAgencyId));
        }
      } else if (routeType != MISSING_INT_FIELD_VALUE) {
        alertBuilder.addEntity(new EntitySelector.RouteType(feedId, routeType));
      } else {
        String description = "Entity selector: " + informed;
        alertBuilder.addEntity(new EntitySelector.Unknown(description));
      }
    }

    if (!alertBuilder.hasEntities()) {
      alertBuilder.addEntity(new EntitySelector.Unknown("Alert had no entities"));
    }

    return alertBuilder.build();
  }

  /**
   * Convert a GTFS-RT Protobuf TranslatedString to an TranslatedString.
   *
   * @return An OTP TranslatedString containing the same information as the input GTFS-RT Protobuf
   * TranslatedString.
   */
  private I18NString deBuffer(GtfsRealtime.TranslatedString input) {
    Map<String, String> translations = new HashMap<>();
    for (GtfsRealtime.TranslatedString.Translation translation : input.getTranslationList()) {
      String language = translation.getLanguage();
      String string = translation.getText();
      translations.put(language, string);
    }
    return translations.isEmpty() ? null : TranslatedString.getI18NString(translations, true);
  }
}
