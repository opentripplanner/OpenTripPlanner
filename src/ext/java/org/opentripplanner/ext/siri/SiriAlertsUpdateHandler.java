package org.opentripplanner.ext.siri;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
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
import uk.org.siri.siri20.SituationExchangeDeliveryStructure;
import uk.org.siri.siri20.StopPointRef;
import uk.org.siri.siri20.VehicleJourneyRef;
import uk.org.siri.siri20.WorkflowStatusEnumeration;

/**
 * This updater applies the equivalent of GTFS Alerts, but from SIRI Situation Exchange feeds. NOTE
 * this cannot handle situations where there are multiple feeds with different IDs (for now it may
 * only work in single-feed regions).
 */
public class SiriAlertsUpdateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SiriAlertsUpdateHandler.class);
  private final String feedId;
  private final TransitService transitService;
  private final Set<TransitAlert> alerts = new HashSet<>();
  private TransitAlertService transitAlertService;
  /** How long before the posted start of an event it should be displayed to users */
  private long earlyStart;
  private SiriFuzzyTripMatcher siriFuzzyTripMatcher;

  public SiriAlertsUpdateHandler(String feedId, TransitModel transitModel) {
    this.feedId = feedId;
    this.transitService = new DefaultTransitService(transitModel);
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
              alert.setId(situationNumber);
              if (alert.getEntities().isEmpty()) {
                LOG.info(
                  "No match found for Alert - setting Unknown entity for situation with situationNumber {}",
                  situationNumber
                );
                alert.addEntity(
                  new EntitySelector.Unknown("Alert had no entities that could be handled")
                );
              }
              alerts.removeIf(transitAlert -> transitAlert.getId().equals(situationNumber));
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

  private TransitAlert handleAlert(PtSituationElement situation) {
    TransitAlert alert = createAlertWithTexts(situation);

    if (
      (alert.alertHeaderText == null || alert.alertHeaderText.toString().isEmpty()) &&
      (alert.alertDescriptionText == null || alert.alertDescriptionText.toString().isEmpty()) &&
      (alert.alertDetailText == null || alert.alertDetailText.toString().isEmpty())
    ) {
      LOG.debug(
        "Empty Alert - ignoring situationNumber: {}",
        situation.getSituationNumber() != null ? situation.getSituationNumber().getValue() : null
      );
      return null;
    }

    ArrayList<TimePeriod> periods = new ArrayList<>();
    if (situation.getValidityPeriods().size() > 0) {
      for (HalfOpenTimestampOutputRangeStructure activePeriod : situation.getValidityPeriods()) {
        final long realStart = activePeriod.getStartTime() != null
          ? getEpochSecond(activePeriod.getStartTime())
          : 0;
        final long start = activePeriod.getStartTime() != null ? realStart - earlyStart : 0;

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

          FeedScopedId stopId = getStop(stopPointRef.getValue(), feedId, transitService);

          if (stopId == null) {
            stopId = new FeedScopedId(feedId, stopPointRef.getValue());
          }

          EntitySelector.Stop entitySelector = new EntitySelector.Stop(
            stopId,
            resolveStopConditions(stopPoint.getStopConditions())
          );

          alert.addEntity(entitySelector);
        }
      } else if (stopPlaces != null && isNotEmpty(stopPlaces.getAffectedStopPlaces())) {
        for (AffectedStopPlaceStructure stopPoint : stopPlaces.getAffectedStopPlaces()) {
          StopPlaceRef stopPlace = stopPoint.getStopPlaceRef();
          if (stopPlace == null || stopPlace.getValue() == null) {
            continue;
          }

          FeedScopedId stopId = getStop(stopPlace.getValue(), feedId, transitService);

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
                    List<Serializable> stopPointsList = route
                      .getStopPoints()
                      .getAffectedStopPointsAndLinkProjectionToNextStopPoints();
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

              if (!affectedStops.isEmpty()) {
                for (AffectedStopPointStructure affectedStop : affectedStops) {
                  FeedScopedId stop = getStop(
                    affectedStop.getStopPointRef().getValue(),
                    feedId,
                    transitService
                  );
                  if (stop == null) {
                    stop = new FeedScopedId(feedId, affectedStop.getStopPointRef().getValue());
                  }
                  EntitySelector.StopAndRoute entitySelector = new EntitySelector.StopAndRoute(
                    stop,
                    resolveStopConditions(affectedStop.getStopConditions()),
                    affectedRoute
                  );
                  alert.addEntity(entitySelector);
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
          List<AffectedStopPointStructure> affectedStops = new ArrayList<>();

          List<AffectedRouteStructure> routes = affectedVehicleJourney.getRoutes();
          // Resolve AffectedStop-ids
          if (routes != null) {
            for (AffectedRouteStructure route : routes) {
              if (route.getStopPoints() != null) {
                List<Serializable> stopPointsList = route
                  .getStopPoints()
                  .getAffectedStopPointsAndLinkProjectionToNextStopPoints();
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

              FeedScopedId tripIdFromVehicleJourney = getTripId(
                vehicleJourneyRef.getValue(),
                feedId,
                transitService
              );

              ZonedDateTime originAimedDepartureTime = affectedVehicleJourney.getOriginAimedDepartureTime() !=
                null
                ? affectedVehicleJourney.getOriginAimedDepartureTime()
                : ZonedDateTime.now();

              ZonedDateTime startOfService = ServiceDateUtils.asStartOfService(
                originAimedDepartureTime
              );

              LocalDate serviceDate = startOfService.toLocalDate();

              if (tripIdFromVehicleJourney != null) {
                tripIds.add(tripIdFromVehicleJourney);
              } else if (siriFuzzyTripMatcher != null) {
                tripIds =
                  siriFuzzyTripMatcher.getTripIdForInternalPlanningCodeServiceDate(
                    vehicleJourneyRef.getValue(),
                    serviceDate
                  );
              }

              for (FeedScopedId tripId : tripIds) {
                if (!affectedStops.isEmpty()) {
                  for (AffectedStopPointStructure affectedStop : affectedStops) {
                    FeedScopedId stop = getStop(
                      affectedStop.getStopPointRef().getValue(),
                      feedId,
                      transitService
                    );
                    if (stop == null) {
                      stop = new FeedScopedId(feedId, affectedStop.getStopPointRef().getValue());
                    }
                    // Creating unique, deterministic id for the alert
                    EntitySelector.StopAndTrip entitySelector = new EntitySelector.StopAndTrip(
                      stop,
                      tripId,
                      serviceDate,
                      resolveStopConditions(affectedStop.getStopConditions())
                    );
                    alert.addEntity(entitySelector);
                  }
                } else {
                  alert.addEntity(new EntitySelector.Trip(tripId, serviceDate));
                }
              }
            }
          }

          final FramedVehicleJourneyRefStructure framedVehicleJourneyRef = affectedVehicleJourney.getFramedVehicleJourneyRef();
          if (framedVehicleJourneyRef != null) {
            final DataFrameRefStructure dataFrameRef = framedVehicleJourneyRef.getDataFrameRef();
            final String datedVehicleJourneyRef = framedVehicleJourneyRef.getDatedVehicleJourneyRef();

            FeedScopedId tripId = getTripId(datedVehicleJourneyRef, feedId, transitService);

            if (tripId != null) {
              LocalDate serviceDate = null;
              if (dataFrameRef != null && dataFrameRef.getValue() != null) {
                serviceDate = LocalDate.parse(dataFrameRef.getValue());
              }

              if (!affectedStops.isEmpty()) {
                for (AffectedStopPointStructure affectedStop : affectedStops) {
                  FeedScopedId stop = getStop(
                    affectedStop.getStopPointRef().getValue(),
                    feedId,
                    transitService
                  );
                  if (stop == null) {
                    stop = new FeedScopedId(feedId, affectedStop.getStopPointRef().getValue());
                  }

                  alert.addEntity(
                    new EntitySelector.StopAndTrip(
                      stop,
                      tripId,
                      serviceDate,
                      resolveStopConditions(affectedStop.getStopConditions())
                    )
                  );
                }
              } else {
                alert.addEntity(new EntitySelector.Trip(tripId, serviceDate));
              }
            }
          }
        }
      }
    }

    alert.alertType = situation.getReportType();

    alert.severity = SiriSeverityMapper.getAlertSeverityForSiriSeverity(situation.getSeverity());

    if (situation.getParticipantRef() != null) {
      String codespace = situation.getParticipantRef().getValue();
      alert.setFeedId(codespace + ":Authority:" + codespace); //TODO - SIRI: Should probably not assume this codespace -> authority rule
    }

    return alert;
  }

  private static FeedScopedId getStop(
    String siriStopId,
    String feedId,
    TransitService transitService
  ) {
    FeedScopedId id = new FeedScopedId(feedId, siriStopId);
    if (transitService.getRegularStop(id) != null) {
      return id;
    } else if (transitService.getStationById(id) != null) {
      return id;
    }

    return null;
  }

  private static FeedScopedId getTripId(
    String vehicleJourney,
    String feedId,
    TransitService transitService
  ) {
    Trip trip = transitService.getTripForId(new FeedScopedId(feedId, vehicleJourney));
    if (trip != null) {
      return trip.getId();
    }
    //Attempt to find trip using datedServiceJourneys
    TripOnServiceDate tripOnServiceDate = transitService.getTripOnServiceDateById(
      new FeedScopedId(feedId, vehicleJourney)
    );
    if (tripOnServiceDate != null) {
      return tripOnServiceDate.getTrip().getId();
    }
    return null;
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
