package org.opentripplanner.graph_builder.impl.raptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.raptor.RaptorData;
import org.opentripplanner.routing.impl.raptor.RaptorRoute;
import org.opentripplanner.routing.impl.raptor.RaptorStop;
import org.opentripplanner.routing.impl.raptor.RegionData;
import org.opentripplanner.routing.impl.raptor.RaptorDataService;
import org.opentripplanner.routing.impl.raptor.RouteSegmentComparator;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;

public class RaptorDataBuilder implements GraphBuilder {

    private static final int MAX_REGION_SIZE = 1000;
    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

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
                        route.boards[stop][pattern] = (PatternBoard) segment.board;
                    }
                    if (stop != 0) {
                        for (Edge e : segment.alight.getToVertex().getOutgoing()) {
                            if (e instanceof PreAlightEdge) {
                                route.stops[stop].stopVertex = (TransitStop) e.getToVertex();
                            }
                        }

                        route.alights[stop - 1][pattern] = (PatternAlight) segment.alight;
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

        RegionData regions = makeRegions(graph, data);
        data.regionData = regions;
        graph.putService(RaptorDataService.class, new RaptorDataService(data));

    }

    private RegionData makeRegions(Graph graph, RaptorData data) {
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        for (Vertex v : graph.getVertices()) {
            if (!(v instanceof OnboardVertex)) {
                vertices.add(v);
            }
        }
        int[] regionForVertex = new int[AbstractVertex.getMaxIndex()];
        Arrays.fill(regionForVertex, -1);
        
        ArrayList<ArrayList<Vertex>> verticesForRegion = new ArrayList<ArrayList<Vertex>>();
        split(regionForVertex, verticesForRegion, vertices, 0, true, MAX_REGION_SIZE);
        RegionData regions = new RegionData();
        regions.regionForVertex = regionForVertex;
        regions.minWalk = new double[verticesForRegion.size()][verticesForRegion.size()];

        // now compute minWalk for each region
        
        int regionIndex = 0;
        for (ArrayList<Vertex> region : verticesForRegion) {
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
                    if (!((e instanceof StreetEdge) || (e instanceof StreetTransitLink))) continue;
                    double edgeDistance = e.getDistance() + distance;
                    Vertex v = e.getToVertex();
                    Double originalDistance = distances.get(v);
                    if (originalDistance == null || originalDistance > edgeDistance) {
                        distances.put(v, edgeDistance);
                        queue.insert(v, edgeDistance);
                    }
                    if (v instanceof TransitStop) {
                        RaptorStop stop = data.raptorStopsForStopId.get(((TransitStop) v)
                                .getStopId());
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
                int toRegion = regions.regionForVertex[v.getIndex()];
                if (toRegion == -1) {
                    System.out.println("Warning: no region for " + v);
                    continue;
                }
                if (minWalk[toRegion] > distance) {
                    minWalk[toRegion] = distance;
                }
            }
            regionIndex += 1;
        }

        return regions;
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

    private int split(int[] regionForVertex, ArrayList<ArrayList<Vertex>> vertexForRegion,
            List<Vertex> vertices, int index, boolean horiz, int regionSize) {
        if (vertices.size() <= regionSize) {
            final ArrayList<Vertex> region = new ArrayList<Vertex>();
            vertexForRegion.add(region);
            for (Vertex vertex : vertices) {
                regionForVertex[vertex.getIndex()] = index;
                region.add(vertex);
            }
            return index + 1;
        }

        Comparator<Vertex> comparator = horiz ? new HorizontalVertexComparator()
                : new VerticalVertexComparator();
        Collections.sort(vertices, comparator);
        int mid = vertices.size() / 2;
        Coordinate last = vertices.get(mid - 1).getCoordinate();
        //we don't want to split two vertices with the same coordinate into different
        //regions, so move mid up until it includes all vertices with the last coordinate
        for (; mid < vertices.size(); ++mid) {
            if (!vertices.get(mid).getCoordinate().equals(last)) {
                break;
            }
        }
        
        //this split is too uneven -- just go ahead and make it one region
        if (mid > vertices.size() * 3 / 4) {
            final ArrayList<Vertex> region = new ArrayList<Vertex>();
            vertexForRegion.add(region);
            for (Vertex vertex : vertices) {
                regionForVertex[vertex.getIndex()] = index;
                region.add(vertex);
            }
            return index + 1;
        }
        index = split(regionForVertex, vertexForRegion, vertices.subList(0, mid), index, !horiz,
                regionSize);
        index = split(regionForVertex, vertexForRegion, vertices.subList(mid, vertices.size()),
                index, !horiz, regionSize);
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
