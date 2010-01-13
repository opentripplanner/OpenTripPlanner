/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.api.ws;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.model.Fare;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Turn;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.util.PolylineEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 *
 */
// NOTE - /ws/plan is the full path -- see web.xml
@Path("/plan")
@XmlRootElement
@Autowire
public class Planner {

    private static final Logger LOGGER = Logger.getLogger(Planner.class.getCanonicalName());

    private PathService pathservice;

    @Autowired
    public void setPathService(PathService pathService) {
        this.pathservice = pathService;
    }

    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getItineraries(
            @QueryParam(RequestInf.FROM) String fromPlace,
            @QueryParam(RequestInf.TO) String toPlace,
            @QueryParam(RequestInf.INTERMEDIATE_PLACES) List<String> intermediatePlaces, 
            @QueryParam(RequestInf.DATE) String date,
            @QueryParam(RequestInf.TIME) String time,
            @QueryParam(RequestInf.ARRIVE_BY) Boolean arriveBy,
            @QueryParam(RequestInf.WHEELCHAIR) Boolean wheelchair,
            @QueryParam(RequestInf.MAX_WALK_DISTANCE) Double maxWalkDistance,
            @QueryParam(RequestInf.WALK_SPEED) Double walkSpeed,
            @QueryParam(RequestInf.OPTIMIZE) OptimizeType optimize,
            @QueryParam(RequestInf.MODE) TraverseModeSet modes,
            @QueryParam(RequestInf.NUMBER_ITINERARIES) Integer max)
            throws JSONException {

        /* create request */
        Request request = new Request();
        request.setFrom(fromPlace);
        request.setTo(toPlace);
        request.setDateTime(date, time);
        request.setWheelchair(wheelchair);

        if (max != null)
            request.setNumItineraries(max);
        if (maxWalkDistance != null)
            request.setWalk(maxWalkDistance);
        if (arriveBy != null && arriveBy)
            request.setArriveBy(true);

        request.setOptimize(optimize);

        request.setModes(modes);

        /* use request to generate trip */

        return generatePlan(request);
    }

    /**
     * Generates a TripPlan from a Request;
     * 
     * @param request
     * @return
     */

    private Response generatePlan(Request request) {
        TraverseModeSet modeSet = request.getModeSet();
        assert (modeSet.isValid());
        TraverseOptions options = new TraverseOptions(modeSet);
        options.optimizeFor = request.getOptimize();
        options.back = request.isArriveBy();
        options.wheelchairAccessible = request.getWheelchair();
        List<GraphPath> paths = null;
        try {
            List<String> intermediates = request.getIntermediatePlaces();
            if (intermediates.size() == 0) {
                paths = pathservice.plan(request.getFrom(), request.getTo(), 
                        request.getDateTime(), options);
                if (paths == null) {
                    paths = pathservice.plan(request.getFrom(), request.getTo(),
                            request.getDateTime(), options);
                }
            } else {
                paths = pathservice.plan(request.getFrom(), request.getTo(),
                        intermediates, request.getDateTime(), options);
            }
        } catch (VertexNotFoundException e) {
            LOGGER.log(Level.INFO, "Vertex not found: " + request.getFrom() + " : " + request.getTo(), e);
            Response response = new Response(request, null);

            response.error = new PlannerError(e.getMissing());
            return response;
        }
        if (paths == null || paths.size() == 0) {
            LOGGER.log(Level.INFO, "Path not found: " + request.getFrom() + " : " + request.getTo());
            Response response = new Response(request, null);
            response.error = new PlannerError();
            return response;
        }
        Vector<SPTVertex> vertices = paths.get(0).vertices;
        SPTVertex tripStartVertex = vertices.firstElement();
        SPTVertex tripEndVertex = vertices.lastElement();
        Place from = new Place(tripStartVertex.getX(), tripStartVertex.getY(), request.getFrom());
        Place to = new Place(tripEndVertex.getX(), tripEndVertex.getY(), request.getTo());

        TripPlan plan = new TripPlan(from, to, request.getDateTime());

        for (GraphPath path : paths) {

            Itinerary itinerary = new Itinerary();
            plan.addItinerary(itinerary);

            SPTVertex startVertex = path.vertices.firstElement();
            State startState = startVertex.state;
            SPTVertex endVertex = path.vertices.lastElement();
            State endState = endVertex.state;

            itinerary.startTime = new Date(startState.getTime());
            itinerary.endTime = new Date(endState.getTime());
            itinerary.duration = endState.getTime() - startState.getTime();
            itinerary.fare = new Fare();
            itinerary.fare.addFare(Fare.FareType.regular, Currency.getInstance("USD"), 225);

            Leg leg = null;
            TraverseMode mode = null;
            Geometry geometry = null;
            String name = null;

            int startWalk = -1;
            int i = -1;
            SPTEdge lastEdge = null;
            for (SPTEdge edge : path.edges) {
                i++;
                Edge graphEdge = edge.payload;

                if (graphEdge instanceof Turn) {
                    continue;
                }
                lastEdge = edge;

                TraverseMode edgeMode = graphEdge.getMode();
                double edgeTime = edge.tov.state.getTime() - edge.fromv.state.getTime();

                if (!edgeMode.isTransit() && edgeMode != TraverseMode.ALIGHTING) {
                    if (edgeMode != mode
                            || (!mode.isOnStreetNonTransit() && graphEdge.getName() != name)) {
                        name = graphEdge.getName();
                        if (leg != null) {
                            /* finalize leg */
                            if (startWalk != -1) {
                                leg.walkSteps = getWalkSteps(path.edges.subList(startWalk, i));
                            }
                            Vertex fromv = graphEdge.getFromVertex();
                            Coordinate endCoord = fromv.getCoordinate();
                            leg.to = new Place(endCoord.x, endCoord.y, fromv.getName());
                            leg.to.stopId = fromv.getStopId();
                            leg.endTime = new Date(edge.tov.state.getTime());
                            leg.legGeometry = PolylineEncoder.createEncodings(geometry);
                            leg.duration = edge.tov.state.getTime() - leg.startTime.getTime();
                            leg = null;
                            geometry = null;
                        }

                        leg = new Leg();
                        itinerary.addLeg(leg);

                        leg.startTime = new Date(edge.fromv.state.getTime());
                        leg.route = graphEdge.getName();
                        mode = graphEdge.getMode();
                        leg.mode = mode.toString();
                        if (mode == TraverseMode.WALK) {
                            startWalk = i;
                        } else {
                            startWalk = -1;
                        }
                        leg.distance = edge.getDistance();
                        Vertex fromv = graphEdge.getFromVertex();
                        Coordinate endCoord = fromv.getCoordinate();
                        leg.from = new Place(endCoord.x, endCoord.y, fromv.getName());
                        leg.from.stopId = fromv.getStopId();
                    }
                }
                Geometry edgeGeometry = graphEdge.getGeometry();
                if (geometry == null) {
                    geometry = edgeGeometry;
                } else {
                    if (edgeGeometry != null) {
                        geometry = geometry.union(edgeGeometry);
                    }
                }

                if (edgeMode == TraverseMode.TRANSFER) {

                    itinerary.transfers++;
                    itinerary.walkTime += edgeTime;
                    itinerary.walkDistance += graphEdge.getDistance();
                    continue;
                }

                if (edgeMode == TraverseMode.BOARDING) {
                    itinerary.waitingTime += edgeTime;
                    continue;
                }

                if (edgeMode == TraverseMode.WALK) {
                    itinerary.walkTime += edgeTime;
                    itinerary.walkDistance += graphEdge.getDistance();
                }

                if (edgeMode.isTransit()) {
                    itinerary.transitTime += edgeTime;
                    mode = graphEdge.getMode();
                    leg.mode = mode.toString();
                    leg.route = graphEdge.getName();
                }
            }

            Edge graphEdge = lastEdge.payload;

            if (leg != null) {
                /* finalize leg */
                Vertex tov = graphEdge.getToVertex();
                Coordinate endCoord = tov.getCoordinate();
                leg.to = new Place(endCoord.x, endCoord.y, tov.getName());
                leg.to.stopId = tov.getStopId();
                leg.endTime = new Date(lastEdge.tov.state.getTime());
                leg.legGeometry = PolylineEncoder.createEncodings(geometry);
                leg.duration = lastEdge.tov.state.getTime() - leg.startTime.getTime();
                if (startWalk != -1) {
                    leg.walkSteps = getWalkSteps(path.edges.subList(startWalk, i + 1));
                }
                leg = null;
            }

        }
        Response response = new Response(request, plan);
        return response;
    }

