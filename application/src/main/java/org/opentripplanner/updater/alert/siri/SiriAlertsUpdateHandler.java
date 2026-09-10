package org.opentripplanner.updater.alert.siri;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.i18n.TranslatedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.alertpatch.TransitAlertBuilder;
import org.opentripplanner.service.transitalert.TransitAlertRepository;
import org.opentripplanner.updater.AlertRealTimeUpdateContext;
import org.opentripplanner.updater.UpdateIncrementality;
import org.opentripplanner.updater.alert.siri.mapping.AffectsMapper;
import org.opentripplanner.updater.alert.siri.mapping.SiriSeverityMapper;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatcherCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.DefaultedTextStructure;
import uk.org.siri.siri21.HalfOpenTimestampOutputRangeStructure;
import uk.org.siri.siri21.InfoLinkStructure;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.PtSituationElement;
import uk.org.siri.siri21.ServiceDelivery;
import uk.org.siri.siri21.SituationExchangeDeliveryStructure;
import uk.org.siri.siri21.WorkflowStatusEnumeration;

/**
 * This updater applies the equivalent of GTFS Alerts, but from SIRI Situation Exchange (SX) feeds.
 * As the incoming SIRI SX messages are mapped to internal TransitAlerts, their FeedScopedIds will
 * be the single feed ID associated with this update handler, plus the situation number provided in
 * the SIRI SX message.
 * This class cannot handle situations where incoming messages are being applied to multiple static
 * feeds with different IDs. For now it may only work in single-feed regions. A possible workaround
 * is to assign the same feed ID to multiple static feeds where it is known that their entity IDs
 * are all drawn from the same namespace (i.e. they are functionally fragments of the same feed).
 * TODO RT_AB: Internal FeedScopedId creation strategy should probably be pluggable or configurable.
 *   TG has indicated this is a necessary condition for moving this updater out of sandbox.
 * TODO RT_AB: The name should be clarified, as there is no such thing as "SIRI Alerts", and it
 *   is referencing the internal model concept of "Alerts" which are derived from GTFS terminology.
 */
