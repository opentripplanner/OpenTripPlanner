package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.time.ZoneIdFallback;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.leg.StreetLegBuilder;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.internal.notes.StreetNotesService;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.SiteResolver;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * A mapper class used in converting internal GraphPaths to Itineraries, which are returned by the
 * OTP APIs. This only produces itineraries for non-transit searches, as well as the non-transit
 * parts of itineraries containing transit, while the whole transit itinerary is produced by
 * {@link RaptorPathToItineraryMapper}.
 */
public class StreetPathToLegsMapper {

  private final SiteResolver siteResolver;
  private final ZoneId timeZone;
  private final StreetNotesService streetNotesService;
  private final StreetDetailsService streetDetailsService;
  private final double ellipsoidToGeoidDifference;

  public StreetPathToLegsMapper(
    SiteResolver siteResolver,
    ZoneId timeZone,
    StreetNotesService streetNotesService,
    StreetDetailsService streetDetailsService,
    double ellipsoidToGeoidDifference
  ) {
    this.siteResolver = siteResolver;
    this.timeZone = ZoneIdFallback.zoneId(timeZone);
    this.streetNotesService = streetNotesService;
    this.streetDetailsService = streetDetailsService;
    this.ellipsoidToGeoidDifference = ellipsoidToGeoidDifference;
  }

  public static boolean isRentalPickUp(State state) {
    return (
      state.getBackEdge() instanceof VehicleRentalEdge &&
      (state.getBackState() == null || !state.getBackState().isRentingVehicle())
    );
  }

  public static boolean isRentalStationDropOff(State state) {
    return (
      state.getBackEdge() instanceof VehicleRentalEdge && state.getBackState().isRentingVehicle()
    );
  }

  /**
   * Dropping of a free-floating vehicle can happen at any edge so be sure to select the correct
   * state (forward, not backward).
   */
  public static boolean isFloatingRentalDropoff(State state) {
    return (
      !state.isRentingVehicle() &&
      (state.getBackState() != null &&
        state.getBackState().getVehicleRentalState() == RENTING_FLOATING)
    );
  }

  /**
   * Generates {@link Leg}s from a {@link StreetPath}. If the whole path is traversed with a
   * singular street mode, this will return a single leg. Each change of street mode within a path
   * generates a new leg. Note, walking a bike does not cause a new leg to be generated. The legs
   * are returned in the order they are traversed in the path.
   */
  public List<Leg> map(StreetPath path, RouteRequest request) {
    return map(path, request, null);
  }

  /**
   * Generates {@link Leg}s from a {@link StreetPath}. If the whole path is traversed with a
   * singular street mode, this will return a single leg. Each change of street mode within a path
   * generates a new leg. Note, walking a bike does not cause a new leg to be generated. The legs
   * are returned in the order they are traversed in the path.
   *
   * @param startTime The legs are time shifted to so that the first leg starts at this time and the
   *                  next legs have the same delay as the first leg. If this is null, no time
   *                  shifting happens.
   */
  public List<Leg> map(StreetPath path, RouteRequest request, @Nullable ZonedDateTime startTime) {
    return map(path, request.listViaLocations(), startTime);
  }

  public List<Leg> map(
    StreetPath path,
    List<ViaLocation> viaLocations,
    @Nullable ZonedDateTime startTime
  ) {
    List<Leg> legs = new ArrayList<>();
    WalkStep previousStep = null;
    var subPaths = slicePath(path);
    if (subPaths.isEmpty()) {
      return List.of();
    }
    var delay = startTime != null
      ? Duration.between(subPaths.getFirst().startTime(), startTime)
      : null;
    for (var subPath : subPaths) {
      if (
        OTPFeature.FlexRouting.isOn() && subPath.states().get(1).backEdge instanceof FlexTripEdge
      ) {
        legs.add(generateFlexLeg(subPath, delay));
        previousStep = null;
        continue;
      }
      StreetLeg leg = generateLeg(subPath, previousStep, viaLocations, delay);
      legs.add(leg);

      List<WalkStep> walkSteps = leg.listWalkSteps();
      if (!walkSteps.isEmpty()) {
        previousStep = walkSteps.getLast();
      } else {
        previousStep = null;
      }
    }
    return legs;
  }

