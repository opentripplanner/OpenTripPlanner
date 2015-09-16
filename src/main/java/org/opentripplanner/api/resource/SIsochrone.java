package org.opentripplanner.api.resource;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.GeodeticCalculator;
import org.opensphere.geometry.algorithm.ConcaveHull;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.ReversibleLineStringWrapper;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.util.*;

/**
 * This is the original Isochrone class provided by Stefan Steineger.
 * Another implementation has been provided by Laurent Grégoire (isochrone2).
 */
@Path("/routers/{routerId}/isochroneOld")
@XmlRootElement
public class SIsochrone extends RoutingResource {

    private static final Logger LOG = LoggerFactory.getLogger(SIsochrone.class);

    public static final String RESULT_TYPE_POINTS = "POINTS";

    public static final String RESULT_TYPE_SHED = "SHED";

    public static final String RESULT_TYPE_EDGES = "EDGES";

    private boolean showTooFastEdgesAsDebugGeomsANDnotUShapes = true;

    private List debugGeoms = null;

    private List tooFastTraversedEdgeGeoms = null;

    /** Walkspeed between user indicated position and road 3000 m/h = 0.83333 m/sec */
    public double offRoadWalkspeed = 0.8333;

    /** To decide between edge-based or point-based calculation of sheds, i.e. hulls. Will be set later again. */
    public long shedCalcMethodSwitchTimeInSec = 60 * 25;

    public double angleLimitForUShapeDetection = 20.0 * Math.PI / 180.0;

    public double distanceToleranceForUShapeDetection = 1.1; // in percent: e.g. 1.1 = 110%

    /**
     * To calculate the length of sub-edges and eventually to detect u-shaped roads, in m/sec (will be set later dependent on mode)
     */
    public double maxUserSpeed = 1.3;

    private boolean usesCar = false;

    /** Parameter for concave hull computation, i.e. the maximal (triangulation) edge length in degrees */
    public double concaveHullAlpha = 0.005;

    public boolean doSpeedTest = false; // to detect u-shaped roads etc., as an additional test besides the angle test

    private boolean noRoadNearBy = false;

