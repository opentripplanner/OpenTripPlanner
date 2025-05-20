package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.time.ZoneIdFallback;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItineraryBuilder;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.ElevationProfile;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.leg.StreetLegBuilder;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;

/**
 * A mapper class used in converting internal GraphPaths to Itineraries, which are returned by the
 * OTP APIs. This only produces itineraries for non-transit searches, as well as the non-transit
 * parts of itineraries containing transit, while the whole transit itinerary is produced by
 * {@link RaptorPathToItineraryMapper}.
 */
public class GraphPathToItineraryMapper {

  private final ZoneId timeZone;
  private final StreetNotesService streetNotesService;
  private final double ellipsoidToGeoidDifference;

  public GraphPathToItineraryMapper(
    ZoneId timeZone,
    StreetNotesService streetNotesService,
    double ellipsoidToGeoidDifference
  ) {
    this.timeZone = ZoneIdFallback.zoneId(timeZone);
    this.streetNotesService = streetNotesService;
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
   * Generates a TripPlan from a set of paths
   */
  public List<Itinerary> mapItineraries(List<GraphPath<State, Edge, Vertex>> paths) {
    List<Itinerary> itineraries = new LinkedList<>();
    for (GraphPath<State, Edge, Vertex> path : paths) {
      Itinerary itinerary = generateItinerary(path);
      if (itinerary.legs().isEmpty()) {
        continue;
      }
      itineraries.add(itinerary);
    }

    return itineraries;
  }

  /**
   * Generate an itinerary from a {@link GraphPath}. This method first slices the list of states at
   * the leg boundaries. These smaller state arrays are then used to generate legs.
   *
   * @param path The graph path to base the itinerary on
   * @return The generated itinerary
   */
  public Itinerary generateItinerary(GraphPath<State, Edge, Vertex> path) {
    List<Leg> legs = new ArrayList<>();
    WalkStep previousStep = null;
    for (List<State> legStates : sliceStates(path.states)) {
      if (OTPFeature.FlexRouting.isOn() && legStates.get(1).backEdge instanceof FlexTripEdge) {
        legs.add(generateFlexLeg(legStates));
        previousStep = null;
        continue;
      }
      StreetLeg leg = generateLeg(legStates, previousStep);
      legs.add(leg);

      List<WalkStep> walkSteps = leg.listWalkSteps();
      if (walkSteps.size() > 0) {
        previousStep = walkSteps.get(walkSteps.size() - 1);
      } else {
        previousStep = null;
      }
    }

    State lastState = path.states.getLast();
    var cost = Cost.costOfSeconds(lastState.weight);
    var builder = Itinerary.ofDirect(legs).withGeneralizedCost(cost);

    builder.withArrivedAtDestinationWithRentedVehicle(lastState.isRentingVehicleFromStation());

    calculateElevations(builder, path.edges);

    return builder.build();
  }

  /**
   * Slice a {@link State} list at the leg boundaries.
   *
   * @param states The list of input states
   * @return A list of lists of states belonging to a single leg
   */
  private static List<List<State>> sliceStates(List<State> states) {
    // Trivial case
    if (states.stream().allMatch(state -> state.getBackMode() == null)) {
      return List.of();
    }

    List<List<State>> legsStates = new LinkedList<>();

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
          legsStates.add(states.subList(previousBreak, nextBreak + 1));
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
      legsStates.add(states.subList(previousBreak, states.size()));
    }

    return legsStates;
  }

