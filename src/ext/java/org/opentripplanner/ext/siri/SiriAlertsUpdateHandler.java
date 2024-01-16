package org.opentripplanner.ext.siri;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.siri.mapper.AffectsMapper;
import org.opentripplanner.ext.siri.mapper.SiriSeverityMapper;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.alertpatch.TransitAlertBuilder;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.DefaultedTextStructure;
import uk.org.siri.siri20.HalfOpenTimestampOutputRangeStructure;
import uk.org.siri.siri20.InfoLinkStructure;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.PtSituationElement;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.SituationExchangeDeliveryStructure;
import uk.org.siri.siri20.WorkflowStatusEnumeration;

/**
 * This updater applies the equivalent of GTFS Alerts, but from SIRI Situation Exchange feeds. NOTE
 * this cannot handle situations where there are multiple feeds with different IDs (for now it may
 * only work in single-feed regions).
 */
public class SiriAlertsUpdateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SiriAlertsUpdateHandler.class);
  private final String feedId;
  private final Set<TransitAlert> alerts = new HashSet<>();
  private final TransitAlertService transitAlertService;
  /** How long before the posted start of an event it should be displayed to users */
  private final Duration earlyStart;
  private final AffectsMapper affectsMapper;

  public SiriAlertsUpdateHandler(
    String feedId,
    TransitModel transitModel,
    TransitAlertService transitAlertService,
    SiriFuzzyTripMatcher siriFuzzyTripMatcher,
    Duration earlyStart
  ) {
    this.feedId = feedId;
    this.transitAlertService = transitAlertService;
    this.earlyStart = earlyStart;

    TransitService transitService = new DefaultTransitService(transitModel);
    this.affectsMapper = new AffectsMapper(feedId, siriFuzzyTripMatcher, transitService);
  }

  public void update(ServiceDelivery delivery) {
    for (SituationExchangeDeliveryStructure sxDelivery : delivery.getSituationExchangeDeliveries()) {
      SituationExchangeDeliveryStructure.Situations situations = sxDelivery.getSituations();
      if (situations != null) {
        long t1 = System.currentTimeMillis();
        int addedCounter = 0;
        int expiredCounter = 0;
        for (PtSituationElement sxElement : situations.getPtSituationElements()) {
          boolean expireSituation =
            (
              sxElement.getProgress() != null &&
              sxElement.getProgress().equals(WorkflowStatusEnumeration.CLOSED)
            );

          if (sxElement.getSituationNumber() == null) {
            continue;
          }
          String situationNumber = sxElement.getSituationNumber().getValue();
          FeedScopedId id = new FeedScopedId(feedId, situationNumber);

          if (expireSituation) {
            alerts.removeIf(transitAlert -> transitAlert.getId().equals(id));
            expiredCounter++;
          } else {
            TransitAlert alert = null;
            try {
              alert = handleAlert(sxElement);
              addedCounter++;
            } catch (Exception e) {
              LOG.info(
                "Caught exception when processing situation with situationNumber {}: {}",
                situationNumber,
                e
              );
            }
            if (alert != null) {
              alerts.removeIf(transitAlert -> transitAlert.getId().equals(id));
              alerts.add(alert);
            }
          }
        }

        transitAlertService.setAlerts(alerts);

        LOG.info(
          "Added {} alerts, expired {} alerts based on {} situations, current alert-count: {}, elapsed time {}ms",
          addedCounter,
          expiredCounter,
          situations.getPtSituationElements().size(),
          transitAlertService.getAllAlerts().size(),
          System.currentTimeMillis() - t1
        );
      }
    }
  }

  private TransitAlert handleAlert(PtSituationElement situation) {
    TransitAlertBuilder alert = createAlertWithTexts(situation);

    if (
      (alert.headerText() == null || alert.headerText().toString().isEmpty()) &&
      (alert.descriptionText() == null || alert.descriptionText().toString().isEmpty()) &&
      (alert.detailText() == null || alert.detailText().toString().isEmpty())
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

    ArrayList<TimePeriod> periods = new ArrayList<>();
    if (situation.getValidityPeriods().size() > 0) {
      for (HalfOpenTimestampOutputRangeStructure activePeriod : situation.getValidityPeriods()) {
        final long realStart = activePeriod.getStartTime() != null
          ? getEpochSecond(activePeriod.getStartTime())
          : 0;
        final long start = activePeriod.getStartTime() != null
          ? realStart - earlyStart.toSeconds()
          : 0;

        final long realEnd = activePeriod.getEndTime() != null
          ? getEpochSecond(activePeriod.getEndTime())
          : TimePeriod.OPEN_ENDED;
        final long end = activePeriod.getEndTime() != null ? realEnd : TimePeriod.OPEN_ENDED;

        periods.add(new TimePeriod(start, end));
      }
    } else {
      // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
      periods.add(new TimePeriod(0, TimePeriod.OPEN_ENDED));
    }

    alert.addTimePeriods(periods);

    if (situation.getPriority() != null) {
      alert.withPriority(situation.getPriority().intValue());
    }

    alert.addEntites(affectsMapper.mapAffects(situation.getAffects()));

    if (alert.entities().isEmpty()) {
      LOG.info(
        "No match found for Alert - setting Unknown entity for situation with situationNumber {}",
        alert.getId()
      );
      alert.addEntity(new EntitySelector.Unknown("Alert had no entities that could be handled"));
    }

    alert.withType(situation.getReportType());

    alert.withSeverity(SiriSeverityMapper.getAlertSeverityForSiriSeverity(situation.getSeverity()));

    if (situation.getParticipantRef() != null) {
      alert.withSiriCodespace(situation.getParticipantRef().getValue());
    }

    return alert.build();
  }

  private long getEpochSecond(ZonedDateTime startTime) {
    return startTime.toEpochSecond();
  }

  /*
   * Creates alert from PtSituation with all textual content
   */
  private TransitAlertBuilder createAlertWithTexts(PtSituationElement situation) {
    return TransitAlert
      .of(new FeedScopedId(feedId, situation.getSituationNumber().getValue()))
      .withDescriptionText(getTranslatedString(situation.getDescriptions()))
      .withDetailText(getTranslatedString(situation.getDetails()))
      .withAdviceText(getTranslatedString(situation.getAdvices()))
      .withHeaderText(getTranslatedString(situation.getSummaries()))
      .withUrl(getInfoLinkAsString(situation.getInfoLinks()))
      .addSiriUrls(getInfoLinks(situation.getInfoLinks()));
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

    return translations.isEmpty()
      ? null
      : TranslatedString.getI18NString(translations, false, true);
  }
}