    /**
     * Calculates walksheds for a given location, based on time given to walk and the walk speed. 
     *
     * Depending on the value for the "output" parameter (i.e. "POINTS", "SHED" or "EDGES"), a 
     * different type of GeoJSON geometry is returned. If a SHED is requested, then a ConcaveHull 
     * of the EDGES/roads is returned. If that fails, a ConvexHull will be returned. 
     * <p>
     * The ConcaveHull parameter is set to 0.005 degrees. The offroad walkspeed is assumed to be 
     * 0.83333 m/sec (= 3km/h) until a road is hit.
     * <p>
     * Note that the set of EDGES/roads returned as well as POINTS returned may contain duplicates. 
     * If POINTS are requested, then not the end-points are returned at which the max time is 
     * reached, but instead all the graph nodes/crossings that are within the time limits.
     * <p>
     * In case there is no road near by within the given time, then a circle for the walktime limit 
     * is created and returned for the SHED parameter. Otherwise the edge with the direction 
     * towards the closest road. Note that the circle is calculated in Euclidian 2D coordinates, 
     * and distortions towards an ellipse will appear if it is transformed/projected to the user location.
     * <p>
     * An example request may look like this:
     * localhost:8080/otp-rest-servlet/ws/iso?layers=traveltime&styles=mask&batch=true&fromPlace=51.040193121307176
     * %2C-114.04471635818481&toPlace
     * =51.09098935%2C-113.95179705&time=2012-06-06T08%3A00%3A00&mode=WALK&maxWalkDistance=10000&walkSpeed=1.38&walkTime=10.7&output=EDGES 
     * Though the first parameters (i) layer, (ii) styles and (iii) batch could be discarded.
     * 
     * @param walkmins Maximum number of minutes to walk.
     * @param output Can be set to "POINTS", "SHED" or "EDGES" to return different types of GeoJSON 
     *        geometry. SHED returns a ConcaveHull or ConvexHull of the edges/roads. POINTS returns
     *        all graph nodes that are within the time limit. 
     * @return a JSON document containing geometries (either points, lineStrings or a polygon).
     * @throws Exception
     * @author sstein---geo.uzh.ch
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String getIsochrone(
            @QueryParam("walkTime") @DefaultValue("15")     double walkmins,
            @QueryParam("output")   @DefaultValue("POINTS") String output ) throws Exception {

        this.debugGeoms = new ArrayList();
        this.tooFastTraversedEdgeGeoms = new ArrayList();

        RoutingRequest sptRequestA = buildRequest();
        String from = sptRequestA.from.toString();
        int pos = 1;
        float lat = 0;
        float lon = 0;
        for (String s : from.split(",")) {
            if (s.isEmpty()) {
                // no location
                Response.status(Status.BAD_REQUEST).entity("no position").build();
                return null;
            }
            try {
                float num = Float.parseFloat(s);
                if (pos == 1) {
                    lat = num;
                }
                if (pos == 2) {
                    lon = num;
                }
            } catch (Exception e) {
                throw new WebApplicationException(
                        Response.status(Status.BAD_REQUEST)
                                .entity("Could not parse position string to number. Require numerical lat & long coords.")
                                .build());
            }
            pos++;
        }

        GeometryFactory gf = new GeometryFactory();

        Coordinate dropPoint = new Coordinate(lon, lat);

        int walkInMin = (int) Math.floor(walkmins);
        double walkInSec = walkmins * 60;
        LOG.debug("given travel time: " + walkInMin + " mins + " + (walkInSec - (60 * walkInMin))
                + " sec");
        // restrict the evaluated SPT size to 30mins for requests with walking < 30min
        // if larger walking times are requested we adjust the evaluated
        // graph dynamically by 1.3 * min -> this should save processing time
        if (walkInMin < 30) {
            sptRequestA.worstTime = sptRequestA.dateTime + (30 * 60);
        } else {
            sptRequestA.worstTime = sptRequestA.dateTime + Math.round(walkInMin * 1.3 * 60);
        }
        // set the switch-time for shed/area calculation, i.e. to decide if the hull is calculated based on points or on edges
        TraverseModeSet modes = sptRequestA.modes;
        LOG.debug("mode(s): " + modes);
        if ((modes.contains(TraverseMode.TRANSIT)) || (modes.contains(TraverseMode.BUSISH))
                || (modes.contains(TraverseMode.TRAINISH))) {
            shedCalcMethodSwitchTimeInSec = 60 * 20; // 20min (use 20min for transit, since buses may not come all the time)
        } else if (modes.contains(TraverseMode.CAR)) {
            shedCalcMethodSwitchTimeInSec = 60 * 10; // 10min
        } else if (modes.contains(TraverseMode.BICYCLE)) {
            shedCalcMethodSwitchTimeInSec = 60 * 10; // 10min
        } else {
            shedCalcMethodSwitchTimeInSec = 60 * 20; // 20min
        }
        // set the maxUserSpeed, which is used later to check for u-type streets/crescents when calculating sub-edges;
        // Note, that the car speed depends on the edge itself, so this value may be replaced later
        this.usesCar = false;
        int numberOfModes = modes.getModes().size();
        if (numberOfModes == 1) {
            if (modes.getWalk()) {
                this.maxUserSpeed = sptRequestA.walkSpeed;
            } else if (modes.getBicycle()) {
                this.maxUserSpeed = sptRequestA.bikeSpeed;
            } else if (modes.getCar()) {
                this.maxUserSpeed = sptRequestA.carSpeed;
                this.usesCar = true;
            }
        } else {// for all other cases (multiple-modes)
                // sstein: I thought I may set it to 36.111 m/sec = 130 km/h,
                // but maybe it is better to assume walk speed for transit, i.e. treat it like if the
                // person gets off the bus on the last crossing and walks the "last mile".
            this.maxUserSpeed = sptRequestA.walkSpeed;
        }

        if (doSpeedTest) {
            LOG.debug("performing angle and speed based test to detect u-shapes");
        } else {
            LOG.debug("performing only angle based test to detect u-shapes");
        }

        // TODO: OTP prefers to snap to car-roads/ways, which is not so nice, when walking,
        // and a footpath is closer by. So far there is no option to switch that off

        Router router = otpServer.getRouter(routerId);
        // create the ShortestPathTree
        try {
            sptRequestA.setRoutingContext(router.graph);
        } catch (Exception e) {
            // if we get an exception here, and in particular a VertexNotFoundException,
            // then it is likely that we chose a (transit) mode without having that (transit) modes data
            LOG.debug("cannot set RoutingContext: " + e.toString());
            LOG.debug("cannot set RoutingContext: setting mode=WALK");
            sptRequestA.setMode(TraverseMode.WALK); // fall back to walk mode
            sptRequestA.setRoutingContext(router.graph);
        }
        ShortestPathTree sptA = new AStar().getShortestPathTree(sptRequestA);
        StreetLocation origin = (StreetLocation) sptRequestA.rctx.fromVertex;
        sptRequestA.cleanup(); // remove inserted points

        // create a LineString for display
        Coordinate pathToStreetCoords[] = new Coordinate[2];
        pathToStreetCoords[0] = dropPoint;
        pathToStreetCoords[1] = origin.getCoordinate();
        LineString pathToStreet = gf.createLineString(pathToStreetCoords);

        // get distance between origin and drop point for time correction
        double distanceToRoad = SphericalDistanceLibrary.distance(origin.getY(), origin.getX(),
                dropPoint.y, dropPoint.x);
        long offRoadTimeCorrection = (long) (distanceToRoad / this.offRoadWalkspeed);

        //
        // --- filter the states ---
        //
        Set<Coordinate> visitedCoords = new HashSet<Coordinate>();
        ArrayList<Edge> allConnectingEdges = new ArrayList<Edge>();
        Coordinate coords[] = null;
        long maxTime = (long) walkInSec - offRoadTimeCorrection;
        // System.out.println("Reducing walktime from: " + (int)(walkmins * 60) + "sec to " + maxTime + "sec due to initial walk of " + distanceToRoad
        // + "m");

        // if the initial walk is already to long, there is no need to parse...
        if (maxTime <= 0) {
            noRoadNearBy = true;
            long timeToWalk = (long) walkInSec;
            long timeBetweenStates = offRoadTimeCorrection;
            long timeMissing = timeToWalk;
            double fraction = (double) timeMissing / (double) timeBetweenStates;
            pathToStreet = getSubLineString(pathToStreet, fraction);
            LOG.debug(
                    "no street found within giving travel time (for off-road walkspeed: {} m/sec)",
                    this.offRoadWalkspeed);
        } else {
            noRoadNearBy = false;
            Map<ReversibleLineStringWrapper, Edge> connectingEdgesMap = Maps.newHashMap();
            for (State state : sptA.getAllStates()) {
                long et = state.getElapsedTimeSeconds();
                if (et <= maxTime) {
                    // -- filter points, as the same coordinate may be passed several times due to the graph structure
                    // in a Calgary suburb family homes neighborhood with a 15min walkshed it filtered about
                    // 250 points away (while 145 were finally displayed)
                    if (visitedCoords.contains(state.getVertex().getCoordinate())) {
                        continue;
                    } else {
                        visitedCoords.add(state.getVertex().getCoordinate());
                    }
                    // -- get all Edges needed later for the edge representation
                    // and to calculate an edge-based walkshed
                    // Note, it can happen that we get a null geometry here, e.g. for hop-edges!
                    Collection<Edge> vertexEdgesIn = state.getVertex().getIncoming();
                    for (Iterator<Edge> iterator = vertexEdgesIn.iterator(); iterator.hasNext();) {
                        Edge edge = (Edge) iterator.next();
                        Geometry edgeGeom = edge.getGeometry();
                        if (edgeGeom != null) { // make sure we get only real edges
                            if (edgeGeom instanceof LineString) {
                                // allConnectingEdges.add(edge); // instead of this, use a map now, so we don't have similar edge many times
                                connectingEdgesMap.put(new ReversibleLineStringWrapper(
                                        (LineString) edgeGeom), edge);
                            }
                        }
                    }
                    Collection<Edge> vertexEdgesOut = state.getVertex().getOutgoing();
                    for (Iterator<Edge> iterator = vertexEdgesOut.iterator(); iterator.hasNext();) {
                        Edge edge = (Edge) iterator.next();
                        Geometry edgeGeom = edge.getGeometry();
                        if (edgeGeom != null) {
                            if (edgeGeom instanceof LineString) {
                                // allConnectingEdges.add(edge); // instead of this, use a map now, so we don't similar edge many times
                                connectingEdgesMap.put(new ReversibleLineStringWrapper(
                                        (LineString) edgeGeom), edge);
                            }
                        }
                    }
                }// end : if(et < maxTime)
            }
            // --
            // points from list to array, for later
            coords = new Coordinate[visitedCoords.size()];
            int i = 0;
            for (Coordinate c : visitedCoords)
                coords[i++] = c;

            // connection edges from Map to List
            allConnectingEdges.clear();
            for (Edge tedge : connectingEdgesMap.values())
                allConnectingEdges.add(tedge);
        }
        StringWriter sw = new StringWriter();
        GeometryJSON geometryJSON = new GeometryJSON();
        //
        // -- create the different outputs ---
        //
        try {
            if (output.equals(SIsochrone.RESULT_TYPE_POINTS)) {
                // in case there was no road we create a circle and
                // and return those points
                if (noRoadNearBy) {
                    Geometry circleShape = createCirle(dropPoint, pathToStreet);
                    coords = circleShape.getCoordinates();
                }
                // -- the states/nodes with time elapsed <= X min.
                LOG.debug("write multipoint geom with {} points", coords.length);
                geometryJSON.write(gf.createMultiPoint(coords), sw);
                LOG.debug("done");
            } else if (output.equals(SIsochrone.RESULT_TYPE_SHED)) {

                Geometry geomsArray[] = null;
                // in case there was no road we create a circle
                if (noRoadNearBy) {
                    Geometry circleShape = createCirle(dropPoint, pathToStreet);
                    geometryJSON.write(circleShape, sw);
                } else {
                    if (maxTime > shedCalcMethodSwitchTimeInSec) { // eg., walkshed > 20 min
                        // -- create a point-based walkshed
                        // less exact and should be used for large walksheds with many edges
                        LOG.debug("create point-based shed (not from edges)");
                        geomsArray = new Geometry[coords.length];
                        for (int j = 0; j < geomsArray.length; j++) {
                            geomsArray[j] = gf.createPoint(coords[j]);
                        }
                    } else {
                        // -- create an edge-based walkshed
                        // it is more exact and should be used for short walks
                        LOG.debug("create edge-based shed (not from points)");
                        Map<ReversibleLineStringWrapper, LineString> walkShedEdges = Maps
                                .newHashMap();
                        // add the walk from the pushpin to closest street point
                        walkShedEdges.put(new ReversibleLineStringWrapper(pathToStreet),
                                pathToStreet);
                        // get the edges and edge parts within time limits
                        ArrayList<LineString> withinTimeEdges = this
                                .getLinesAndSubEdgesWithinMaxTime(maxTime, allConnectingEdges,
                                        sptA, angleLimitForUShapeDetection,
                                        distanceToleranceForUShapeDetection, maxUserSpeed, usesCar,
                                        doSpeedTest);
                        for (LineString ls : withinTimeEdges) {
                            walkShedEdges.put(new ReversibleLineStringWrapper(ls), ls);
                        }
                        geomsArray = new Geometry[walkShedEdges.size()];
                        int k = 0;
                        for (LineString ls : walkShedEdges.values())
                            geomsArray[k++] = ls;
                    } // end if-else: maxTime condition
                    GeometryCollection gc = gf.createGeometryCollection(geomsArray);
                    // create the concave hull, but in case it fails we just return the convex hull
                    Geometry outputHull = null;
                    LOG.debug(
                            "create concave hull from {} geoms with edge length limit of about {} m (distance on meridian)",
                            geomsArray.length, concaveHullAlpha * 111132);
                    // 1deg at Latitude phi = 45deg is about 111.132km
                    // (see wikipedia: http://en.wikipedia.org/wiki/Latitude#The_length_of_a_degree_of_latitude)
                    try {
                        ConcaveHull hull = new ConcaveHull(gc, concaveHullAlpha);
                        outputHull = hull.getConcaveHull();
                    } catch (Exception e) {
                        outputHull = gc.convexHull();
                        LOG.debug("Could not generate ConcaveHull for WalkShed, using ConvexHull instead.");
                    }
                    LOG.debug("write shed geom");
                    geometryJSON.write(outputHull, sw);
                    LOG.debug("done");
                }
            } else if (output.equals(SIsochrone.RESULT_TYPE_EDGES)) {
                // in case there was no road we return only the suggested path to the street
                if (noRoadNearBy) {
                    geometryJSON.write(pathToStreet, sw);
                } else {
                    // -- if we would use only the edges from the paths to the origin we will miss
                    // some edges that will be never on the shortest path (e.g. loops/crescents).
                    // However, we can retrieve all edges by checking the times for each
                    // edge end-point
                    Map<ReversibleLineStringWrapper, LineString> walkShedEdges = Maps.newHashMap();
                    // add the walk from the pushpin to closest street point
                    walkShedEdges.put(new ReversibleLineStringWrapper(pathToStreet), pathToStreet);
                    // get the edges and edge parts within time limits
                    ArrayList<LineString> withinTimeEdges = this
                            .getLinesAndSubEdgesWithinMaxTime(maxTime, allConnectingEdges, sptA,
                                    angleLimitForUShapeDetection,
                                    distanceToleranceForUShapeDetection, maxUserSpeed, usesCar,
                                    doSpeedTest);
                    for (LineString ls : withinTimeEdges) {
                        walkShedEdges.put(new ReversibleLineStringWrapper(ls), ls);
                    }
                    Geometry mls = null;
                    LineString edges[] = new LineString[walkShedEdges.size()];
                    int k = 0;
                    for (LineString ls : walkShedEdges.values())
                        edges[k++] = ls;
                    LOG.debug("create multilinestring from {} geoms", edges.length);
                    mls = gf.createMultiLineString(edges);
                    LOG.debug("write geom");
                    geometryJSON.write(mls, sw);
                    LOG.debug("done");
                }
            } else if (output.equals("DEBUGEDGES")) {
                // -- for debugging, i.e. display of detected u-shapes/crescents
                ArrayList<LineString> withinTimeEdges = this.getLinesAndSubEdgesWithinMaxTime(
                        maxTime, allConnectingEdges, sptA, angleLimitForUShapeDetection,
                        distanceToleranceForUShapeDetection, maxUserSpeed, usesCar, doSpeedTest);
                if (this.showTooFastEdgesAsDebugGeomsANDnotUShapes) {
                    LOG.debug("displaying edges that are traversed too fast");
                    this.debugGeoms = this.tooFastTraversedEdgeGeoms;
                } else {
                    LOG.debug("displaying detected u-shaped roads/crescents");
                }
                LineString edges[] = new LineString[this.debugGeoms.size()];
                int k = 0;
                for (Iterator iterator = debugGeoms.iterator(); iterator.hasNext();) {
                    LineString ls = (LineString) iterator.next();
                    edges[k] = ls;
                    k++;
                }
                Geometry mls = gf.createMultiLineString(edges);
                LOG.debug("write debug geom");
                geometryJSON.write(mls, sw);
                LOG.debug("done");
            }
        } catch (Exception e) {
            LOG.error("Exception creating isochrone", e);
        }
        return sw.toString();
    }

    /**
     * Creates a circle shape, using the JTS buffer algorithm. The method is used when there is no street found within the given traveltime, e.g. when
     * the pointer is placed on a field or in the woods.<br>
     * TODO: Note it is actually not correct to do buffer calculation in Euclidian 2D, since the resulting shape will be elliptical when projected.
     * 
     * @param dropPoint the location given by the user
     * @param pathToStreet the path from the dropPoint to the street, used to retrieve the buffer distance
     * @return a Circle
     */
    private Geometry createCirle(Coordinate dropPoint, LineString pathToStreet) {
        double length = pathToStreet.getLength();
        GeometryFactory gf = new GeometryFactory();
        Point dp = gf.createPoint(dropPoint);
        Geometry buffer = dp.buffer(length);
        return buffer;
    }