public class SiriAlertsUpdateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SiriAlertsUpdateHandler.class);

  private final String feedId;

  private final Duration earlyStart;

  @Nullable
  private final SiriFuzzyTripMatcherCache siriFuzzyTripMatcherCache;

  /**
   * @param feedId the feed the incoming situations belong to
   * @param earlyStart display the alerts to users this long before their activePeriod begins
   */
  public SiriAlertsUpdateHandler(
    String feedId,
    Duration earlyStart,
    @Nullable SiriFuzzyTripMatcherCache siriFuzzyTripMatcherCache
  ) {
    this.feedId = feedId;
    this.earlyStart = earlyStart;
    this.siriFuzzyTripMatcherCache = siriFuzzyTripMatcherCache;
  }

  /**
   * Apply a SIRI-SX service delivery to the alert repository of the current transaction.
   * <p>
   * The handler is stateless: the repository carries the alerts committed by previous updates
   * forward. For a {@link UpdateIncrementality#DIFFERENTIAL} feed the mapped situations are merged
   * into the existing ones and situations with progress {@code CLOSED} are removed. For a
   * {@link UpdateIncrementality#FULL_DATASET} feed the delivery contains every situation that is
   * still active, so the previous alerts of this feed are all replaced, which also expires
   * situations that silently disappeared from the feed.
   */
  public void update(
    ServiceDelivery delivery,
    UpdateIncrementality incrementality,
    AlertRealTimeUpdateContext context
  ) {
    TransitAlertRepository repository = context.transitAlertRepository();

    for (SituationExchangeDeliveryStructure sxDelivery : delivery.getSituationExchangeDeliveries()) {
      SituationExchangeDeliveryStructure.Situations situations = sxDelivery.getSituations();
      if (situations != null) {
        long t1 = System.currentTimeMillis();
        var updatedAlerts = new ArrayList<TransitAlert>();
        var expiredAlertIds = new ArrayList<FeedScopedId>();

        for (PtSituationElement sxElement : situations.getPtSituationElements()) {
          boolean expireSituation =
            sxElement.getProgress() != null &&
            sxElement.getProgress().equals(WorkflowStatusEnumeration.CLOSED);

          if (sxElement.getSituationNumber() == null) {
            continue;
          }
          String situationNumber = sxElement.getSituationNumber().getValue();
          FeedScopedId id = new FeedScopedId(feedId, situationNumber);

          if (expireSituation) {
            expiredAlertIds.add(id);
          } else {
            TransitAlert alert = null;
            try {
              alert = mapSituationToAlert(sxElement, context);
            } catch (Exception e) {
              LOG.info(
                "Caught exception when processing situation with situationNumber {}: {}",
                situationNumber,
                e
              );
            }
            if (alert != null) {
              updatedAlerts.add(alert);
            } else {
              // The situation could not be mapped, e.g. because it has no text at all. Drop any
              // previous version of it instead of keeping a stale alert around.
              expiredAlertIds.add(id);
            }
          }
        }

        if (incrementality == UpdateIncrementality.FULL_DATASET) {
          repository.replaceAlerts(feedId, updatedAlerts);
        } else {
          repository.addOrUpdateAlerts(feedId, updatedAlerts);
          repository.removeAlerts(feedId, expiredAlertIds);
        }

        LOG.info(
          "Added {} alerts, expired {} alerts based on {} situations, current alert-count: {}, elapsed time {}ms",
          updatedAlerts.size(),
          expiredAlertIds.size(),
          situations.getPtSituationElements().size(),
          repository.getAlertCount(feedId),
          System.currentTimeMillis() - t1
        );
      }
    }
  }

  /**
   * Build an internal model Alert from an incoming SIRI situation exchange element.
   * May return null if the header, description, and detail text are all empty or missing in the
   * SIRI message. In all other cases it will return a valid TransitAlert instance.
   */
  private TransitAlert mapSituationToAlert(
    PtSituationElement situation,
    AlertRealTimeUpdateContext context
  ) {
    TransitAlertBuilder alert = createAlertWithTexts(situation);

    if (
      I18NString.hasNoValue(alert.headerText()) &&
      I18NString.hasNoValue(alert.descriptionText()) &&
      I18NString.hasNoValue(alert.detailText())
    ) {
      LOG.debug(
        "Empty Alert - ignoring situationNumber: {}",
        situation.getSituationNumber() != null ? situation.getSituationNumber().getValue() : null
      );
      return null;
    }

    if (situation.getCreationTime() != null) {
      alert.withCreationTime(situation.getCreationTime());
    }
    if (situation.getVersionedAtTime() != null) {
      alert.withUpdatedTime(situation.getVersionedAtTime());
    }
    if (situation.getVersion() != null && situation.getVersion().getValue() != null) {
      alert.withVersion(situation.getVersion().getValue().intValue());
    }

    if (situation.getValidityPeriods().size() > 0) {
      ArrayList<TimePeriod> periods = new ArrayList<>();
      for (HalfOpenTimestampOutputRangeStructure activePeriod : situation.getValidityPeriods()) {
        final Instant start =
          activePeriod.getStartTime() != null
            ? activePeriod.getStartTime().toInstant().minus(earlyStart)
            : null;
        final Instant end =
          activePeriod.getEndTime() != null ? activePeriod.getEndTime().toInstant() : null;

        periods.add(TimePeriod.of(start, end));
      }
      alert.withCalendar(AlertCalendar.of(periods));
    } else {
      // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
      alert.withCalendar(AlertCalendar.ofAlwaysActive());
    }

    if (situation.getPriority() != null) {
      alert.withPriority(situation.getPriority().intValue());
    }

    var fuzzyTripMatcher =
      siriFuzzyTripMatcherCache != null
        ? new SiriFuzzyTripMatcher(siriFuzzyTripMatcherCache, context.transitService())
        : null;
    alert.addEntites(
      new AffectsMapper(feedId, fuzzyTripMatcher, context.transitService()).mapAffects(
        situation.getAffects()
      )
    );

    if (alert.entities().isEmpty()) {
      LOG.info(
        "No match found for Alert - setting Unknown entity for situation with situationNumber {}",
        alert.getId()
      );
      alert.addEntity(new EntitySelector.Unknown("Alert had no entities that could be handled"));
    }

    if (situation.getReportType() != null) {
      alert.withType(situation.getReportType().value());
    }

    alert.withSeverity(SiriSeverityMapper.getAlertSeverityForSiriSeverity(situation.getSeverity()));

    if (situation.getParticipantRef() != null) {
      alert.withSiriCodespace(situation.getParticipantRef().getValue());
    }

    return alert.build();
  }

  /*
   * Creates a builder for an internal model TransitAlert. The builder is pre-filled with all
   * textual content from the supplied SIRI PtSituation. The builder also has the feed scoped ID
   * pre-set to the single feed ID associated with this update handler, plus the situation number
   * provided in the SIRI PtSituation.
   */
  private TransitAlertBuilder createAlertWithTexts(PtSituationElement situation) {
    return TransitAlert.of(new FeedScopedId(feedId, situation.getSituationNumber().getValue()))
      .withDescriptionText(mapTranslatedString(situation.getDescriptions()))
      .withDetailText(mapTranslatedString(situation.getDetails()))
      .withAdviceText(mapTranslatedString(situation.getAdvices()))
      .withHeaderText(mapTranslatedString(situation.getSummaries()))
      .withUrl(mapInfoLinkToI18NString(situation.getInfoLinks()))
      .addSiriUrls(mapInfoLinks(situation));
  }

  /*
   * Returns first InfoLink-uri as a String
   */
  private I18NString mapInfoLinkToI18NString(PtSituationElement.InfoLinks infoLinks) {
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
  private List<AlertUrl> mapInfoLinks(PtSituationElement situation) {
    PtSituationElement.InfoLinks infoLinks = situation.getInfoLinks();
    List<AlertUrl> alertUrls = new ArrayList<>();
    if (infoLinks != null) {
      if (isNotEmpty(infoLinks.getInfoLinks())) {
        for (InfoLinkStructure infoLink : infoLinks.getInfoLinks()) {
          String label = null;
          List<NaturalLanguageStringStructure> labels = infoLink.getLabels();
          if (labels != null && !labels.isEmpty()) {
            NaturalLanguageStringStructure lbl = labels.get(0);
            label = lbl.getValue();
          }

          var uri = infoLink.getUri();
          if (uri != null) {
            alertUrls.add(new AlertUrl(uri, label));
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug(
                "URI missing in info-link - ignoring info-link in situation: {}",
                situation.getSituationNumber() != null
                  ? situation.getSituationNumber().getValue()
                  : null
              );
            }
          }
        }
      }
    }
    return alertUrls;
  }

  /**
   * @return True if list have at least one element. {@code false} is returned if the given list is
   * empty or {@code null}.
   */
  private boolean isNotEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }

  /**
   * convert a SIRI DefaultedTextStructure to a OTP TranslatedString
   *
   * @return A TranslatedString containing the same information as the input
   */
  private I18NString mapTranslatedString(List<DefaultedTextStructure> input) {
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

    return translations.isEmpty() ? null : TranslatedString.getI18NString(translations, true);
  }
}
