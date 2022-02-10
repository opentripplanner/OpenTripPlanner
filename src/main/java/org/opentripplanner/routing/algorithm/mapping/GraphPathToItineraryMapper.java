package org.opentripplanner.routing.algorithm.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.edgetype.VehicleRentalEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO OTP2 There is still a lot of transit-related logic here that should be removed. We also need
//      to decide where real-time updates should be applied to the itinerary.
/**
 * A library class with only static methods used in converting internal GraphPaths to TripPlans, which are
 * returned by the OTP "planner" web service. TripPlans are made up of Itineraries, so the functions to produce them
 * are also bundled together here. This only produces itineraries for non-transit searches, as well as
 * the non-transit parts of itineraries containing transit, while the whole transit itinerary is produced
 * by {@link RaptorPathToItineraryMapper}.
 */
public abstract class GraphPathToItineraryMapper {

    private static final Logger LOG = LoggerFactory.getLogger(GraphPathToItineraryMapper.class);

    /**
     * Generates a TripPlan from a set of paths
     */
    public static List<Itinerary> mapItineraries(List<GraphPath> paths) {

        List<Itinerary> itineraries = new LinkedList<>();
        for (GraphPath path : paths) {
            Itinerary itinerary = generateItinerary(path);
            if (itinerary.legs.isEmpty()) { continue; }
            itineraries.add(itinerary);
        }

        return itineraries;
    }

    /**
     * Generate an itinerary from a {@link GraphPath}. This method first slices the list of states
     * at the leg boundaries. These smaller state arrays are then used to generate legs. Finally the
     * rest of the itinerary is generated based on the complete state array.
     *
     * @param path The graph path to base the itinerary on
     * @return The generated itinerary
     */
    public static Itinerary generateItinerary(GraphPath path) {

        State[] states = new State[path.states.size()];
        State lastState = path.states.getLast();
        states = path.states.toArray(states);

        Edge[] edges = new Edge[path.edges.size()];
        edges = path.edges.toArray(edges);

        Graph graph = path.getRoutingContext().graph;

        State[][] legsStates = sliceStates(states);

        List<Leg> legs = new ArrayList<>();
        WalkStep previousStep = null;

        for (State[] legStates : legsStates) {
            if (OTPFeature.FlexRouting.isOn() && legStates[1].backEdge instanceof FlexTripEdge) {
                legs.add(generateFlexLeg(graph, Arrays.asList(legStates)));
                previousStep = null;
                continue;
            }
            StreetLeg leg = generateLeg(graph, legStates, previousStep);
            legs.add(leg);

            List<WalkStep> walkSteps = leg.getWalkSteps();
            if (walkSteps.size() > 0) {
                previousStep = walkSteps.get(walkSteps.size() - 1);
            }
            else {
                previousStep = null;
            }
        }

        Itinerary itinerary = new Itinerary(legs);

        calculateElevations(itinerary, edges);

        itinerary.generalizedCost = (int) lastState.weight;
        itinerary.arrivedAtDestinationWithRentedVehicle = lastState.isRentingVehicleFromStation();

        return itinerary;
    }

    private static Calendar makeCalendar(State state) {
        RoutingContext rctx = state.getContext();
        TimeZone timeZone = rctx.graph.getTimeZone();
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(state.getTimeInMillis());
        return calendar;
    }

    /**
     * Generate a {@link CoordinateArrayListSequence} based on an {@link Edge} array.
     *
     * @param edges The array of input edges
     * @return The coordinates of the points on the edges
     */
    private static CoordinateArrayListSequence makeCoordinates(List<Edge> edges) {
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();

        for (Edge edge : edges) {
            LineString geometry = edge.getGeometry();

            if (geometry != null) {
                if (coordinates.size() == 0) {
                    coordinates.extend(geometry.getCoordinates());
                } else {
                    coordinates.extend(geometry.getCoordinates(), 1); // Avoid duplications
                }
            }
        }

        return coordinates;
    }