    /**
     * Extraction of a sub-LineString from an existing line, starting from 0;
     * 
     * @param ls the line from which we extract the sub LineString ()
     * @param fraction [0..1], the length until where we want the substring to go
     * @return the sub-LineString
     */
    LineString getSubLineString(LineString ls, double fraction) {
        if (fraction >= 1)
            return ls;
        LengthIndexedLine linRefLine = new LengthIndexedLine(ls);
        LineString subLine = (LineString) linRefLine.extractLine(0, fraction * ls.getLength());
        return subLine;
    }

    /**
     * Filters all input edges and returns all those as LineString geometries, that have at least one end point within the time limits. If they have
     * only one end point inside, then the sub-edge is returned.
     * 
     * @param maxTime the time limit in seconds that defines the size of the walkshed
     * @param allConnectingStateEdges all Edges that have been found to connect all states < maxTime
     * @param spt the ShortestPathTree generated for the pushpin drop point as origin
     * @param angleLimit the angle tolerance to detect roads with u-shapes, i.e. Pi/2 angles, in Radiant.
     * @param distanceTolerance in percent (e.g. 1.1 = 110%) for u-shape detection based on distance criteria
     * @param hasCar is travel mode by CAR?
     * @param performSpeedTest if true applies a test to each edge to check if the edge can be traversed in time. The test can detect u-shaped roads.
     * @return
     */
    ArrayList<LineString> getLinesAndSubEdgesWithinMaxTime(long maxTime,
            ArrayList<Edge> allConnectingStateEdges, ShortestPathTree spt, double angleLimit,
            double distanceTolerance, double userSpeed, boolean hasCar, boolean performSpeedTest) {

        LOG.debug("maximal userSpeed set to: " + userSpeed + " m/sec ");
        if (hasCar) {
            LOG.debug("travel mode is set to CAR, hence the given speed may be adjusted for each edge");
        }

        ArrayList<LineString> walkShedEdges = new ArrayList<LineString>();
        ArrayList<LineString> otherEdges = new ArrayList<LineString>();
        ArrayList<LineString> borderEdges = new ArrayList<LineString>();
        ArrayList<LineString> uShapes = new ArrayList<LineString>();
        int countEdgesOutside = 0;
        // -- determination of walkshed edges via edge states
        for (Iterator iterator = allConnectingStateEdges.iterator(); iterator.hasNext();) {
            Edge edge = (Edge) iterator.next();
            State sFrom = spt.getState(edge.getFromVertex());
            State sTo = spt.getState(edge.getToVertex());
            if ((sFrom != null) && (sTo != null)) {
                long fromTime = sFrom.getElapsedTimeSeconds();
                long toTime = sTo.getElapsedTimeSeconds();
                long dt = Math.abs(toTime - fromTime);
                Geometry edgeGeom = edge.getGeometry();
                if ((edgeGeom != null) && (edgeGeom instanceof LineString)) {
                    LineString ls = (LineString) edgeGeom;
                    // detect u-shape roads/crescents - they need to be treated separately
                    boolean uShapeOrLonger = testForUshape(edge, maxTime, fromTime, toTime,
                            angleLimit, distanceTolerance, userSpeed, hasCar, performSpeedTest);
                    if (uShapeOrLonger) {
                        uShapes.add(ls);
                    }

                    // evaluate if an edge is completely within the time or only with one end
                    if ((fromTime < maxTime) && (toTime < maxTime)) {
                        // this one is within the time limit on both ends, however we need to do
                        // a second test if we have a u-shaped road.
                        if (uShapeOrLonger) {
                            treatAndAddUshapeWithinTimeLimits(maxTime, userSpeed, walkShedEdges,
                                    edge, fromTime, toTime, ls, hasCar);
                        } else {
                            walkShedEdges.add(ls);
                        }
                    }// end if:fromTime & toTime < maxTime
                    else {
                        // check if at least one end is inside, because then we need to
                        // create the sub edge
                        if ((fromTime < maxTime) || (toTime < maxTime)) {
                            double lineDist = edge.getDistance();
                            LineString inputLS = ls;
                            double fraction = 1.0;
                            if (fromTime < toTime) {
                                double distanceToWalkInTimeMissing = distanceToMoveInRemainingTime(
                                        maxTime, fromTime, dt, userSpeed, edge, hasCar,
                                        uShapeOrLonger);
                                fraction = (double) distanceToWalkInTimeMissing / (double) lineDist;
                            } else {
                                // toTime < fromTime : invert the edge direction
                                inputLS = (LineString) ls.reverse();
                                double distanceToWalkInTimeMissing = distanceToMoveInRemainingTime(
                                        maxTime, toTime, dt, userSpeed, edge, hasCar,
                                        uShapeOrLonger);
                                fraction = (double) distanceToWalkInTimeMissing / (double) lineDist;
                            }
                            // get the subedge
                            LineString subLine = this.getSubLineString(inputLS, fraction);
                            borderEdges.add(subLine);
                        } else {
                            // this edge is completely outside - this should actually not happen
                            // we will not do anything, just count
                            countEdgesOutside++;
                        }
                    }// end else: fromTime & toTime < maxTime
                }// end if: edge instance of LineString
                else {
                    // edge is not instance of LineString
                    LOG.debug("edge not instance of LineString");
                }
            }// end if(sFrom && sTo != null) start Else
            else {
                // LOG.debug("could not retrieve state for edge-endpoint"); //for a 6min car ride, there can be (too) many of such messages
                Geometry edgeGeom = edge.getGeometry();
                if ((edgeGeom != null) && (edgeGeom instanceof LineString)) {
                    otherEdges.add((LineString) edgeGeom);
                }
            }// end else: sFrom && sTo != null
        }// end for loop over edges
        walkShedEdges.addAll(borderEdges);
        this.debugGeoms.addAll(uShapes);
        LOG.debug("number of detected u-shapes/crescents: " + uShapes.size());
        return walkShedEdges;
    }