  /**
   * Slice a street path at the leg boundaries.
   *
   * @param streetPath The path to slice of input states
   * @return A list of subpaths representing the final legs
   */
  private static List<StreetPath> slicePath(StreetPath streetPath) {
    var states = streetPath.states();
    // Trivial case
    if (states.stream().allMatch(state -> state.getBackMode() == null)) {
      return List.of();
    }

    List<StreetPath> subPaths = new LinkedList<>();

    int previousBreak = 0;

    for (int i = 1; i < states.size() - 1; i++) {
      var backState = states.get(i);
      var forwardState = states.get(i + 1);

      var flexChange =
        forwardState.backEdge instanceof FlexTripEdge || backState.backEdge instanceof FlexTripEdge;
      var rentalChange =
        isRentalPickUp(backState) ||
        isRentalStationDropOff(backState) ||
        isFloatingRentalDropoff(backState);
      var parkingChange = backState.isVehicleParked() != forwardState.isVehicleParked();
      var carPickupChange = backState.getCarPickupState() != forwardState.getCarPickupState();

      if (parkingChange || flexChange || rentalChange || carPickupChange) {
        int nextBreak = i;

        if (nextBreak > previousBreak) {
          subPaths.add(streetPath.subPath(previousBreak, nextBreak + 1));
        }

        /* Remove the state for actually parking (traversing a VehicleParkingEdge) from the
         * states so that the leg from/to edges correspond to the actual entrances.
         * The actual time for parking is added to the walking leg in generateLeg().
         */
        if (parkingChange) {
          nextBreak++;
        }

        previousBreak = nextBreak;
      }
    }

    // Final leg
    if (states.size() > previousBreak) {
      subPaths.add(streetPath.subPath(previousBreak, states.size()));
    }

    return subPaths;
  }

  /**
   * Resolve mode from states.
   *
   * @param states The states that go with the leg
   */
  private static TraverseMode resolveMode(List<State> states) {
    return states
      .stream()
      // The first state is part of the previous leg
      .skip(1)
      .map(state -> {
        var mode = state.currentMode();

        if (mode != null) {
          // Resolve correct mode if renting vehicle
          if (state.isRentingVehicle()) {
            return state.stateData.rentalVehicleFormFactor.traverseMode;
          } else {
            return mode;
          }
        }

        return null;
      })
      .filter(Objects::nonNull)
      .findFirst()
      // Fallback to walking
      .orElse(TraverseMode.WALK);
  }

  /**
   * Make a {@link Place} to add to a {@link Leg}.
   *
   * @param state The {@link State}.
   * @return The resulting {@link Place} object.
   */
  private Place makePlace(State state, List<ViaLocation> viaLocations) {
    Vertex vertex = state.getVertex();
    I18NString name = vertex.getName();

    //This gets nicer names instead of osm:node:id when changing mode of transport
    //Names are generated from all the streets in a corner, same as names in origin and destination
    //We use name in TemporaryStreetLocation since this name generation already happened when temporary location was generated
    if (vertex instanceof StreetVertex && !(vertex instanceof TemporaryStreetLocation)) {
      name = ((StreetVertex) vertex).getIntersectionName();
    }

    if (vertex instanceof TransitStopVertex tsv) {
      var stop = Objects.requireNonNull(siteResolver.getStop(tsv.getId()));
      return Place.forStop(stop, ViaLocationTypeMapper.map(viaLocations, stop));
    } else if (vertex instanceof VehicleRentalPlaceVertex) {
      return Place.forVehicleRentalPlace((VehicleRentalPlaceVertex) vertex);
    } else if (vertex instanceof VehicleParkingEntranceVertex) {
      return Place.forVehicleParkingEntrance((VehicleParkingEntranceVertex) vertex, state);
    } else if (vertex instanceof TemporaryStreetLocation temporaryStreetLocation) {
      return Place.normal(
        vertex,
        name,
        ViaLocationTypeMapper.map(viaLocations, temporaryStreetLocation)
      );
    } else {
      return Place.normal(vertex, name);
    }
  }