  /**
   * Calculate the elevationGained and elevationLost fields of an {@link Itinerary}.
   *
   * @param itinerary The itinerary to calculate the elevation changes for
   * @param edges     The edges that go with the itinerary
   */
  private static void calculateElevations(ItineraryBuilder builder, List<Edge> edges) {
    for (Edge edge : edges) {
      if (!(edge instanceof StreetEdge edgeWithElevation)) {
        continue;
      }
      PackedCoordinateSequence coordinates = edgeWithElevation.getElevationProfile();

      if (coordinates == null) continue;
      // TODO Check the test below, AFAIU current elevation profile has 3 dimensions.
      if (coordinates.getDimension() != 2) continue;

      for (int i = 0; i < coordinates.size() - 1; i++) {
        double change = coordinates.getOrdinate(i + 1, 1) - coordinates.getOrdinate(i, 1);
        builder.addElevationChange(change);
      }
    }
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

  private static ElevationProfile encodeElevationProfileWithNaN(
    Edge edge,
    double distanceOffset,
    double heightOffset
  ) {
    var elevations = encodeElevationProfile(edge, distanceOffset, heightOffset);
    if (elevations.isEmpty()) {
      return ElevationProfile.of()
        .stepYUnknown(distanceOffset)
        .stepYUnknown(distanceOffset + edge.getDistanceMeters())
        .build();
    }
    return elevations;
  }

  private static ElevationProfile encodeElevationProfile(
    Edge edge,
    double distanceOffset,
    double heightOffset
  ) {
    if (!(edge instanceof StreetEdge elevEdge)) {
      return ElevationProfile.empty();
    }
    if (elevEdge.getElevationProfile() == null) {
      return ElevationProfile.empty();
    }

    var out = ElevationProfile.of();
    Coordinate[] coordArr = elevEdge.getElevationProfile().toCoordinateArray();
    for (final Coordinate coordinate : coordArr) {
      out.step(coordinate.x + distanceOffset, coordinate.y + heightOffset);
    }

    return out.build();
  }

  /**
   * Make a {@link Place} to add to a {@link Leg}.
   *
   * @param state The {@link State}.
   * @return The resulting {@link Place} object.
   */
  private static Place makePlace(State state) {
    Vertex vertex = state.getVertex();
    I18NString name = vertex.getName();

    //This gets nicer names instead of osm:node:id when changing mode of transport
    //Names are generated from all the streets in a corner, same as names in origin and destination
    //We use name in TemporaryStreetLocation since this name generation already happened when temporary location was generated
    if (vertex instanceof StreetVertex && !(vertex instanceof TemporaryStreetLocation)) {
      name = ((StreetVertex) vertex).getIntersectionName();
    }

    if (vertex instanceof TransitStopVertex) {
      return Place.forStop(((TransitStopVertex) vertex).getStop());
    } else if (vertex instanceof VehicleRentalPlaceVertex) {
      return Place.forVehicleRentalPlace((VehicleRentalPlaceVertex) vertex);
    } else if (vertex instanceof VehicleParkingEntranceVertex) {
      return Place.forVehicleParkingEntrance((VehicleParkingEntranceVertex) vertex, state);
    } else {
      return Place.normal(vertex, name);
    }
  }

  /**
   * Generate a flex leg from the states belonging to the flex leg
   */
  private Leg generateFlexLeg(List<State> states) {
    State fromState = states.get(0);
    State toState = states.get(1);
    FlexTripEdge flexEdge = (FlexTripEdge) toState.backEdge;
    ZonedDateTime startTime = fromState.getTime().atZone(timeZone);
    ZonedDateTime endTime = toState.getTime().atZone(timeZone);
    int generalizedCost = (int) (toState.getWeight() - fromState.getWeight());

    return FlexibleTransitLeg.of()
      .withFlexTripEdge(flexEdge)
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withGeneralizedCost(generalizedCost)
      .build();
  }

  /**
   * Generate one leg of an itinerary from a list of {@link State}.
   *
   * @param states       The list of states to base the leg on
   * @param previousStep the previous walk step, so that the first relative turn direction is
   *                     calculated correctly
   * @return The generated leg
   */
  private StreetLeg generateLeg(List<State> states, WalkStep previousStep) {
    List<Edge> edges = states
      .stream()
      // The first back edge is part of the previous leg, skip it
      .skip(1)
      // when linking an OSM boarding location, like a platform centroid, we create a link edge
      // so we can see it in the debug UI's traversal permission layer but we don't want to show the
      // link to the user so we remove it here
      .filter(e -> !(e.backEdge instanceof BoardingLocationToStopLink))
      .map(State::getBackEdge)
      .toList();

    State firstState = states.get(0);
    State lastState = states.get(states.size() - 1);

    double distanceMeters = edges.stream().mapToDouble(Edge::getDistanceMeters).sum();

    LineString geometry = GeometryUtils.concatenateLineStrings(edges, Edge::getGeometry);

    var statesToWalkStepsMapper = new StatesToWalkStepsMapper(
      states,
      previousStep,
      streetNotesService,
      ellipsoidToGeoidDifference
    );
    List<WalkStep> walkSteps = statesToWalkStepsMapper.generateWalkSteps();

    /* For the from/to vertices to be in the correct place for vehicle parking
     * the state for actually parking (traversing the VehicleParkEdge) is excluded
     * from the list of states.
     * This adds the time for parking to the walking leg.
     */
    var previousStateIsVehicleParking =
      firstState.getBackState() != null && firstState.getBackEdge() instanceof VehicleParkingEdge;

    State startTimeState = previousStateIsVehicleParking ? firstState.getBackState() : firstState;

    StreetLegBuilder leg = StreetLeg.of()
      .withMode(resolveMode(states))
      .withStartTime(startTimeState.getTime().atZone(timeZone))
      .withEndTime(lastState.getTime().atZone(timeZone))
      .withFrom(makePlace(firstState))
      .withTo(makePlace(lastState))
      .withDistanceMeters(distanceMeters)
      .withGeneralizedCost((int) (lastState.getWeight() - firstState.getWeight()))
      .withGeometry(geometry)
      .withElevationProfile(
        makeElevation(edges, firstState.getPreferences().system().geoidElevation())
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

  private ElevationProfile makeElevation(List<Edge> edges, boolean geoidElevation) {
    var builder = ElevationProfile.of();

    double heightOffset = geoidElevation ? ellipsoidToGeoidDifference : 0;

    double distanceOffset = 0;
    for (final Edge edge : edges) {
      if (edge.getDistanceMeters() > 0) {
        builder.add(encodeElevationProfileWithNaN(edge, distanceOffset, heightOffset));
        distanceOffset += edge.getDistanceMeters();
      }
    }

    var p = builder.build();

    return p.isAllYUnknown() ? null : p;
  }
}