    private void treatAndAddUshapeWithinTimeLimits(long maxTime, double userSpeed,
            ArrayList<LineString> walkShedEdges, Edge edge, long fromTime, long toTime,
            LineString ls, boolean hasCar) {

        // check if the u-shape can be traveled within the remaining time
        long dt = Math.abs(toTime - fromTime);
        double distanceToMoveInTimeMissing = distanceToMoveInRemainingTime(maxTime, fromTime, dt,
                userSpeed, edge, hasCar, true);
        double lineDist = edge.getDistance();
        double fraction = (double) distanceToMoveInTimeMissing / (double) lineDist;
        // get the sub-edge geom
        LineString subLine = null;
        if (fraction < 1.0) {
            // the u-shape is not fully walkable in maxTime
            subLine = this.getSubLineString(ls, fraction);
            walkShedEdges.add(subLine);
            // if it is smaller we need also to calculate the LS from the other side
            LineString reversedLine = (LineString) ls.reverse();
            double distanceToMoveInTimeMissing2 = distanceToMoveInRemainingTime(maxTime, toTime,
                    dt, userSpeed, edge, hasCar, true);
            double fraction2 = (double) distanceToMoveInTimeMissing2 / (double) lineDist;
            LineString secondsubLine = this.getSubLineString(reversedLine, fraction2);
            ;
            walkShedEdges.add(secondsubLine);
        } else { // the whole u-shape is within the time
                 // add only once
            walkShedEdges.add(ls);
        }
    }

