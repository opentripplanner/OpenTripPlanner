/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.ws;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.util.PolylineEncoder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class PlanGenerator {
    private static final Logger LOGGER = Logger.getLogger(PlanGenerator.class.getCanonicalName());

	Request request;
	private PathService pathService;
	private FareService fareService;
	
	public PlanGenerator(Request request, PathServiceFactory pathServiceFactory) {
		this.request = request;
        pathService = pathServiceFactory.getPathService(request.getRouterId());
        Graph graph = pathService.getGraphService().getGraph();
        fareService = graph.getService(FareService.class);
	}
	
	/**
     * Generates a TripPlan from a Request;
     * 
     */
    public TripPlan generate() {

        TraverseOptions options = getOptions(request);

        checkLocationsAccessible(request, options);

        /* try to plan the trip */
        List<GraphPath> paths = null;
        boolean tooSloped = false;
        try {
            List<String> intermediates = request.getIntermediatePlaces();
            if (intermediates.size() == 0) {
                paths = pathService.plan(request.getFrom(), request.getTo(), request.getDateTime(),
                        options, request.getNumItineraries());
                if (paths == null && request.getWheelchair()) {
                    // There are no paths that meet the user's slope restrictions.
                    // Try again without slope restrictions (and warn user).
                    options.maxSlope = Double.MAX_VALUE;
                    paths = pathService.plan(request.getFrom(), request.getTo(),
                            request.getDateTime(), options, request.getNumItineraries());
                    tooSloped = true;
                }
            } else {
                paths = pathService.plan(request.getFrom(), request.getTo(), intermediates,
                        request.getDateTime(), options);
            }
        } catch (VertexNotFoundException e) {
            LOGGER.log(Level.INFO, "Vertex not found: " + request.getFrom() + " : "
                    + request.getTo(), e);
            throw e;
        }

        if (paths == null || paths.size() == 0) {
            LOGGER
                    .log(Level.INFO, "Path not found: " + request.getFrom() + " : "
                            + request.getTo());
            throw new PathNotFoundException();
        }

        TripPlan plan = generatePlan(paths, request);
        if (plan != null) {
            for (Itinerary i : plan.itinerary) {
                i.tooSloped = tooSloped;
            }
        }
        return plan;
    }

    /**
     * Generates a TripPlan from a set of paths
     */
    public TripPlan generatePlan(List<GraphPath> paths, Request request) {

        GraphPath exemplar = paths.get(0);
        Vertex tripStartVertex = exemplar.getStartVertex();
        Vertex tripEndVertex = exemplar.getEndVertex();
        String startName = tripStartVertex.getName();
        String endName = tripEndVertex.getName();

        // Use vertex labels if they don't have names
        if (startName == null) {
            startName = tripStartVertex.getLabel();
        }
        if (endName == null) {
            endName = tripEndVertex.getLabel();
        }
        Place from = new Place(tripStartVertex.getX(), tripStartVertex.getY(), startName);
        Place to = new Place(tripEndVertex.getX(), tripEndVertex.getY(), endName);

        TripPlan plan = new TripPlan(from, to, request.getDateTime());

        for (GraphPath path : paths) {
            Itinerary itinerary = generateItinerary(path, request.getShowIntermediateStops());
            plan.addItinerary(itinerary);
        }
        return plan;
    }

    /**
     * Generate an itinerary from a @{link GraphPath}. The algorithm here is to walk over each edge
     * in the graph path, accumulating geometry, time, and length data. On mode change, a new leg is
     * generated. Street legs undergo an additional processing step to generate turn-by-turn
     * directions.
     * 
     * @param path
     * @param showIntermediateStops
     *            whether intermediate stops are included in the generated itinerary
     * @return itinerary
     */
    private Itinerary generateItinerary(GraphPath path, boolean showIntermediateStops) {

        Itinerary itinerary = makeEmptyItinerary(path);

        Leg leg = null;
		List<String> notesForNewLeg = new ArrayList<String>();
        Edge edge = null;
        EdgeNarrative edgeNarrative = null;
        TraverseMode mode = null;
        TraverseMode previousMode = null;
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
        State finalState = path.states.getLast();
        double previousElevation = Double.MAX_VALUE;
        double edgeElapsedTime;
        GeometryFactory geometryFactory = new GeometryFactory();
        int startWalk = -1;
        int i = -1;

        for (State currState : path.states) {
            i++;
            /* grab base edge and associated narrative information from SPT edge */
            edge = currState.getBackEdge();
            edgeNarrative = currState.getBackEdgeNarrative();
            /* skip initial state, which has no back edges */
            if (edge == null) continue;
            
            /* Add in notes */
            Set<String> notes = edgeNarrative.getNotes();
			if (notes != null) {
				if (leg == null) {
					notesForNewLeg.addAll(notes);
				} else {
					for (String note : notes) {
						leg.addNote(note);
					}
				}
			}
			
			edgeElapsedTime = currState.getTime() - currState.getBackState().getTime();
			
            if (mode == TraverseMode.BOARDING || mode == TraverseMode.ALIGHTING) {
            	itinerary.waitingTime += edgeElapsedTime;
            }
            
            /* ignore freeEdges */ 
            if (edge instanceof FreeEdge && currState != finalState) {
                continue;
            }
            mode = edgeNarrative.getMode();
            
            boolean changingToInterlinedTrip = leg != null && leg.route != null && 
            	!leg.route.equals(edgeNarrative.getName()) && mode.isTransit() && 
            	previousMode != null && previousMode.isTransit(); 

            if (mode != previousMode || changingToInterlinedTrip) {
                /* change in mode. make a new leg if we are entering walk or transit,
                 * otherwise just update the general itinerary info and move to next edge.
                 */
                previousMode = mode; 
                if (mode == TraverseMode.TRANSFER) {
                    /* transferring mode */
                    itinerary.walkTime += edgeElapsedTime;
                    itinerary.walkDistance += edgeNarrative.getDistance();
                    continue;
                } else if (mode == TraverseMode.BOARDING) {
                    /* boarding mode */
                	itinerary.transfers++;
                    continue;
                } else if (mode == TraverseMode.ALIGHTING) {
                    /* alighting mode */
                    leg.to = makePlace(edgeNarrative.getToVertex()); 
                    leg.endTime = new Date(currState.getBackState().getTime());
                    continue;
                } else if (changingToInterlinedTrip) {
                	/* finalize leg */
                	leg.to = makePlace(edgeNarrative.getFromVertex()); 
                	leg.endTime = new Date(currState.getBackState().getTime());
                	Geometry geometry = geometryFactory.createLineString(coordinates);
                    leg.legGeometry = PolylineEncoder.createEncodings(geometry);
                    /* reset coordinates */
                    coordinates = new CoordinateArrayListSequence();

                	/* initialize new leg */
                    leg = makeLeg(currState);
                    leg.interlineWithPreviousLeg = true;
                    for (String noteForNewLeg : notesForNewLeg) {
                    	leg.addNote(noteForNewLeg);
                    }
                    notesForNewLeg.clear();
                    leg.mode = mode.toString();
                    
                    startWalk = -1;
                    leg.route = edgeNarrative.getName();

                    itinerary.addLeg(leg);
                
                } else {
                    /* entering transit or onStreet mode leg because traverseMode can only be: 
                     * transit, onStreetNonTransit, board, alight, or transfer.
                     */
                    if (leg != null) {
                        /* finalize prior leg if it exists */
                        if (startWalk != -1) {
                            leg.walkSteps = getWalkSteps(pathService, path.states.subList(startWalk, i));
                        }
                        if (leg.to == null) {
                        	/* this stuff is filled in in the alight case, but not in the walk case */
                        	leg.to = makePlace(edgeNarrative.getFromVertex()); 
                        	leg.endTime = new Date(currState.getBackState().getTime());
                        }
                        Geometry geometry = geometryFactory.createLineString(coordinates);
                        leg.legGeometry = PolylineEncoder.createEncodings(geometry);
                        /* reset coordinates */
                        coordinates = new CoordinateArrayListSequence();
                    }
                    /* initialize new leg */
                    leg = makeLeg(currState);
                    for (String noteForNewLeg : notesForNewLeg) {
                    	leg.addNote(noteForNewLeg);
                    }
                    notesForNewLeg.clear();
                    if (mode == null) {
                    	mode = currState.getBackState().getBackEdgeNarrative().getMode();
                    	previousMode = mode;
                    }
                    leg.mode = mode.toString();
                    
                    if (mode.isOnStreetNonTransit()) {
                        /* on-street (walk/bike) leg
                         * mark where in edge list on-street legs begin,
                         * so step-by-step instructions can be generated for this sublist later
                         */
                        startWalk = i;
                    } else {
                        /* transit leg */
                        startWalk = -1;
                        leg.route = edgeNarrative.getName();
                    }
                    itinerary.addLeg(leg);
                }   
            } /* end handling mode changes */
            /* either a new leg has been created, or a leg already existed,
             * and the current edge's mode is same as that leg.
             * if you fall through to here, a leg necessarily exists and leg != null.
             * accumulate current edge's distance onto this existing leg.
             */
            leg.distance += edgeNarrative.getDistance(); 
            /* for all edges with geometry, append their coordinates to the leg's. */
            Geometry edgeGeometry = edgeNarrative.getGeometry();
            if (edgeGeometry != null) {
                Coordinate[] edgeCoordinates = edgeGeometry.getCoordinates();
                if (coordinates.size() > 0
                        && coordinates.getCoordinate(coordinates.size() - 1).equals(
                                edgeCoordinates[0])) {
                    coordinates.extend(edgeCoordinates, 1);
                } else {
                    coordinates.extend(edgeCoordinates);
                }
            }
            /* we are not boarding, alighting, etc.
             * so are we walking/biking/driving or using transit?
             */
            if (mode.isOnStreetNonTransit()) {
                /* we are on the street (non-transit) */
                itinerary.walkTime += edgeElapsedTime;
                itinerary.walkDistance += edgeNarrative.getDistance();
                if (edge instanceof EdgeWithElevation) {
                    PackedCoordinateSequence profile = ((EdgeWithElevation) edge)
                            .getElevationProfile();
                    previousElevation = applyElevation(profile, itinerary, previousElevation);
                }
            } else if (mode.isTransit()) {
                /* we are on a transit trip */
                itinerary.transitTime += edgeElapsedTime;
                if (showIntermediateStops) {
                    /* add an intermediate stop to the current leg */
                    if (leg.stop == null) {
                        /* first transit edge, just create the list (the initial stop is current
                         * "from" vertex)
                         */
                        leg.stop = new ArrayList<Place>();
                    } 
                    /* any further transit edge, add "from" vertex to intermediate stops */
                    Place stop = makePlace(currState);
                    leg.stop.add(stop);
                }
            } 
        } /* end loop over graphPath edge list */

        if (leg != null) {
            /* finalize leg */ 
            leg.to = makePlace(edgeNarrative.getToVertex());
            leg.endTime = new Date(finalState.getTime());
            Geometry geometry = geometryFactory.createLineString(coordinates);
            leg.legGeometry = PolylineEncoder.createEncodings(geometry);
            if (startWalk != -1) {
                leg.walkSteps = getWalkSteps(pathService, path.states.subList(startWalk, i + 1));
            }
        }
        if (itinerary.transfers == -1) {
            itinerary.transfers = 0;
        }
        itinerary.removeBogusLegs();
        return itinerary;
    }

    
  
    
    
    /**
     * Adjusts an Itinerary's elevation fields from an elevation profile
     * 
     * @return the elevation at the end of the profile
     */
    private double applyElevation(PackedCoordinateSequence profile, Itinerary itinerary,
            double previousElevation) {
        if (profile != null) {
            for (Coordinate coordinate : profile.toCoordinateArray()) {
                if (previousElevation == Double.MAX_VALUE) {
                    previousElevation = coordinate.y;
                    continue;
                }
                double elevationChange = previousElevation - coordinate.y;
                if (elevationChange > 0) {
                    itinerary.elevationGained += elevationChange;
                } else {
                    itinerary.elevationLost -= elevationChange;
                }
                previousElevation = coordinate.y;
            }
        }
        return previousElevation;
    }

    /**
     * Makes a new empty leg from a starting edge
     */
    private Leg makeLeg(State s) {
        Leg leg = new Leg();

        leg.startTime = new Date(s.getBackState().getTime());
        EdgeNarrative en = s.getBackEdgeNarrative();
        leg.route = en.getName();
        Trip trip = en.getTrip();
        if (trip != null) {
            leg.headsign      = trip.getTripHeadsign();
            leg.agencyId      = trip.getId().getAgencyId();
            leg.tripShortName = trip.getTripShortName();
            leg.routeShortName = trip.getRoute().getShortName();
            leg.routeLongName = trip.getRoute().getLongName();
        }
        leg.distance = 0.0;
        leg.from = makePlace(en.getFromVertex());
        return leg;
    }

    /**
     * Makes a new empty Itinerary for a given path.
     * 
     * @return
     */
    private Itinerary makeEmptyItinerary(GraphPath path) {
        Itinerary itinerary = new Itinerary();

        State startState = path.states.getFirst();
        State endState = path.states.getLast();

        itinerary.startTime = new Date(startState.getTime());
        itinerary.endTime = new Date(endState.getTime());
        itinerary.duration = endState.getTime() - startState.getTime();
        if (fareService != null) {
        	itinerary.fare = fareService.getCost(path);
        }
        itinerary.transfers = -1;
        return itinerary;
    }

    /**
     * Makes a new Place from a state. Contains information about time.
     * 
     * @return
     */
    private Place makePlace(State state) {
        Coordinate endCoord = state.getVertex().getCoordinate();
		String name = state.getVertex().getName();        
        AgencyAndId stopId = state.getVertex().getStopId();
		Date timeAtState = new Date(state.getTime());
		Place place = new Place(endCoord.x, endCoord.y, name, stopId,  timeAtState);
        return place;
    }
    
    /**
     * Makes a new Place from a vertex.
     * 
     * @return
     */
    private Place makePlace(Vertex vertex) {
        Coordinate endCoord = vertex.getCoordinate();
        Place place = new Place(endCoord.x, endCoord.y, vertex.getName());
        place.stopId = vertex.getStopId();
        return place;
    }

    /**
     * Throw an exception if the start and end locations are not wheelchair accessible given the
     * user's specified maximum slope.
     */
    private void checkLocationsAccessible(Request request, TraverseOptions options) {
        if (request.getWheelchair()) {
            // check if the start and end locations are accessible
            if (!pathService.isAccessible(request.getFrom(), options)
                    || !pathService.isAccessible(request.getTo(), options)) {
                throw new LocationNotAccessible();
            }

        }
    }

    /**
     * Get the traverse options for a request
     * 
     * @param request
     * @return
     */
    private TraverseOptions getOptions(Request request) {

        TraverseModeSet modeSet = request.getModeSet();
        assert (modeSet.isValid());
        TraverseOptions options = new TraverseOptions(modeSet);
        options.optimizeFor = request.getOptimize();
        options.setArriveBy(request.isArriveBy());
        options.wheelchairAccessible = request.getWheelchair();
        if (request.getMaxSlope() > 0) {
            options.maxSlope = request.getMaxSlope();
        }
        if (request.getMaxWalkDistance() > 0) {
            options.setMaxWalkDistance(request.getMaxWalkDistance());
        }
        if (request.getMinTransferTime() != null) {
            options.minTransferTime = request.getMinTransferTime();
        }
        if (request.getPreferredRoutes()!= null){
            for(String element : request.getPreferredRoutes()){
            	String[] routeSpec = element.split("_", 2);
            	if (routeSpec.length != 2) {
                    throw new IllegalArgumentException("AgencyId or routeId not set in preferredRoutes list");
                }
                options.preferredRoutes.add(new RouteSpec(routeSpec[0], routeSpec[1]));
            }
        }
        if (request.getUnpreferredRoutes()!= null){
        	for(String element : request.getUnpreferredRoutes()){
            	String[] routeSpec = element.split("_", 2);
            	if (routeSpec.length != 2) {
                    throw new IllegalArgumentException("AgencyId or routeId not set in unpreferredRoutes list");
                }
                options.unpreferredRoutes.add(new RouteSpec(routeSpec[0], routeSpec[1]));
            }
        }
        if (request.getBannedRoutes()!= null){
        	for(String element : request.getBannedRoutes()){
            	String[] routeSpec = element.split("_", 2);
            	if (routeSpec.length != 2) {
                    throw new IllegalArgumentException("AgencyId or routeId not set in bannedRoutes list");
                }
                options.bannedRoutes.add(new RouteSpec(routeSpec[0], routeSpec[1]));
            }
        }
        return options;
    }

    /**
     * Converts a list of street edges to a list of turn-by-turn directions.
     * 
     * @param edges
     *            : A list of street edges
     * @return
     */
    private List<WalkStep> getWalkSteps(PathService pathService, List<State> states) {
        List<WalkStep> steps = new ArrayList<WalkStep>();
        WalkStep step = null;
        double lastAngle = 0, distance = 0; // distance used for appending elevation profiles
        int roundaboutExit = 0; // track whether we are in a roundabout, and if so the exit number
        
        for (State currState : states) {
            Edge edge = currState.getBackEdge();
            EdgeNarrative edgeResult = currState.getBackEdgeNarrative();
            if (edge instanceof FreeEdge) {
                continue;
            }
            Geometry geom = edgeResult.getGeometry();
            if (geom == null) {
                continue;
            }
            String streetName = edgeResult.getName();
            if (step == null) {
                // first step
                step = createWalkStep(currState);
                steps.add(step);
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setAbsoluteDirection(thisAngle);
                // new step, set distance to length of first edge
                distance = edgeResult.getDistance(); 
            } else if (step.streetName != streetName
                    && (step.streetName != null && !step.streetName.equals(streetName))) {
                /* street name has changed */
                if (roundaboutExit > 0) {
                    // if we were just on a roundabout, 
                    // make note of which exit was taken in the existing step
                    step.exit = Integer.toString(roundaboutExit); // ordinal numbers from localization
                    roundaboutExit = 0;
                }
                /* start a new step */
                step = createWalkStep(currState);
                steps.add(step);
                if (edgeResult.isRoundabout()) {
                    // indicate that we are now on a roundabout
                    // and use one-based exit numbering 
                    roundaboutExit = 1;
                }
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                step.setDirections(lastAngle, thisAngle, edgeResult.isRoundabout());
                step.becomes = !pathService.multipleOptionsBefore(edge);
                // new step, set distance to length of first edge
                distance = edgeResult.getDistance();
            } else {
                /* street name has not changed */
                double thisAngle = DirectionUtils.getFirstAngle(geom);
                RelativeDirection direction = WalkStep.getRelativeDirection(lastAngle, thisAngle, edgeResult.isRoundabout());
                boolean optionsBefore = pathService.multipleOptionsBefore(edge);
                if (edgeResult.isRoundabout()) {
                    // we are on a roundabout, and have already traversed at least one edge of it.
                    if (optionsBefore) {
                        // increment exit count if we passed one.
                        roundaboutExit += 1;
                    } 
                } 
                if (edgeResult.isRoundabout() || direction == RelativeDirection.CONTINUE) {
                    // we are continuing almost straight, or continuing along a roundabout.
                    // just append elevation info onto the existing step.
                    if (step.elevation != null) {
                        String s = encodeElevationProfile(edge, distance);
                        if (step.elevation.length() > 0 && s != null && s.length() > 0)
                            step.elevation += ",";
                        step.elevation += s;
                    }
                    // extending a step, increment the existing distance
                    distance += edgeResult.getDistance();
                } else { 
                    // we are not on a roundabout, and not continuing straight through.
                    // figure out if there were turn options at the last intersection.
                    if (optionsBefore) {
                        // turn to stay on same-named street
                        step = createWalkStep(currState);
                        steps.add(step);
                        step.setDirections(lastAngle, thisAngle, false);
                        step.stayOn = true;
                        // new step, set distance to length of first edge
                        distance = edgeResult.getDistance();
                    }
                }
            }
            // increment the total length for this step
            step.distance += edgeResult.getDistance();
            lastAngle = DirectionUtils.getLastAngle(geom);
        }
        return steps;
    }

    private WalkStep createWalkStep(State s) {
    	EdgeNarrative en = s.getBackEdgeNarrative();
        WalkStep step;
        step = new WalkStep();
        step.streetName = en.getName();
        step.lon = en.getFromVertex().getX();
        step.lat = en.getFromVertex().getY();
        step.elevation = encodeElevationProfile(s.getBackEdge(), 0);
        step.bogusName = en.hasBogusName();
        return step;
    }

    private String encodeElevationProfile(Edge edge, double offset) {
        if (!(edge instanceof EdgeWithElevation)) {
            return "";
        }
        EdgeWithElevation elevEdge = (EdgeWithElevation) edge;
        if (elevEdge.getElevationProfile() == null) {
            return "";
        }
        String str = "";
        Coordinate[] coordArr = elevEdge.getElevationProfile().toCoordinateArray();
        for (int i = 0; i < coordArr.length; i++) {
            str += Math.round(coordArr[i].x + offset) + "," + Math.round(coordArr[i].y * 10.0)
                    / 10.0 + (i < coordArr.length - 1 ? "," : "");
        }
        return str;
    }

}
