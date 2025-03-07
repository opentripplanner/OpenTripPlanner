package org.opentripplanner.updater.alert.siri.mapping;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.siri.EntityResolver;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatcher;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.AffectedLineStructure;
import uk.org.siri.siri20.AffectedOperatorStructure;
import uk.org.siri.siri20.AffectedRouteStructure;
import uk.org.siri.siri20.AffectedStopPlaceStructure;
import uk.org.siri.siri20.AffectedStopPointStructure;
import uk.org.siri.siri20.AffectedVehicleJourneyStructure;
import uk.org.siri.siri20.AffectsScopeStructure;
import uk.org.siri.siri20.DatedVehicleJourneyRef;
import uk.org.siri.siri20.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri20.LineRef;
import uk.org.siri.siri20.NetworkRefStructure;
import uk.org.siri.siri20.OperatorRefStructure;
import uk.org.siri.siri20.RoutePointTypeEnumeration;
import uk.org.siri.siri20.StopPointRef;
import uk.org.siri.siri20.VehicleJourneyRef;

/**
 * Maps a {@link AffectsScopeStructure} to a list of {@link EntitySelector}s
 *
 * Concretely: this takes the parts of the SIRI SX (Alerts) message describing which transit
 * entities are concerned by the alert, and maps them to EntitySelectors, which can match multiple
 * OTP internal model entities that should be associated with the message.
 */
public class AffectsMapper {

  private final String feedId;
  private final SiriFuzzyTripMatcher siriFuzzyTripMatcher;
  private final TransitService transitService;

  private final EntityResolver entityResolver;

  public AffectsMapper(
    String feedId,
    SiriFuzzyTripMatcher siriFuzzyTripMatcher,
    TransitService transitService
  ) {
    this.feedId = feedId;
    this.siriFuzzyTripMatcher = siriFuzzyTripMatcher;
    this.transitService = transitService;
    this.entityResolver = new EntityResolver(transitService, feedId);
  }

  public List<EntitySelector> mapAffects(AffectsScopeStructure affectsStructure) {
    if (affectsStructure == null) {
      return List.of();
    }

    List<EntitySelector> selectors = new ArrayList<>();
    selectors.addAll(mapOperators(affectsStructure.getOperators()));
    selectors.addAll(mapStopPoints(affectsStructure.getStopPoints()));
    selectors.addAll(mapStopPlaces(affectsStructure.getStopPlaces()));
    selectors.addAll(mapNetworks(affectsStructure.getNetworks()));
    selectors.addAll(mapVehicleJourneys(affectsStructure.getVehicleJourneys()));

    return selectors;
  }