    /**
     * Slice a {@link State} array at the leg boundaries. Leg switches occur when the mode changes,
     * for instance from BICYCLE to WALK.
     *
     * @param states The one-dimensional array of input states
     * @return An array of arrays of states belonging to a single leg (i.e. a two-dimensional array)
     */
    private static State[][] sliceStates(State[] states) {
        boolean trivial = true;

        for (State state : states) {
            TraverseMode traverseMode = state.getBackMode();

            if (traverseMode != null) {
                trivial = false;
                break;
            }
        }

        if (trivial) {
            return new State[][]{};
        }

        int[] legIndexPairs = {0, states.length - 1};
        List<int[]> legsIndexes = new ArrayList<int[]>();

        TraverseMode lastMode = null;
        for (int i = 1; i < states.length - 1; i++) {
            var backState = states[i];
            var forwardState = states[i + 1];
            var backMode = backState.getBackMode();
            var forwardMode = forwardState.getBackMode();

            if (backMode != null) {
                lastMode = backMode;
            }

            var modeChange = lastMode != forwardMode && lastMode != null && forwardMode != null;
            var rentalChange = isRentalPickUp(backState) || isRentalDropOff(backState);
            var parkingChange = backState.isVehicleParked() != forwardState.isVehicleParked();

            if (parkingChange) {
                /* Remove the state for actually parking (traversing VehicleParkingEdge) from the
                 * states so that the leg from/to edges correspond to the actual entrances.
                 * The actual time for parking is added to the walking leg in generateLeg().
                 */
                legIndexPairs[1] = i;
                legsIndexes.add(legIndexPairs);
                legIndexPairs = new int[] {i + 1, states.length - 1};
            }
            else if (modeChange || rentalChange) {
                legIndexPairs[1] = i;
                legsIndexes.add(legIndexPairs);
                legIndexPairs = new int[] {i, states.length - 1};
            }

            if (rentalChange || parkingChange) {
                /* Clear the lastMode, so that switching modes doesn't re-trigger a mode change
                 * a few states latter. */
                lastMode = null;
            }
        }

        // Final leg
        legsIndexes.add(legIndexPairs);

        State[][] legsStates = new State[legsIndexes.size()][];

        // Fill the two-dimensional array with states
        for (int i = 0; i < legsStates.length; i++) {
            legIndexPairs = legsIndexes.get(i);
            legsStates[i] = new State[legIndexPairs[1] - legIndexPairs[0] + 1];
            if (legIndexPairs[1] - legIndexPairs[0] + 1 >= 0)
                System.arraycopy(
                        states, legIndexPairs[0], legsStates[i], 0,
                        legIndexPairs[1] - legIndexPairs[0] + 1
                );
        }

        return legsStates;
    }

    /**
     * Generate a flex leg from the states belonging to the flex leg
     */
    private static Leg generateFlexLeg(Graph graph, List<State> states) {
        State fromState = states.get(0);
        State toState = states.get(1);
        FlexTripEdge flexEdge = (FlexTripEdge) toState.backEdge;
        Calendar startTime = makeCalendar(fromState);
        Calendar endTime = makeCalendar(toState);
        int generalizedCost = (int) (toState.getWeight() - fromState.getWeight());

        Leg leg = new FlexibleTransitLeg(flexEdge, startTime, endTime, generalizedCost);

        AlertToLegMapper.addTransitAlertPatchesToLeg(graph, leg, true);
        return leg;
    }

    /**
     * Generate one leg of an itinerary from a list of {@link State}.
     *
     * @param states The list of states to base the leg on
     * @param previousStep the previous walk step, so that the first relative turn direction is
     *                     calculated correctly
     * @return The generated leg
     */
    private static StreetLeg generateLeg(Graph graph, State[] states, WalkStep previousStep) {
        List<Edge> edges = Arrays.stream(states)
                // The first back edge is part of the previous leg, skip it
                .skip(1)
                .map(State::getBackEdge)
                .collect(Collectors.toList());

        State firstState = states[0];
        State lastState = states[states.length - 1];

        double distanceMeters = edges.stream().mapToDouble(Edge::getDistanceMeters).sum();

        CoordinateArrayListSequence coordinates = makeCoordinates(edges);
        LineString geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);

        List<WalkStep> walkSteps = new StatesToWalkStepsMapper(graph, Arrays.asList(states), previousStep).generateWalkSteps();

        /* For the from/to vertices to be in the correct place for vehicle parking
         * the state for actually parking (traversing the VehicleParkEdge) is excluded
         * from the list of states.
         * This adds the time for parking to the walking leg.
         */
        var previousStateIsVehicleParking = firstState.getBackState() != null
                && firstState.getBackEdge() instanceof VehicleParkingEdge;

        Calendar startTime = makeCalendar(
                previousStateIsVehicleParking ? firstState.getBackState() : firstState
        );

