package org.opentripplanner.routing.algorithm.mapping;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.ext.flex.FlexLegMapper;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.model.VehicleRentalStationInfo;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.ElevatorAlightEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.edgetype.VehicleRentalEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.ExitVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.PolylineEncoder;
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
    private static final double MAX_ZAG_DISTANCE = 30; // TODO add documentation, what is a "zag"?

    /**
     * Generates a TripPlan from a set of paths
     */
    public static List<Itinerary> mapItineraries(List<GraphPath> paths, RoutingRequest request) {

        List<Itinerary> itineraries = new LinkedList<>();
        for (GraphPath path : paths) {
            Itinerary itinerary = generateItinerary(path, request.locale);
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
    public static Itinerary generateItinerary(GraphPath path, Locale requestedLocale) {

        State[] states = new State[path.states.size()];
        State lastState = path.states.getLast();
        states = path.states.toArray(states);

        Edge[] edges = new Edge[path.edges.size()];
        edges = path.edges.toArray(edges);

        Graph graph = path.getRoutingContext().graph;

        State[][] legsStates = sliceStates(states);

        List<Leg> legs = new ArrayList<>();
        for (State[] legStates : legsStates) {
            legs.add(generateLeg(graph, legStates, requestedLocale));
        }

        addWalkSteps(graph, legs, legsStates, requestedLocale);


        boolean first = true;
        for (Leg leg : legs) {
            AlertToLegMapper.addTransitAlertPatchesToLeg(graph, leg, first, requestedLocale);
            first = false;
        }

        setPathwayInfo(legs, legsStates);

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
    private static CoordinateArrayListSequence makeCoordinates(Edge[] edges) {
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
     * Generate one leg of an itinerary from a {@link State} array.
     *
     * @param states The array of states to base the leg on
     * @return The generated leg
     */
    private static Leg generateLeg(Graph graph, State[] states, Locale requestedLocale) {
        Leg leg = null;
        FlexTripEdge flexEdge = null;

        if (OTPFeature.FlexRouting.isOn()) {
            flexEdge = (FlexTripEdge) Stream
                .of(states)
                .skip(1)
                .map(state -> state.backEdge)
                .filter(edge -> edge instanceof FlexTripEdge)
                .findFirst()
                .orElse(null);
            if (flexEdge != null) {
                leg = new Leg(flexEdge.getTrip());
                leg.flexibleTrip = true;
            }
        }

        if (leg == null) {
            leg = new Leg(resolveMode(states));
        }

        Edge[] edges = new Edge[states.length - 1];

        leg.startTime = makeCalendar(states[0]);
        leg.endTime = makeCalendar(states[states.length - 1]);

        // Calculate leg distance and fill array of edges
        leg.distanceMeters = 0.0;
        for (int i = 0; i < edges.length; i++) {
            edges[i] = states[i + 1].getBackEdge();
            leg.distanceMeters += edges[i].getDistanceMeters();
        }

        TimeZone timeZone = leg.startTime.getTimeZone();
        leg.agencyTimeZoneOffset = timeZone.getOffset(leg.startTime.getTimeInMillis());

        if (flexEdge != null) {
            FlexLegMapper.addFlexPlaces(leg, flexEdge, requestedLocale);
        } else {
            addPlaces(leg, states, requestedLocale);
        }

        CoordinateArrayListSequence coordinates = makeCoordinates(edges);
        Geometry geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);

        leg.legGeometry = PolylineEncoder.createEncodings(geometry);

        leg.generalizedCost = (int) (states[states.length - 1].getWeight() - states[0].getWeight());

        leg.walkingBike = states[states.length - 1].isBackWalkingBike();

        leg.rentedVehicle = states[0].isRentingVehicle();

        if (leg.rentedVehicle) {
            String vehicleRentalNetwork = states[0].getVehicleRentalNetwork();
            if (vehicleRentalNetwork != null) {
                leg.setVehicleRentalNetwork(vehicleRentalNetwork);
            }
        }

        addStreetNotes(graph, leg, states);

        if (flexEdge != null) {
            FlexLegMapper.fixFlexTripLeg(leg, flexEdge);
        }

        /* For the from/to vertices to be in the correct place for vehicle parking
         * the state for actually parking (traversing the VehicleParkEdge) is excluded
         * from the list of states.
         * This add the time for parking to the walking leg.
         */
        var previousStateIsVehicleParking = states[0].getBackState() != null
                && states[0].getBackEdge() instanceof VehicleParkingEdge;
        if (previousStateIsVehicleParking) {
            leg.startTime = makeCalendar(states[0].getBackState());
        }

        return leg;
    }

    /**
     * Add a {@link WalkStep} {@link List} to a {@link Leg} {@link List}.
     * It's more convenient to process all legs in one go because the previous step should be kept.
     *
     * @param legs The legs of the itinerary
     * @param legsStates The states that go with the legs
     */
    private static void addWalkSteps(Graph graph, List<Leg> legs, State[][] legsStates, Locale requestedLocale) {
        WalkStep previousStep = null;

        TraverseMode lastMode = null;

        for (int i = 0; i < legsStates.length; i++) {
            List<WalkStep> walkSteps = generateWalkSteps(graph, legsStates[i], previousStep, requestedLocale);
            TraverseMode legMode = legs.get(i).mode;
            if(legMode != lastMode && !walkSteps.isEmpty()) {
                lastMode = legMode;
            }

            legs.get(i).walkSteps = walkSteps;

            if (walkSteps.size() > 0) {
                previousStep = walkSteps.get(walkSteps.size() - 1);
            } else {
                previousStep = null;
            }
        }
    }

    /**
     * This was originally in TransitUtils.handleBoardAlightType.
     * Edges that always block traversal (forbidden pickups/dropoffs) are simply not ever created.
     */
    public static String getBoardAlightMessage (int boardAlightType) {
        switch (boardAlightType) {
        case 1:
            return "impossible";
        case 2:
            return "mustPhone";
        case 3:
            return "coordinateWithDriver";
        default:
            return null;
        }
    }

    private static void setPathwayInfo(List<Leg> legs, State[][] legsStates) {
        OUTER:
        for (int i = 0; i < legsStates.length; i++) {
            for (int j = 1; j < legsStates[i].length; j++) {
                if (legsStates[i][j].getBackEdge() instanceof PathwayEdge) {
                    PathwayEdge pe = (PathwayEdge) legsStates[i][j].getBackEdge();
                    legs.get(i).pathwayId = pe.getId();
                    legs.get(i).pathway = true;
                    break OUTER;
                }
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
    private static void addStreetNotes(Graph graph, Leg leg, State[] states) {
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
     * Add {@link Place} fields to a {@link Leg}.
     *
     * @param leg The leg to add the places to
     * @param states The states that go with the leg
     */
    private static void addPlaces(Leg leg, State[] states, Locale requestedLocale) {
        leg.from = makePlace(states[0], requestedLocale);
        leg.to = makePlace(states[states.length - 1], requestedLocale);
    }

    /**
     * Make a {@link Place} to add to a {@link Leg}.
     *
     * @param state The {@link State}.
     * @param requestedLocale The locale to use for all text attributes.
     * @return The resulting {@link Place} object.
     */
    private static Place makePlace(State state, Locale requestedLocale) {
        Vertex vertex = state.getVertex();
        String name = vertex.getName(requestedLocale);

        //This gets nicer names instead of osm:node:id when changing mode of transport
        //Names are generated from all the streets in a corner, same as names in origin and destination
        //We use name in TemporaryStreetLocation since this name generation already happened when temporary location was generated
        if (vertex instanceof StreetVertex && !(vertex instanceof TemporaryStreetLocation)) {
            name = ((StreetVertex) vertex).getIntersectionName(requestedLocale).toString(requestedLocale);
        }

        if (vertex instanceof TransitStopVertex) {
            return Place.forStop((TransitStopVertex) vertex, name);
        } else if(vertex instanceof VehicleRentalStationVertex) {
            return Place.forVehicleRentalPlace((VehicleRentalStationVertex) vertex, name);
        } else if (vertex instanceof VehicleParkingEntranceVertex) {
            var vehicleParking = ((VehicleParkingEntranceVertex) vertex).getVehicleParking();
            var limit = state.getTimeAsZonedDateTime()
                    .plusSeconds(state.getOptions().vehicleParkingClosesSoonSeconds);
            var closesSoon = false;
            if (vehicleParking.getOpeningHours() != null) {
                // This ignores the parking being closed for less than vehicleParkingClosesSoonSeconds
                closesSoon = !vehicleParking.getOpeningHours()
                        .isTraverseableAt(limit.toLocalDateTime());
            }
            return Place.forVehicleParkingEntrance(
                    (VehicleParkingEntranceVertex) vertex, name, closesSoon, state.getOptions());
        } else {
            return Place.normal(vertex, name);
        }
    }

    /**
     * Converts a list of street edges to a list of turn-by-turn directions.
     * 
     * @param previous a non-transit leg that immediately precedes this one (bike-walking, say), or null
     */
    public static List<WalkStep> generateWalkSteps(Graph graph, State[] states, WalkStep previous, Locale requestedLocale) {
        List<WalkStep> steps = new ArrayList<>();
        WalkStep step = null;
        double lastAngle = 0, distance = 0; // distance used for appending elevation profiles
        int roundaboutExit = 0; // track whether we are in a roundabout, and if so the exit number
        String roundaboutPreviousStreet = null;

        State onVehicleRentalState = null, offVehicleRentalState = null;

        if (isRentalPickUp(states[states.length - 1])) {
            onVehicleRentalState = states[states.length - 1];
        }
        if (isRentalDropOff(states[0])) {
            offVehicleRentalState = states[0];
        }

        for (int i = 0; i < states.length - 1; i++) {
            State backState = states[i];
            State forwardState = states[i + 1];
            Edge edge = forwardState.getBackEdge();

            boolean createdNewStep = false, disableZagRemovalForThisStep = false;
            if (edge instanceof FreeEdge) {
                continue;
            }
            if (forwardState.getBackMode() == null || !forwardState.getBackMode().isOnStreetNonTransit()) {
                continue; // ignore STLs and the like
            }
            Geometry geom = edge.getGeometry();
            if (geom == null) {
                continue;
            }

            // generate a step for getting off an elevator (all
            // elevator narrative generation occurs when alighting). We don't need to know what came
            // before or will come after
            if (edge instanceof ElevatorAlightEdge) {
                // don't care what came before or comes after
                step = createWalkStep(graph, forwardState, backState, requestedLocale);
                createdNewStep = true;
                disableZagRemovalForThisStep = true;

                // tell the user where to get off the elevator using the exit notation, so the
                // i18n interface will say 'Elevator to <exit>'
                // what happens is that the webapp sees name == null and ignores that, and it sees
                // exit != null and uses to <exit>
                // the floor name is the AlightEdge name
                // reset to avoid confusion with 'Elevator on floor 1 to floor 1'
                step.streetName = ((ElevatorAlightEdge) edge).getName(requestedLocale);

                step.relativeDirection = RelativeDirection.ELEVATOR;

                steps.add(step);
                continue;
            }

            String streetName = edge.getName(requestedLocale);
            int idx = streetName.indexOf('(');
            String streetNameNoParens;
            if (idx > 0)
                streetNameNoParens = streetName.substring(0, idx - 1);
            else
                streetNameNoParens = streetName;

            if (step == null) {
                // first step
                step = createWalkStep(graph, forwardState, backState, requestedLocale);
                createdNewStep = true;

                steps.add(step);
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                if (previous == null) {
                    step.setAbsoluteDirection(thisAngle);
                    step.relativeDirection = RelativeDirection.DEPART;
                } else {
                    step.setDirections(previous.angle, thisAngle, false);
                }
                // new step, set distance to length of first edge
                distance = edge.getDistanceMeters();
            } else if (((step.streetName != null && !step.streetNameNoParens().equals(streetNameNoParens))
                    && (!step.bogusName || !edge.hasBogusName())) ||
                    edge.isRoundabout() != (roundaboutExit > 0) || // went on to or off of a roundabout
                    isLink(edge) && !isLink(backState.getBackEdge())) {
                // Street name has changed, or we've gone on to or off of a roundabout.
                if (roundaboutExit > 0) {
                    // if we were just on a roundabout,
                    // make note of which exit was taken in the existing step
                    step.exit = Integer.toString(roundaboutExit); // ordinal numbers from
                    if (streetNameNoParens.equals(roundaboutPreviousStreet)) {
                        step.stayOn = true;
                    }
                    roundaboutExit = 0;
                }
                /* start a new step */
                step = createWalkStep(graph, forwardState, backState, requestedLocale);
                createdNewStep = true;

                steps.add(step);
                if (edge.isRoundabout()) {
                    // indicate that we are now on a roundabout
                    // and use one-based exit numbering
                    roundaboutExit = 1;
                    roundaboutPreviousStreet = backState.getBackEdge().getName(requestedLocale);
                    idx = roundaboutPreviousStreet.indexOf('(');
                    if (idx > 0)
                        roundaboutPreviousStreet = roundaboutPreviousStreet.substring(0, idx - 1);
                }
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setDirections(lastAngle, thisAngle, edge.isRoundabout());
                // new step, set distance to length of first edge
                distance = edge.getDistanceMeters();
            } else {
                /* street name has not changed */
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                RelativeDirection direction = WalkStep.getRelativeDirection(lastAngle, thisAngle,
                        edge.isRoundabout());
                boolean optionsBefore = backState.multipleOptionsBefore();
                if (edge.isRoundabout()) {
                    // we are on a roundabout, and have already traversed at least one edge of it.
                    if (optionsBefore) {
                        // increment exit count if we passed one.
                        roundaboutExit += 1;
                    }
                }
                if (edge.isRoundabout() || direction == RelativeDirection.CONTINUE) {
                    // we are continuing almost straight, or continuing along a roundabout.
                    // just append elevation info onto the existing step.

                } else {
                    // we are not on a roundabout, and not continuing straight through.

                    // figure out if there were other plausible turn options at the last
                    // intersection
                    // to see if we should generate a "left to continue" instruction.
                    boolean shouldGenerateContinue = false;
                    if (edge instanceof StreetEdge) {
                        // the next edges will be PlainStreetEdges, we hope
                        double angleDiff = getAbsoluteAngleDiff(thisAngle, lastAngle);
                        for (Edge alternative : backState.getVertex().getOutgoingStreetEdges()) {
                            if (alternative.getName(requestedLocale).equals(streetName)) {
                                // alternatives that have the same name
                                // are usually caused by street splits
                                continue;
                            }
                            double altAngle = DirectionUtils.getFirstAngle(alternative
                                    .getGeometry());
                            double altAngleDiff = getAbsoluteAngleDiff(altAngle, lastAngle);
                            if (angleDiff > Math.PI / 4 || altAngleDiff - angleDiff < Math.PI / 16) {
                                shouldGenerateContinue = true;
                                break;
                            }
                        }
                    } else {
                        double angleDiff = getAbsoluteAngleDiff(lastAngle, thisAngle);
                        // FIXME: this code might be wrong with the removal of the edge-based graph
                        State twoStatesBack = backState.getBackState();
                        Vertex backVertex = twoStatesBack.getVertex();
                        for (Edge alternative : backVertex.getOutgoingStreetEdges()) {
                            List<Edge> alternatives = alternative.getToVertex()
                                    .getOutgoingStreetEdges();
                            if (alternatives.size() == 0) {
                                continue; // this is not an alternative
                            }
                            alternative = alternatives.get(0);
                            if (alternative.getName(requestedLocale).equals(streetName)) {
                                // alternatives that have the same name
                                // are usually caused by street splits
                                continue;
                            }
                            double altAngle = DirectionUtils.getFirstAngle(alternative
                                    .getGeometry());
                            double altAngleDiff = getAbsoluteAngleDiff(altAngle, lastAngle);
                            if (angleDiff > Math.PI / 4 || altAngleDiff - angleDiff < Math.PI / 16) {
                                shouldGenerateContinue = true;
                                break;
                            }
                        }
                    }

                    if (shouldGenerateContinue) {
                        // turn to stay on same-named street
                        step = createWalkStep(graph, forwardState, backState, requestedLocale);
                        createdNewStep = true;
                        steps.add(step);
                        step.setDirections(lastAngle, thisAngle, false);
                        step.stayOn = true;
                        // new step, set distance to length of first edge
                        distance = edge.getDistanceMeters();
                    }
                }
            }

            State exitState = backState;
            Edge exitEdge = exitState.getBackEdge();
            while (exitEdge instanceof FreeEdge) {
                exitState = exitState.getBackState();
                exitEdge = exitState.getBackEdge();
            }
            if (exitState.getVertex() instanceof ExitVertex) {
                step.exit = ((ExitVertex) exitState.getVertex()).getExitName();
            }

            if (createdNewStep && !disableZagRemovalForThisStep && forwardState.getBackMode() == backState.getBackMode()) {
                //check last three steps for zag
                int last = steps.size() - 1;
                if (last >= 2) {
                    WalkStep threeBack = steps.get(last - 2);
                    WalkStep twoBack = steps.get(last - 1);
                    WalkStep lastStep = steps.get(last);

                    if (twoBack.distance < MAX_ZAG_DISTANCE
                            && lastStep.streetNameNoParens().equals(threeBack.streetNameNoParens())) {
                        
                        if (((lastStep.relativeDirection == RelativeDirection.RIGHT ||
                                lastStep.relativeDirection == RelativeDirection.HARD_RIGHT) &&
                                (twoBack.relativeDirection == RelativeDirection.RIGHT ||
                                twoBack.relativeDirection == RelativeDirection.HARD_RIGHT)) ||
                                ((lastStep.relativeDirection == RelativeDirection.LEFT ||
                                lastStep.relativeDirection == RelativeDirection.HARD_LEFT) &&
                                (twoBack.relativeDirection == RelativeDirection.LEFT ||
                                twoBack.relativeDirection == RelativeDirection.HARD_LEFT))) {
                            // in this case, we have two left turns or two right turns in quick 
                            // succession; this is probably a U-turn.
                            
                            steps.remove(last - 1);
                            
                            lastStep.distance += twoBack.distance;
                            
                            // A U-turn to the left, typical in the US. 
                            if (lastStep.relativeDirection == RelativeDirection.LEFT ||
                                    lastStep.relativeDirection == RelativeDirection.HARD_LEFT)
                                lastStep.relativeDirection = RelativeDirection.UTURN_LEFT;
                            else
                                lastStep.relativeDirection = RelativeDirection.UTURN_RIGHT;
                            
                            // in this case, we're definitely staying on the same street 
                            // (since it's zag removal, the street names are the same)
                            lastStep.stayOn = true;
                        }
                                
                        else {
                            // What is a zag? TODO write meaningful documentation for this.
                            // It appears to mean simplifying out several rapid turns in succession
                            // from the description.
                            // total hack to remove zags.
                            steps.remove(last);
                            steps.remove(last - 1);
                            step = threeBack;
                            step.distance += twoBack.distance;
                            distance += step.distance;
                            if (twoBack.elevation != null) {
                                if (step.elevation == null) {
                                    step.elevation = twoBack.elevation;
                                } else {
                                    for (P2<Double> d : twoBack.elevation) {
                                        step.elevation.add(new P2<Double>(d.first + step.distance, d.second));
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (!createdNewStep && step.elevation != null) {
                    List<P2<Double>> s = encodeElevationProfile(edge, distance,
                            backState.getOptions().geoidElevation ? -graph.ellipsoidToGeoidDifference : 0);
                    if (step.elevation != null && step.elevation.size() > 0) {
                        step.elevation.addAll(s);
                    } else {
                        step.elevation = s;
                    }
                }
                distance += edge.getDistanceMeters();

            }

            // increment the total length for this step
            step.distance += edge.getDistanceMeters();
            step.addStreetNotes(graph.streetNotesService.getNotes(forwardState));
            lastAngle = DirectionUtils.getLastAngle(geom);

            step.edges.add(edge);
        }

        // add vehicle rental information if applicable
        if(onVehicleRentalState != null && !steps.isEmpty()) {
            steps.get(steps.size()-1).vehicleRentalOnStation =
                    new VehicleRentalStationInfo((VehicleRentalStationVertex) onVehicleRentalState.getVertex());
        }
        if(offVehicleRentalState != null && !steps.isEmpty()) {
            steps.get(0).vehicleRentalOffStation =
                    new VehicleRentalStationInfo((VehicleRentalStationVertex) offVehicleRentalState.getVertex());
        }

        return steps;
    }

    private static boolean isRentalPickUp(State state) {
        return state.getBackEdge() instanceof VehicleRentalEdge && (state.getBackState() == null || !state.getBackState()
                .isRentingVehicle());
    }

    private static boolean isRentalDropOff(State state) {
        return state.getBackEdge() instanceof VehicleRentalEdge && state.getBackState().isRentingVehicle();
    }

    private static boolean isLink(Edge edge) {
        return edge instanceof StreetEdge && (((StreetEdge)edge).getStreetClass() & StreetEdge.CLASS_LINK) == StreetEdge.CLASS_LINK;
    }

    private static double getAbsoluteAngleDiff(double thisAngle, double lastAngle) {
        double angleDiff = thisAngle - lastAngle;
        if (angleDiff < 0) {
            angleDiff += Math.PI * 2;
        }
        double ccwAngleDiff = Math.PI * 2 - angleDiff;
        if (ccwAngleDiff < angleDiff) {
            angleDiff = ccwAngleDiff;
        }
        return angleDiff;
    }

    private static WalkStep createWalkStep(Graph graph, State forwardState, State backState, Locale wantedLocale) {
        Edge en = forwardState.getBackEdge();
        WalkStep step;
        step = new WalkStep();
        step.streetName = en.getName(wantedLocale);
        step.startLocation = new WgsCoordinate(
                backState.getVertex().getLat(),
                backState.getVertex().getLon()
        );
        step.elevation = encodeElevationProfile(forwardState.getBackEdge(), 0,
                forwardState.getOptions().geoidElevation ? -graph.ellipsoidToGeoidDifference : 0);
        step.bogusName = en.hasBogusName();
        step.addStreetNotes(graph.streetNotesService.getNotes(forwardState));
        step.angle = DirectionUtils.getFirstAngle(forwardState.getBackEdge().getGeometry());
        if (forwardState.getBackEdge() instanceof AreaEdge) {
            step.area = true;
        }
        return step;
    }

    private static List<P2<Double>> encodeElevationProfile(Edge edge, double distanceOffset, double heightOffset) {
        if (!(edge instanceof StreetEdge)) {
            return new ArrayList<P2<Double>>();
        }
        StreetEdge elevEdge = (StreetEdge) edge;
        if (elevEdge.getElevationProfile() == null) {
            return new ArrayList<P2<Double>>();
        }
        ArrayList<P2<Double>> out = new ArrayList<P2<Double>>();
        Coordinate[] coordArr = elevEdge.getElevationProfile().toCoordinateArray();
        for (int i = 0; i < coordArr.length; i++) {
            out.add(new P2<Double>(coordArr[i].x + distanceOffset, coordArr[i].y + heightOffset));
        }
        return out;
    }

}