  private List<EntitySelector> mapVehicleJourneys(AffectsScopeStructure.VehicleJourneys vjs) {
    if (vjs == null || isEmpty(vjs.getAffectedVehicleJourneies())) {
      return List.of();
    }

    List<EntitySelector> selectors = new ArrayList<>();
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
              if (serializable instanceof AffectedStopPointStructure stopPointStructure) {
                affectedStops.add(stopPointStructure);
              }
            }
          }
        }
      }

      List<VehicleJourneyRef> vehicleJourneyReves = affectedVehicleJourney.getVehicleJourneyReves();

      if (isNotEmpty(vehicleJourneyReves)) {
        List<FeedScopedId> affectedTripIds = new ArrayList<>();

        for (VehicleJourneyRef vehicleJourneyRef : vehicleJourneyReves) {
          List<FeedScopedId> tripIds = new ArrayList<>();

          Trip trip = entityResolver.resolveTrip(vehicleJourneyRef.getValue());

          if (trip != null) {
            // Match found - add tripId to selector
            tripIds.add(trip.getId());
          } else if (siriFuzzyTripMatcher != null) {
            // "Temporary", custom legacy solution - supports alerts tagged on NeTEx-"privateCode" + serviceDate
            tripIds.addAll(
              siriFuzzyTripMatcher.getTripIdForInternalPlanningCodeServiceDate(
                vehicleJourneyRef.getValue(),
                entityResolver.resolveServiceDate(
                  affectedVehicleJourney.getOriginAimedDepartureTime()
                )
              )
            );
          }

          if (tripIds.isEmpty()) {
            // Neither referenced Trip nor Fuzzy-matching found Trip - add Trip-id as-is
            tripIds.add(entityResolver.resolveId(vehicleJourneyRef.getValue()));
          }

          affectedTripIds.addAll(tripIds);
        }

        selectors.addAll(
          mapTripSelectors(
            affectedStops,
            affectedTripIds,
            entityResolver.resolveServiceDate(affectedVehicleJourney.getOriginAimedDepartureTime())
          )
        );
      }

      final FramedVehicleJourneyRefStructure framedVehicleJourneyRef =
        affectedVehicleJourney.getFramedVehicleJourneyRef();
      if (framedVehicleJourneyRef != null) {
        selectors.addAll(
          mapTripSelectors(
            affectedStops,
            List.of(entityResolver.resolveId(framedVehicleJourneyRef.getDatedVehicleJourneyRef())),
            entityResolver.resolveServiceDate(framedVehicleJourneyRef)
          )
        );
      }

      final List<DatedVehicleJourneyRef> datedVehicleJourneyReves =
        affectedVehicleJourney.getDatedVehicleJourneyReves();
      if (isNotEmpty(datedVehicleJourneyReves)) {
        for (DatedVehicleJourneyRef datedVehicleJourneyRef : datedVehicleJourneyReves) {
          // Lookup provided reference as if it is a DSJ
          TripOnServiceDate tripOnServiceDate = entityResolver.resolveTripOnServiceDate(
            datedVehicleJourneyRef.getValue()
          );
          if (tripOnServiceDate != null) {
            // Match found - add TripOnServiceDate-selector
            selectors.addAll(
              mapTripSelectors(
                affectedStops,
                List.of(tripOnServiceDate.getTrip().getId()),
                tripOnServiceDate.getServiceDate()
              )
            );
          } else {
            // Not found - add generic Trip-selector - with legacy ServiceDate if provided
            selectors.addAll(
              mapTripSelectors(
                affectedStops,
                List.of(entityResolver.resolveId(datedVehicleJourneyRef.getValue())),
                entityResolver.resolveServiceDate(
                  affectedVehicleJourney.getOriginAimedDepartureTime()
                )
              )
            );
          }
        }
      }
    }
    return selectors;
  }

  private List<EntitySelector> mapTripSelectors(
    List<AffectedStopPointStructure> affectedStops,
    List<FeedScopedId> tripIds,
    LocalDate serviceDate
  ) {
    List<EntitySelector> selectors = new ArrayList<>();

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
          EntitySelector.StopAndTrip entitySelector = new EntitySelector.StopAndTrip(
            stop,
            tripId,
            serviceDate,
            resolveStopConditions(affectedStop.getStopConditions())
          );
          selectors.add(entitySelector);
        }
      } else {
        selectors.add(new EntitySelector.Trip(tripId, serviceDate));
      }
    }
    return selectors;
  }

  private List<EntitySelector> mapNetworks(AffectsScopeStructure.Networks networks) {
    if (networks == null || isEmpty(networks.getAffectedNetworks())) {
      return List.of();
    }

    List<EntitySelector> selectors = new ArrayList<>();

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
                  if (serializable instanceof AffectedStopPointStructure stopPointStructure) {
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
              selectors.add(entitySelector);
            }
          } else {
            selectors.add(new EntitySelector.Route(affectedRoute));
          }
        }
      } else {
        NetworkRefStructure networkRef = affectedNetwork.getNetworkRef();
        if (networkRef == null || networkRef.getValue() == null) {
          continue;
        }
        String networkId = networkRef.getValue();
        // TODO: What to do here - We need to store network on route and add new EntitySelector
        selectors.add(new EntitySelector.Unknown("Alert affects network %s".formatted(networkId)));
      }
    }
    return selectors;
  }

  private List<EntitySelector> mapStopPoints(AffectsScopeStructure.StopPoints stopPoints) {
    if (stopPoints == null || isEmpty(stopPoints.getAffectedStopPoints())) {
      return List.of();
    }

    List<EntitySelector> selectors = new ArrayList<>();

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

      selectors.add(entitySelector);
    }

    return selectors;
  }

  private List<EntitySelector> mapStopPlaces(AffectsScopeStructure.StopPlaces stopPlaces) {
    if (stopPlaces == null || isEmpty(stopPlaces.getAffectedStopPlaces())) {
      return List.of();
    }
    List<EntitySelector> selectors = new ArrayList<>();

    for (AffectedStopPlaceStructure stopPlace : stopPlaces.getAffectedStopPlaces()) {
      StopPlaceRef stopPlaceRef = stopPlace.getStopPlaceRef();
      if (stopPlaceRef == null || stopPlaceRef.getValue() == null) {
        continue;
      }

      FeedScopedId stopId = getStop(stopPlaceRef.getValue(), feedId, transitService);

      if (stopId == null) {
        stopId = new FeedScopedId(feedId, stopPlaceRef.getValue());
      }

      selectors.add(new EntitySelector.Stop(stopId));
    }

    return selectors;
  }

  private List<EntitySelector> mapOperators(AffectsScopeStructure.Operators operators) {
    if (operators == null || isEmpty(operators.getAffectedOperators())) {
      return List.of();
    }

    List<EntitySelector> selectors = new ArrayList<>();

    for (AffectedOperatorStructure affectedOperator : operators.getAffectedOperators()) {
      OperatorRefStructure operatorRef = affectedOperator.getOperatorRef();
      if (operatorRef == null || operatorRef.getValue() == null) {
        continue;
      }

      // SIRI Operators are mapped to OTP Agency, this is probably wrong - but
      // I leave this for now.
      String agencyId = operatorRef.getValue();

      selectors.add(new EntitySelector.Agency(new FeedScopedId(feedId, agencyId)));
    }

    return selectors;
  }

  private static FeedScopedId getStop(
    String siriStopId,
    String feedId,
    TransitService transitService
  ) {
    FeedScopedId id = new FeedScopedId(feedId, siriStopId);
    if (transitService.getRegularStop(id) != null) {
      return id;
    } else if (transitService.getStation(id) != null) {
      return id;
    }

    return null;
  }

  private static Set<StopCondition> resolveStopConditions(
    List<RoutePointTypeEnumeration> stopConditions
  ) {
    Set<StopCondition> alertStopConditions = new HashSet<>();
    if (stopConditions != null) {
      for (RoutePointTypeEnumeration stopCondition : stopConditions) {
        switch (stopCondition) {
          case EXCEPTIONAL_STOP -> alertStopConditions.add(StopCondition.EXCEPTIONAL_STOP);
          case DESTINATION -> alertStopConditions.add(StopCondition.DESTINATION);
          case NOT_STOPPING -> alertStopConditions.add(StopCondition.NOT_STOPPING);
          case REQUEST_STOP -> alertStopConditions.add(StopCondition.REQUEST_STOP);
          case START_POINT -> alertStopConditions.add(StopCondition.START_POINT);
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
   * @return True if list is null or is empty.
   */
  private static boolean isEmpty(List<?> list) {
    return list == null || list.isEmpty();
  }

  /**
   * @return True if list have at least one element. {@code false} is returned if the given list is
   * empty or {@code null}.
   */
  private static boolean isNotEmpty(List<?> list) {
    return list != null && !list.isEmpty();
  }
}