        StreetLeg leg = new StreetLeg(
                resolveMode(states),
                startTime,
                makeCalendar(lastState),
                makePlace(firstState),
                makePlace(lastState),
                distanceMeters,
                (int) (lastState.getWeight() - firstState.getWeight()),
                geometry,
                walkSteps
        );

        leg.setWalkingBike(states[states.length - 1].isBackWalkingBike());
        leg.setRentedVehicle(firstState.isRentingVehicle());

        if (leg.getRentedVehicle()) {
            String vehicleRentalNetwork = firstState.getVehicleRentalNetwork();
            if (vehicleRentalNetwork != null) {
                leg.setVehicleRentalNetwork(vehicleRentalNetwork);
            }
        }

        addStreetNotes(graph, leg, states);

        setPathwayInfo(leg, states);

        return leg;
    }

    /**
     * TODO: This is mindless. Why is this set on leg, rather than on a walk step? Now only the first pathway is used
     */
    private static void setPathwayInfo(StreetLeg leg, State[] legsStates) {
        for (State legsState : legsStates) {
            if (legsState.getBackEdge() instanceof PathwayEdge) {
                PathwayEdge pe = (PathwayEdge) legsState.getBackEdge();
                leg.setPathwayId(pe.getId());
                return;
            }
        }
    }

    /**
     * Calculate the elevationGained and elevationLost fields of an {@link Itinerary}.
     *
     * @param itinerary The itinerary to calculate the elevation changes for
     * @param edges The edges that go with the itinerary
     */
    private static void calculateElevations(Itinerary itinerary, Edge[] edges) {
        for (Edge edge : edges) {
            if (!(edge instanceof StreetEdge)) continue;

            StreetEdge edgeWithElevation = (StreetEdge) edge;
            PackedCoordinateSequence coordinates = edgeWithElevation.getElevationProfile();

            if (coordinates == null) continue;
            // TODO Check the test below, AFAIU current elevation profile has 3 dimensions.
            if (coordinates.getDimension() != 2) continue;

            for (int i = 0; i < coordinates.size() - 1; i++) {
                double change = coordinates.getOrdinate(i + 1, 1) - coordinates.getOrdinate(i, 1);

                if (change > 0) {
                    itinerary.elevationGained += change;
                } else if (change < 0) {
                    itinerary.elevationLost -= change;
                }
            }
        }
    }

    /**
     * Resolve mode from states.
     * @param states The states that go with the leg
     */
    private static TraverseMode resolveMode(State[] states) {
        TraverseMode returnMode = TraverseMode.WALK;

        for (State state : states) {
            TraverseMode mode = state.getBackMode();

            if (mode != null) {
                // Resolve correct mode if renting vehicle, and is not walking with it
                if (state.isRentingVehicle() && !state.isBackWalkingBike()) {
                    switch (state.stateData.rentalVehicleFormFactor) {
                        case BICYCLE:
                        case OTHER:
                            returnMode = TraverseMode.BICYCLE;
                            continue;
                        case SCOOTER:
                        case MOPED:
                            returnMode = TraverseMode.SCOOTER;
                            continue;
                        case CAR:
                            returnMode = TraverseMode.CAR;
                            continue;
                    }
                }
                returnMode = mode;
            }
        }
        return returnMode;
    }

    /**
     * Add mode and alerts fields to a {@link Leg}.
     *
     * @param leg The leg to add the mode and alerts to
     * @param states The states that go with the leg
     */
    private static void addStreetNotes(Graph graph, StreetLeg leg, State[] states) {
        for (State state : states) {
            Set<StreetNote> streetNotes = graph.streetNotesService.getNotes(state);

            if (streetNotes != null) {
                for (StreetNote streetNote : streetNotes) {
                    leg.addStretNote(streetNote);
                }
            }
        }
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
        } else if(vertex instanceof VehicleRentalStationVertex) {
            return Place.forVehicleRentalPlace((VehicleRentalStationVertex) vertex);
        } else if (vertex instanceof VehicleParkingEntranceVertex) {
            return Place.forVehicleParkingEntrance((VehicleParkingEntranceVertex) vertex, state.getOptions());
        } else {
            return Place.normal(vertex, name);
        }
    }

    public static boolean isRentalPickUp(State state) {
        return state.getBackEdge() instanceof VehicleRentalEdge && (state.getBackState() == null || !state.getBackState()
                .isRentingVehicle());
    }

    public static boolean isRentalDropOff(State state) {
        return state.getBackEdge() instanceof VehicleRentalEdge && state.getBackState().isRentingVehicle();
    }
}