    /**
     * Converts a list of street edges to a list of turn-by-turn directions.
     * 
     * @param edges
     *            : A list of street edges
     * @return
     */
    private List<WalkStep> getWalkSteps(List<SPTEdge> edges) {

        List<WalkStep> steps = new ArrayList<WalkStep>();

        WalkStep step = null;

        double lastAngle = 0;

        for (SPTEdge sptEdge : edges) {
            Edge edge = sptEdge.payload;
            if (edge instanceof Turn) {
                //Turns do not exist outside of routing
                continue;
            }
            String streetName = edge.getName();
            if (step == null) {
                //first step
                step = new WalkStep();
                steps.add(step);
                step.streetName = streetName;
                step.lon = edge.getFromVertex().getX();
                step.lat = edge.getFromVertex().getY();
                double thisAngle = DirectionUtils.getFirstAngle(edge.getGeometry());
                step.setAbsoluteDirection(thisAngle);
            } else if (!step.streetName.equals(streetName)) {
                // change of street name
                step = new WalkStep();
                steps.add(step);
                step.streetName = streetName;
                double thisAngle = DirectionUtils.getFirstAngle(edge.getGeometry());
                step.setDirections(lastAngle, thisAngle);
                step.lon = edge.getFromVertex().getX();
                step.lat = edge.getFromVertex().getY();
                step.becomes = !multipleOptionsBefore(edge);
            } else {
                /* generate turn-to-stay-on directions, where needed */
                double thisAngle = DirectionUtils.getFirstAngle(edge.getGeometry());
                RelativeDirection direction = WalkStep.getRelativeDirection(lastAngle, thisAngle);
                if (direction != RelativeDirection.CONTINUE) {
                    // figure out if there was another way we could have turned
                    boolean optionsBefore = multipleOptionsBefore(edge);
                    if (optionsBefore) {
                        step = new WalkStep();
                        steps.add(step);
                        step.streetName = streetName;
                        step.setDirections(lastAngle, thisAngle);
                        step.stayOn = true;
                        step.lon = edge.getFromVertex().getX();
                        step.lat = edge.getFromVertex().getY();
                    }
                }
            }

            step.distance += edge.getDistance();

            lastAngle = DirectionUtils.getLastAngle(edge.getGeometry());
        }
        return steps;
    }

    private boolean multipleOptionsBefore(Edge edge) {
        boolean foundAlternatePaths = false;
        Vertex start = edge.getFromVertex();
        for (Edge out : start.getOutgoing()) {
            if (out == edge) {
                continue;
            }
            if (!(out instanceof Turn)) {
                continue;
            }
            // there were paths we didn't take.
            foundAlternatePaths = true;
            break;
        }
        return foundAlternatePaths;
    }
}