  /**
   * Generate a flex leg from the states belonging to the flex leg
   */
  private Leg generateFlexLeg(StreetPath path, @Nullable Duration delay) {
    var states = path.states();
    State fromState = states.get(0);
    State toState = states.get(1);
    FlexTripEdge flexEdge = (FlexTripEdge) toState.backEdge;
    int generalizedCost = IntUtils.round(toState.getWeight() - fromState.getWeight());

    return FlexibleTransitLeg.of()
      .withFlexTripEdge(flexEdge)
      .withFromStop(siteResolver.getStopLocation(flexEdge.fromStopId()))
      .withToStop(siteResolver.getStopLocation(flexEdge.toStopId()))
      .withStartTime(getTimeWithDelay(fromState, delay))
      .withEndTime(getTimeWithDelay(toState, delay))
      .withGeneralizedCost(generalizedCost)
      .build();
  }

  /**
   * Generate one leg of an itinerary from a list of {@link State}.
   *
   * @param subPath       The street path to base the leg on
   * @param previousStep the previous walk step, so that the first relative turn direction is
   *                     calculated correctly
   * @return The generated leg
   */
  private StreetLeg generateLeg(
    StreetPath subPath,
    WalkStep previousStep,
    List<ViaLocation> viaLocations,
    @Nullable Duration delay
  ) {
    var states = subPath.states();

    State firstState = states.get(0);
    State lastState = states.get(states.size() - 1);

    var statesToWalkStepsMapper = new StatesToWalkStepsMapper(
      states,
      previousStep,
      streetNotesService,
      streetDetailsService,
      siteResolver,
      ellipsoidToGeoidDifference
    );
    List<WalkStep> walkSteps = statesToWalkStepsMapper.generateWalkSteps();

    /* For the from/to vertices to be in the correct place for vehicle parking
     * the state for actually parking (traversing the VehicleParkEdge) is excluded
     * from the list of states.
     * This adds the time and cost for parking to the walking leg.
     */
    var previousStateIsVehicleParking =
      firstState.getBackState() != null && firstState.getBackEdge() instanceof VehicleParkingEdge;

    State startTimeState = previousStateIsVehicleParking ? firstState.getBackState() : firstState;
    var extraWeight = firstState.getWeight() - startTimeState.getWeight();

    StreetLegBuilder leg = StreetLeg.of()
      .withMode(resolveMode(states))
      .withStartTime(getTimeWithDelay(startTimeState, delay))
      .withEndTime(getTimeWithDelay(lastState, delay))
      .withFrom(makePlace(firstState, viaLocations))
      .withTo(makePlace(lastState, viaLocations))
      .withDistanceMeters(subPath.distanceMeters())
      .withGeneralizedCost(IntUtils.round(subPath.weight() + extraWeight))
      .withGeometry(subPath.geometry())
      .withElevationProfile(
        subPath.elevation(firstState.getRequest().geoidElevation(), ellipsoidToGeoidDifference)
      )
      .withWalkSteps(walkSteps)
      .withRentedVehicle(firstState.isRentingVehicle())
      .withWalkingBike(false);

    if (firstState.isRentingVehicle()) {
      String vehicleRentalNetwork = firstState.getVehicleRentalNetwork();
      if (vehicleRentalNetwork != null) {
        leg.withVehicleRentalNetwork(vehicleRentalNetwork);
      }
    }

    addStreetNotes(leg, states);

    return leg.build();
  }

  /**
   * Add mode and alerts fields to a {@link StreetLeg}.
   *
   * @param leg    The leg to add the mode and alerts to
   * @param states The states that go with the leg
   */
  private void addStreetNotes(StreetLegBuilder leg, List<State> states) {
    for (State state : states) {
      Set<StreetNote> streetNotes = streetNotesService.getNotes(state);

      if (streetNotes != null) {
        leg.withStreetNotes(streetNotes);
      }
    }
  }

  private ZonedDateTime getTimeWithDelay(State state, @Nullable Duration delay) {
    if (delay != null) {
      return state.getTime().atZone(timeZone).plus(delay);
    }
    return state.getTime().atZone(timeZone);
  }
}
