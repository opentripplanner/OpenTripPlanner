package org.opentripplanner.graph_builder.impl.raptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.impl.raptor.MaxTransitRegions;
import org.opentripplanner.routing.impl.raptor.MaxWalkState;
import org.opentripplanner.routing.impl.raptor.RaptorData;
import org.opentripplanner.routing.impl.raptor.RaptorDataService;
import org.opentripplanner.routing.impl.raptor.RaptorRoute;
import org.opentripplanner.routing.impl.raptor.RaptorStop;
import org.opentripplanner.routing.impl.raptor.RegionData;
import org.opentripplanner.routing.impl.raptor.RouteSegmentComparator;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

public class RaptorDataBuilder implements GraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(RaptorDataBuilder.class);

    private static final int MAX_REGION_SIZE = 80000;

    private static final double MIN_SPEED = 1.33;

    private static final double MAX_DISTANCE = 3218;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private int MAX_TRANSFERS = 7;

    @SuppressWarnings("unchecked")
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        RaptorData data = new RaptorData();

        TransitIndexService transitIndex = graph.getService(TransitIndexService.class);

        int nTotalStops = 0;
        for (Vertex v : graph.getVertices()) {
            if (v instanceof TransitStop) {
                nTotalStops++;
            }
        }
        data.routesForStop = new List[nTotalStops];

        data.stops = new RaptorStop[nTotalStops];

        for (String agency : transitIndex.getAllAgencies()) {
            for (RouteVariant variant : transitIndex.getVariantsForAgency(agency)) {
                ArrayList<Stop> variantStops = variant.getStops();
                final int nStops = variantStops.size();

                int nPatterns = variant.getSegments().size() / nStops;
                RaptorRoute route = new RaptorRoute(nStops, nPatterns);
                data.routes.add(route);

                for (int i = 0; i < nStops; ++i) {

                    final Stop stop = variantStops.get(i);
                    RaptorStop raptorStop = makeRaptorStop(data, stop);
                    route.stops[i] = raptorStop;
                    if (data.routesForStop[raptorStop.index] == null)
                        data.routesForStop[raptorStop.index] = new ArrayList<RaptorRoute>();
                    data.routesForStop[raptorStop.index].add(route);
                }

                List<RouteSegment> segments = variant.getSegments();
                // this sorter ensures that route segments are ordered by stop sequence, and, at a
                // given stop, patterns are in a consistent order
                Collections.sort(segments, new RouteSegmentComparator());
                int stop = 0;
                int pattern = 0;
                for (RouteSegment segment : segments) {
                    if (stop != nStops - 1) {
                        for (Edge e : segment.board.getFromVertex().getIncoming()) {
                            if (e instanceof PreBoardEdge) {
                                route.stops[stop].stopVertex = (TransitStop) e.getFromVertex();
                            }
                        }
                        route.boards[stop][pattern] = (TransitBoardAlight) segment.board;
                    }
                    if (stop != 0) {
                        for (Edge e : segment.alight.getToVertex().getOutgoing()) {
                            if (e instanceof PreAlightEdge) {
                                route.stops[stop].stopVertex = (TransitStop) e.getToVertex();
                            }
                        }

                        route.alights[stop - 1][pattern] = (TransitBoardAlight) segment.alight;
                    }
                    if (++pattern == nPatterns) {
                        pattern = 0;
                        stop++;
                    }
                }
                if (stop != nStops || pattern != 0) {
                    throw new RuntimeException("Wrong number of segments");
                }
            }
        }

        data.stops = Arrays.copyOfRange(data.stops, 0, data.raptorStopsForStopId.size());
        nTotalStops = data.stops.length;
        // initNearbyStops();

        //MaxTransitRegions regions = makeMaxTransitRegions(graph, data);
        //data.maxTransitRegions = regions;
        graph.putService(RaptorDataService.class, new RaptorDataService(data));

    }

    private MaxTransitRegions makeMaxTransitRegions(Graph graph, RaptorData data) {
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        for (Vertex v : graph.getVertices()) {
            if (v instanceof TransitStop) {
                vertices.add(v);
            }
        }

        ArrayList<ArrayList<Vertex>> verticesForRegion = new ArrayList<ArrayList<Vertex>>();
        int nRegions = split(verticesForRegion, vertices, 0, true, 1000);

        for (int region = 0; region < verticesForRegion.size(); ++region) {
            for (Vertex vertex : verticesForRegion.get(region)) {
                vertex.setGroupIndex(region);
            }
        }

        MaxTransitRegions regions = new MaxTransitRegions();

        // compute stop-to-stop walk times
        HashMap<Vertex, T2<Integer, Double>>[] stopToStopWalkTimes = computeStopToStopWalkTimes(
                vertices, MIN_SPEED, MAX_DISTANCE);
        regions.minSpeed = MIN_SPEED;
        regions.maxDistance = MAX_DISTANCE;

        final int NDAYS = 5;

        CalendarService calendarService = graph.getCalendarService();
        TimeZone timeZone = graph.getTimeZone();

        // get start of today
        Calendar calendar = Calendar.getInstance(timeZone);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        regions.startYear = year;
        regions.startMonth = month;
        regions.startDay = day;

        regions.maxTransit = new int[NDAYS][nRegions][nRegions];

        // todo slack
        Map<List<ServiceDay>, int[][]> cache = new HashMap<List<ServiceDay>, int[][]>();

        for (int d = 0; d < NDAYS; ++d) {
            calendar = Calendar.getInstance(timeZone);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            int yesterday = (int) (calendar.getTime().getTime() / 1000);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            int today = (int) (calendar.getTime().getTime() / 1000);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            int tomorrow = (int) (calendar.getTime().getTime() / 1000);

            ArrayList<ServiceDay> serviceDays = new ArrayList<ServiceDay>();
            for (String agency : graph.getAgencyIds()) {
                serviceDays.add(new ServiceDay(graph, yesterday, calendarService, agency));
                serviceDays.add(new ServiceDay(graph, today, calendarService, agency));
                serviceDays.add(new ServiceDay(graph, tomorrow, calendarService, agency));
            }

            int[][] cached = cache.get(serviceDays);
            if (cached != null) {
                log.debug("using cached values for " + calendar);
                regions.maxTransit[d] = cached;
                continue;
            }

            log.debug("Computing max transit data for day " + d);
            for (int region = 0; region < nRegions; ++region) {
                log.debug("Computing max transit data for region " + region);
                regions.maxTransit[d][region] = computeMaxTransitData(data, serviceDays,
                        verticesForRegion.get(region), stopToStopWalkTimes, today, nRegions);
            }
        }

        return regions;
    }

    private HashMap<Vertex, T2<Integer, Double>>[] computeStopToStopWalkTimes(
            ArrayList<Vertex> vertices, double minSpeed, double maxDistance) {
        log.debug("Finding stop-to-stop walk times");
        @SuppressWarnings("unchecked")
        HashMap<Vertex, T2<Integer, Double>>[] times = new HashMap[AbstractVertex.getMaxIndex()];
        RoutingRequest walkOptions = new RoutingRequest(TraverseMode.WALK);
        walkOptions.setWalkSpeed(minSpeed);
        walkOptions.setArriveBy(true);
        walkOptions.setMaxWalkDistance(maxDistance);
        GenericDijkstra dijkstra = new GenericDijkstra(walkOptions);
        for (Vertex destination : vertices) {
            final HashMap<Vertex, T2<Integer, Double>> timesByDestination = new HashMap<Vertex, T2<Integer, Double>>();
            times[destination.getIndex()] = timesByDestination;
            State initialState = new MaxWalkState(destination, walkOptions);
            ShortestPathTree spt = dijkstra.getShortestPathTree(initialState);
            for (State state : spt.getAllStates()) {
                Vertex vertex = state.getVertex();
                if (vertex instanceof TransitStop) {
                    T2<Integer, Double> timeAndDistance = new T2<Integer, Double>(
                            (int) state.getElapsedTime(), state.getWalkDistance());
                    timesByDestination.put(vertex, timeAndDistance);
                }
            }
        }

        return times;
    }

    class RouteAlight {

        public StopProfile destinationProfiel;

        public int stopNo;

    }

    private int[] computeMaxTransitData(RaptorData data, ArrayList<ServiceDay> serviceDays,
            ArrayList<Vertex> destinations, HashMap<Vertex, T2<Integer, Double>>[] stopToStopWalk,
            int startTime, int nRegions) {

        HashMap<Vertex, StopProfile> stopProfile = new HashMap<Vertex, StopProfile>();
        // initialize stop profiles for destination vertices

        HashSet<Vertex> visitedLastRound = new HashSet<Vertex>();

        for (Vertex v : destinations) {
            stopProfile.put(v, new StopProfile(v, true));
            visitedLastRound.add(v);
        }

        for (int round = 0; round < MAX_TRANSFERS ; ++round) {
            System.out.println("round " + round + " from " + visitedLastRound.size());
            HashSet<Vertex> visitedThisRound = new HashSet<Vertex>();
            // transit phase
            HashSet<StopProfile> newlyBoarded = new HashSet<StopProfile>();
            for (RaptorRoute route : data.routes) {
                boolean started = false;
                List<RouteAlight> alightings = new ArrayList<RouteAlight>();
                for (int stopNo = route.getNStops() - 1; stopNo >= 0; --stopNo) {

                    // try boarding here
                    RaptorStop stop = route.stops[stopNo];
                    TransitStop stopVertex = stop.stopVertex;
                    if (!started && !visitedLastRound.contains(stopVertex))
                        continue;
                    started = true;
                    StopProfile proflie = stopProfile.get(stopVertex);
                    if (proflie == null) {
                        proflie = new StopProfile(stopVertex);
                        stopProfile.put(stopVertex, proflie);
                    }
                    for (RouteAlight alight : alightings) {
                        if (proflie.transitTo(alight.destinationProfiel, route, stopNo,
                                alight.stopNo, serviceDays, startTime)) {
                            visitedThisRound.add(stopVertex);

                            newlyBoarded.add(proflie);
                        }
                    }

                    // try alighting here
                    if (visitedLastRound.contains(stopVertex)) {
                        final RouteAlight routeAlight = new RouteAlight();
                        routeAlight.stopNo = stopNo;
                        routeAlight.destinationProfiel = proflie;
                        alightings.add(routeAlight);
                    }
                }
            }

            // walk phase
            for (StopProfile profile : newlyBoarded) {
                Vertex vertex = profile.vertex;
                HashMap<Vertex, T2<Integer, Double>> nearbyStops = stopToStopWalk[vertex.getIndex()];
                if (nearbyStops == null)
                    continue;
                for (Map.Entry<Vertex, T2<Integer, Double>> nearbyStop : nearbyStops.entrySet()) {
                    Vertex nearbyVertex = nearbyStop.getKey();
                    // no need to walk from a stop to itself
                    if (nearbyVertex == vertex)
                        continue;

                    T2<Integer, Double> timeAndDistance = nearbyStop.getValue();
                    int time = timeAndDistance.getFirst();
                    double distance = timeAndDistance.getSecond();
                    StopProfile nearbyProfile = stopProfile.get(nearbyVertex);
                    if (nearbyProfile == null) {
                        nearbyProfile = new StopProfile(nearbyVertex);
                        stopProfile.put(nearbyVertex, nearbyProfile);
                    }
                    if (nearbyProfile.walkTo(profile, time, distance)) {
                        visitedThisRound.add(vertex);
                    }
                }
            }
            visitedLastRound = visitedThisRound;
        }

        int[] timeForRegion = new int[nRegions];
        for (RaptorStop stop : data.stops) {
            TransitStop vertex = stop.stopVertex;
            int region = vertex.getGroupIndex();
            if (region < 0) {
                log.warn("Missing region for " + vertex);
                continue; // this should never happen
            }
            StopProfile profile = stopProfile.get(vertex);
            if (profile == null) { // unreachable stop inside this region
                timeForRegion[region] = Integer.MAX_VALUE;
            } else {
                int duration = profile.getMaxDuration(0, 86400 + 3600);
                if (duration > timeForRegion[region]) {
                    timeForRegion[region] = duration;
                }
            }
        }

        return timeForRegion;
    }

    @SuppressWarnings("unused")
    private RegionData makeRegions(Graph graph, RaptorData data) {
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        for (Vertex v : graph.getVertices()) {
            if (!(v instanceof OnboardVertex)) {
                vertices.add(v);
            }
        }

        ArrayList<ArrayList<Vertex>> verticesForRegion = new ArrayList<ArrayList<Vertex>>();
        int nRegions = split(verticesForRegion, vertices, 0, true, MAX_REGION_SIZE);
        RegionData regions = new RegionData();
        // regions.minWalk = new double[verticesForRegion.size()][verticesForRegion.size()];
        regions.minTime = new int[nRegions][nRegions];

        // now compute minWalk for each region

        int regionIndex = 0;
        for (ArrayList<Vertex> region : verticesForRegion) {
            if (regionIndex % 5 == 0) {
                log.debug("Building regions: " + regionIndex + " / " + verticesForRegion.size());
            }
            // findMinWalkDistance(data, regions, regionIndex, region);
            findMinTime(graph, data, regions, regionIndex, region);
            regionIndex += 1;
        }

        return regions;
    }

    @SuppressWarnings("unused")
    private void findMinWalkDistance(RaptorData data, RegionData regions, int regionIndex,
            ArrayList<Vertex> region) {
        // find initial spt from all nodes in region
        HashMap<Vertex, Double> distances = new HashMap<Vertex, Double>();
        BinHeap<Vertex> queue = new BinHeap<Vertex>();
        for (Vertex v : region) {
            queue.insert(v, 0);
            distances.put(v, 0.0);
        }

        // walk-distance free-transit spt computation
        HashSet<Vertex> closed = new HashSet<Vertex>();
        while (!queue.empty()) {
            Vertex u = queue.extract_min();
            if (closed.contains(u))
                continue;
            closed.add(u);
            double distance = distances.get(u);
            for (Edge e : u.getOutgoing()) {
                if (!((e instanceof StreetEdge) || (e instanceof StreetTransitLink)))
                    continue;
                double edgeDistance = e.getDistance() + distance;
                Vertex v = e.getToVertex();
                Double originalDistance = distances.get(v);
                if (originalDistance == null || originalDistance > edgeDistance) {
                    distances.put(v, edgeDistance);
                    queue.insert(v, edgeDistance);
                }
                if (v instanceof TransitStop) {
                    RaptorStop stop = data.raptorStopsForStopId.get(((TransitStop) v).getStopId());
                    if (stop == null)
                        continue;
                    for (RaptorRoute route : data.routesForStop[stop.index]) {
                        for (RaptorStop stopOnRoute : route.stops) {
                            Vertex stopVertex = stopOnRoute.stopVertex;
                            originalDistance = distances.get(stopVertex);
                            if (originalDistance == null || originalDistance > edgeDistance) {
                                distances.put(stopVertex, edgeDistance);
                                queue.insert(stopVertex, edgeDistance);
                            }
                        }
                    }
                }
            }
        }
        final double[] minWalk = regions.minWalk[regionIndex];
        Arrays.fill(minWalk, Double.MAX_VALUE);
        for (Map.Entry<Vertex, Double> entry : distances.entrySet()) {
            Vertex v = entry.getKey();
            double distance = entry.getValue();
            int toRegion = v.getGroupIndex();
            if (toRegion == -1) {
                System.out.println("Warning: no region for " + v);
                continue;
            }
            if (minWalk[toRegion] > distance) {
                minWalk[toRegion] = distance;
            }
        }
    }

    private void findMinTime(Graph graph, RaptorData data, RegionData regions, int regionIndex,
            ArrayList<Vertex> region) {
        // find initial spt from all nodes in region
        HashMap<Vertex, Integer> times = new HashMap<Vertex, Integer>();
        BinHeap<Vertex> queue = new BinHeap<Vertex>();
        for (Vertex v : region) {
            queue.insert(v, 0);
            times.put(v, 0);
        }
        RoutingRequest options = new RoutingRequest();
        options.setWalkSpeed(6); // assume slightly fast biking speeds, which should be a good bound
        // rctx ctor requires this
        graph.streetIndex = new StreetVertexIndexServiceImpl(graph);
        options.setRoutingContext(graph, region.get(0), null);
        options.rctx.serviceDays = new ArrayList<ServiceDay>();
        options.rctx.serviceDays.add(ServiceDay.universalService(graph));

        // walk-distance free-transit spt computation
        HashSet<Vertex> closed = new HashSet<Vertex>();
        while (!queue.empty()) {
            Vertex u = queue.extract_min();
            if (closed.contains(u))
                continue;
            closed.add(u);
            int time = times.get(u);
            for (Edge e : u.getOutgoing()) {
                final double timeLowerBound = e.timeLowerBound(options);
                if (Double.isNaN(timeLowerBound))
                    continue;
                int edgeTime = (int) (timeLowerBound + time);
                Vertex v = e.getToVertex();
                Integer originalTime = times.get(v);
                if (originalTime == null || originalTime > edgeTime) {
                    times.put(v, edgeTime);
                    queue.insert(v, (double) edgeTime);
                }
            }
        }
        final int[] minTime = regions.minTime[regionIndex];
        Arrays.fill(minTime, Integer.MAX_VALUE);
        for (Map.Entry<Vertex, Integer> entry : times.entrySet()) {
            Vertex v = entry.getKey();
            int distance = entry.getValue();
            // int toRegion = regions.regionForVertex[v.getIndex()];
            int toRegion = v.getGroupIndex();
            if (toRegion == -1) {
                continue;
            }
            if (minTime[toRegion] > distance) {
                minTime[toRegion] = distance;
            }
        }
    }

    class HorizontalVertexComparator implements Comparator<Vertex> {
        @Override
        public int compare(Vertex o1, Vertex o2) {
            double cmp = o1.getCoordinate().x - o2.getCoordinate().x;
            if (cmp == 0) {
                return 0;
            }
            return cmp > 0 ? 1 : -1;
        }
    }

    class VerticalVertexComparator implements Comparator<Vertex> {
        @Override
        public int compare(Vertex o1, Vertex o2) {
            double cmp = o1.getCoordinate().y - o2.getCoordinate().y;
            if (cmp == 0) {
                return 0;
            }
            return cmp > 0 ? 1 : -1;
        }
    }

    private int split(ArrayList<ArrayList<Vertex>> vertexForRegion, List<Vertex> vertices,
            int index, boolean horiz, int regionSize) {
        if (vertices.size() <= regionSize) {
            final ArrayList<Vertex> region = new ArrayList<Vertex>();
            vertexForRegion.add(region);
            for (Vertex vertex : vertices) {
                vertex.setGroupIndex(index);
                region.add(vertex);
            }
            return index + 1;
        }

        Comparator<Vertex> comparator = horiz ? new HorizontalVertexComparator()
                : new VerticalVertexComparator();
        Collections.sort(vertices, comparator);
        int mid = vertices.size() / 2;
        Coordinate last = vertices.get(mid - 1).getCoordinate();
        // we don't want to split two vertices with the same coordinate into different
        // regions, so move mid up until it includes all vertices with the last coordinate
        for (; mid < vertices.size(); ++mid) {
            if (!vertices.get(mid).getCoordinate().equals(last)) {
                break;
            }
        }

        // this split is too uneven -- just go ahead and make it one region
        if (mid > vertices.size() * 3 / 4) {
            final ArrayList<Vertex> region = new ArrayList<Vertex>();
            vertexForRegion.add(region);
            for (Vertex vertex : vertices) {
                vertex.setGroupIndex(index);
                region.add(vertex);
            }
            return index + 1;
        }
        index = split(vertexForRegion, vertices.subList(0, mid), index, !horiz, regionSize);
        index = split(vertexForRegion, vertices.subList(mid, vertices.size()), index, !horiz,
                regionSize);
        return index;
    }

    private RaptorStop makeRaptorStop(RaptorData data, Stop stop) {
        RaptorStop rs = data.raptorStopsForStopId.get(stop.getId());
        if (rs == null) {
            rs = new RaptorStop();
            rs.index = data.raptorStopsForStopId.size();
            data.stops[rs.index] = rs;
            data.raptorStopsForStopId.put(stop.getId(), rs);
        }
        return rs;
    }

    // this doesn't speed things up
    @SuppressWarnings({ "unchecked", "unused" })
    private void initNearbyStops(RaptorData data) {
        final int nTotalStops = data.stops.length;

        data.nearbyStops = new List[nTotalStops];
        for (int i = 0; i < nTotalStops; ++i) {
            if (i % 500 == 0) {
                System.out.println("Precomputing nearby stops:" + i + " / " + nTotalStops);
            }
            data.nearbyStops[i] = new ArrayList<T2<Double, RaptorStop>>();
            RaptorStop stop = data.stops[i];
            Coordinate coord = stop.stopVertex.getCoordinate();
            for (RaptorStop other : data.stops) {
                if (other == stop)
                    continue;
                Coordinate otherCoord = other.stopVertex.getCoordinate();
                if (Math.abs(otherCoord.x - coord.x) > 4850 / 111111.0) {
                    continue;
                }
                if (Math.abs(otherCoord.y - coord.y) > 4850 / 111111.0) {
                    continue;
                }
                double distance = distanceLibrary.fastDistance(coord, otherCoord);
                if (distance > 4850) // 3 mi
                    continue;
                data.nearbyStops[i].add(new T2<Double, RaptorStop>(distance, other));
            }
            Collections.sort(data.nearbyStops[i], new Comparator<T2<Double, RaptorStop>>() {

                @Override
                public int compare(T2<Double, RaptorStop> arg0, T2<Double, RaptorStop> arg1) {
                    return (int) Math.signum(arg0.getFirst() - arg1.getFirst());
                }

            });
        }
    }

    @Override
    public List<String> provides() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }

    @Override
    public void checkInputs() {

    }

}