    private boolean testForUshape(Edge edge, long maxTime, long fromTime, long toTime,
            double angleLimit, double distanceTolerance, double userSpeed, boolean hasCar,
            boolean performSpeedTest) {

        LineString ls = (LineString) edge.getGeometry();
        if (ls.getNumPoints() <= 3) { // first filter since u-shapes need at least 4 pts
            // this is the normal case
            return false;
        } else {
            // try to identify u-shapes by checking if the angle EndPoint-StartPoint-StartPoint+1
            // is about 90 degrees (using Azimuths on the sphere)
            double diffTo90Azimuths = 360;
            if (edge instanceof StreetEdge) {
                double firstSegmentAngle = DirectionUtils.getFirstAngle(edge.getGeometry());
                if (firstSegmentAngle < 0)
                    firstSegmentAngle = firstSegmentAngle + Math.PI;
                double firstToLastSegmentAngle = getFirstToLastSegmentAngle(edge.getGeometry());
                if (firstToLastSegmentAngle < 0)
                    firstToLastSegmentAngle = firstToLastSegmentAngle + Math.PI;
                double diffAzimuths = Math.abs(firstToLastSegmentAngle - firstSegmentAngle);
                diffTo90Azimuths = Math.abs(diffAzimuths - (Math.PI / 2.0));
            } else {
                // this will happen in particular for transit routes
                // LOG.debug("Edge is not a PlainStreetEdge");
            }
            if (diffTo90Azimuths < angleLimit) {
                // no need to test further if we know its a u-shape
                // System.out.println("u-shape found, (spherical) angle: " + diffTo90Azimuths* 180/Math.PI);
                return true;
            } else {
                if (performSpeedTest) {
                    // Use also a distance based criteria since the angle criteria may fail.
                    // However a distance based one may fail as well for steep terrain.
                    long dt = Math.abs(toTime - fromTime);
                    double lineDist = edge.getDistance();
                    double distanceToWalkInTimeMissing = distanceToMoveInRemainingTime(maxTime,
                            fromTime, dt, userSpeed, edge, hasCar, false);
                    double approxWalkableDistanceInTime = distanceToWalkInTimeMissing
                            * distanceTolerance;
                    if ((approxWalkableDistanceInTime < lineDist)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * Calculates what distance can be traveled with the remaining time and given speeds. For car use the speed limit is taken from the edge itself.
     * Slopes are accounted for when walking and biking. A minimal slope of 0.06 (6m/100m) is necessary.
     * 
     * @param maxTime in sec, the time we have left
     * @param fromTime in sec, the time when we enter the edge
     * @param traverseTime in sec, original edge traverse time needed to adjust the speed based calculation to slope effects
     * @param userSpeed in m/sec, dependent on traversal mode
     * @param edge the edge itself (used to the get the speed in car mode)
     * @param usesCar if we traverse the edge in car mode
     * @param hasUshape if know, indicate if the edge has a u-shape
     * @return the distance in meter that can be moved until maxTime
     */
    double distanceToMoveInRemainingTime(long maxTime, long fromTime, double traverseTime,
            double userSpeed, Edge edge, boolean usesCar, boolean hasUshape) {

        boolean isTooFast = false;
        String msg = "";

        double originalTravelSpeed = edge.getDistance() / traverseTime; // this may be wrong for u-shapes

        if (originalTravelSpeed < userSpeed) {
            // we may have slope effects
            if (edge instanceof StreetEdge) {
                StreetEdge pe = (StreetEdge) edge;
                double maxSlope = pe.getMaxSlope();
                // if we are over the slope limit, then we should use the slower speed
                if (maxSlope > 0.06) { // limit 6m/100m = 3.4 degree
                    userSpeed = originalTravelSpeed;
                }
            }
        } else {
            // in this case we may have a u-shape, or the user speeds are too small, or something else.
            double vdiff = Math.abs(originalTravelSpeed - userSpeed);
            double vDiffPercent = vdiff / (userSpeed / 100.0);
            if (vDiffPercent > 20) {
                isTooFast = true;
                // [sstein Dec 2012]: Note, it seems like most of these edges are indeed of u-shape type,
                // i.e. small roads that come from and return from (the same) main road
                msg = "v_traversed is much faster than (allowed) v_user, edgeName: "
                        + edge.getName() + ", >>> (in m/s): v_traversed="
                        + (int) Math.floor(originalTravelSpeed) + ", v_maxUser="
                        + (int) Math.floor(userSpeed);
                if (hasUshape) {
                    msg = msg + ", known u-shape, ";
                }
                if ((usesCar == false) && (hasUshape == false)) {
                    this.tooFastTraversedEdgeGeoms.add(edge.getGeometry());
                    LOG.debug(msg);
                } // otherwise we print msg below
            }
        }
        // correct speed for car use, as each road has its speed limits
        if (usesCar) {
            if (edge instanceof StreetEdge) {
                StreetEdge pe = (StreetEdge) edge;
                userSpeed = pe.getCarSpeed();
                // we need to check again if the originalTravelSpeed is faster
                if ((isTooFast == true) && (originalTravelSpeed > userSpeed)
                        && (hasUshape == false)) {
                    this.tooFastTraversedEdgeGeoms.add(edge.getGeometry());
                    LOG.debug(msg + "; setting v_PlainStreetEdge=" + (int) Math.floor(userSpeed));
                }
            }
        }
        // finally calculate how far we can travel with the remaining time
        long timeMissing = maxTime - fromTime;
        double distanceToWalkInTimeMissing = timeMissing * userSpeed;
        return distanceToWalkInTimeMissing;
    }

    private GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

    /**
     * Computes the angle from the first point to the last point of a LineString or MultiLineString. TODO: put this method into
     * org.opentripplanner.common.geometry.DirectionUtils
     * 
     * @param geometry a LineString or a MultiLineString
     * 
     * @return
     */
    public synchronized double getFirstToLastSegmentAngle(Geometry geometry) {
        LineString line;
        if (geometry instanceof MultiLineString) {
            line = (LineString) geometry.getGeometryN(geometry.getNumGeometries() - 1);
        } else {
            assert geometry instanceof LineString;
            line = (LineString) geometry;
        }
        int numPoints = line.getNumPoints();
        Coordinate coord0 = line.getCoordinateN(0);
        Coordinate coord1 = line.getCoordinateN(numPoints - 1);
        int i = numPoints - 3;
        while (SphericalDistanceLibrary.fastDistance(coord0, coord1) < 10 && i >= 0) {
            coord1 = line.getCoordinateN(i--);
        }

        geodeticCalculator.setStartingGeographicPoint(coord0.x, coord0.y);
        geodeticCalculator.setDestinationGeographicPoint(coord1.x, coord1.y);
        return geodeticCalculator.getAzimuth() * Math.PI / 180;
    }
}
