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

package org.opentripplanner.graph_builder.impl.shapefile;

import static org.opentripplanner.common.IterableLibrary.filter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.Test;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.model.GtfsBundles;
import org.opentripplanner.graph_builder.services.DisjointSet;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.contraction.ContractionHierarchy;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.contraction.ModeAndOptimize;
import org.opentripplanner.routing.contraction.Shortcut;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.edgetype.loader.NetworkLinker;
import org.opentripplanner.routing.impl.ContractionHierarchySerializationLibrary;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;

class NonFreeEdge extends FreeEdge {
    private static final long serialVersionUID = 1L;
    private double weight;

    public NonFreeEdge (Vertex v1, Vertex v2, double weight) {
        super(v1, v2);
        this.weight = weight;
    }
    
    @Override
    public TraverseResult traverse(State s0, TraverseOptions options) {
        State s1 = s0.clone();
        return new TraverseResult(weight, s1,this);
    }
    
    @Override
    public TraverseResult traverseBack(State s0, TraverseOptions options) {
        State s1 = s0.clone();
        return new TraverseResult(weight, s1,this);
    }
}

class ForbiddenEdge extends FreeEdge {
    private static final long serialVersionUID = 1L;

    public ForbiddenEdge(Vertex from, Vertex to) {
        super(from, to);
    }

    @Override
    public TraverseResult traverse(State s0, TraverseOptions options) {
        return null;
    }
    
    @Override
    public TraverseResult traverseBack(State s0, TraverseOptions options) {
        return null;
    }
}

public class TestContractionHeirarchies extends TestCase {

    @Test
    public void testBasic() {
        final int N = 10;

        Graph graph = new Graph();
        // create a NxN grid of vertices
        Vertex[][] verticesIn = new Vertex[N][];
        Vertex[][] verticesOut = new Vertex[N][];
        for (int y = 0; y < N; ++y) {
            verticesIn[y] = new Vertex[N];
            verticesOut[y] = new Vertex[N];
            for (int x = 0; x < N; ++x) {
                double xc = x * 0.001 - 71;
                double yc = y * 0.001 + 40;
                Vertex in = new EndpointVertex("(" + x + ", " + y + ") in", xc, yc);
                graph.addVertex(in);
                verticesIn[y][x] = in;

                Vertex out = new EndpointVertex("(" + x + ", " + y + ") out", xc, yc);
                graph.addVertex(out);
                verticesOut[y][x] = out;
            }
        }

        for (int y = 0; y < N; ++y) {
            for (int x = 0; x < N - 1; ++x) {
                double xc = x * 0.001 - 71;
                double yc = y * 0.001 + 40;
                LineString geometry = GeometryUtils.makeLineString(xc, yc, xc + 0.001, yc);
                double d = DistanceLibrary.distance(yc, xc, yc, xc + 0.001);
                StreetVertex left = new StreetVertex("a(" + x + ", " + y + ")", geometry, "", d, false);
                StreetVertex right = new StreetVertex("a(" + x + ", " + y + ")", (LineString) geometry.reverse(), "", d, true);
                
                graph.addVertex(left);
                graph.addVertex(right);

                d = DistanceLibrary.distance(xc, yc, xc + 0.001, yc);
                geometry = GeometryUtils.makeLineString(yc, xc, yc, xc + 0.001);
                StreetVertex down = new StreetVertex("d(" + y + ", " + x + ")", geometry, "", d, false);
                StreetVertex up = new StreetVertex("d(" + y + ", " + x + ")", (LineString) geometry.reverse(), "", d, true);

                graph.addVertex(down);
                graph.addVertex(up);
                
                graph.addEdge(new FreeEdge(verticesOut[y][x], left));
                graph.addEdge(new FreeEdge(verticesOut[x][y], down));
                
                graph.addEdge(new FreeEdge(verticesOut[y][x + 1], right));
                graph.addEdge(new FreeEdge(verticesOut[x + 1][y], up));
                
                graph.addEdge(new FreeEdge(right, verticesIn[y][x]));
                graph.addEdge(new FreeEdge(up, verticesIn[x][y]));
            
                graph.addEdge(new FreeEdge(left, verticesIn[y][x + 1]));
                graph.addEdge(new FreeEdge(down, verticesIn[x + 1][y]));
                
                assertEquals(left, graph.addVertex(left));
                assertEquals(right, graph.addVertex(right));
                assertEquals(down, graph.addVertex(down));
                assertEquals(up, graph.addVertex(up));
            }
        }

        for (int y = 0; y < N; ++y) {
            for (int x = 0; x < N; ++x) {
                Vertex vertexIn = verticesIn[y][x];
                for (DirectEdge e1: filter(graph.getIncoming(vertexIn),DirectEdge.class)) {
                    Vertex vertexOut = verticesOut[y][x];
                    StreetVertex fromv = (StreetVertex) e1.getFromVertex();
                    for (DirectEdge e2: filter(graph.getOutgoing(vertexOut),DirectEdge.class)) {
                        StreetVertex tov = (StreetVertex) e2.getToVertex();
                        if (tov.getEdgeId().equals(fromv.getEdgeId())) {
                            continue;
                        }
                        graph.addEdge(new TurnEdge(fromv, tov));
                    }
                    assertTrue(graph.getDegreeOut(fromv) <= 4);
                }
            }
        }
        
        final int graphSize = N * N * 2 + (N * (N - 1) * 4);
        assertEquals(graphSize, graph.getVertices().size());
        
        // test Dijkstra
        TraverseOptions options = new TraverseOptions();
        options.optimizeFor = OptimizeType.QUICK;
        options.walkReluctance = 1;
        options.speed = 1;

        // test hop limit
        Dijkstra dijkstra = new Dijkstra(graph, verticesOut[0][0], options, graph.getVertex("a(0, 0)"), 3);
        BasicShortestPathTree spt = dijkstra.getShortestPathTree(verticesIn[0][2], 4);
        SPTVertex v03 = spt.getVertex(verticesIn[0][3]);
        assertNull(v03);

        dijkstra = new Dijkstra(graph, verticesOut[0][0], options, graph.getVertex("a(0, 0)"), 6);
        spt = dijkstra.getShortestPathTree(verticesIn[0][3], 500);
        v03 = spt.getVertex(verticesIn[0][3]);
        assertNotNull(v03);

        // test distance limit
        dijkstra = new Dijkstra(graph, verticesOut[0][1], options, verticesIn[0][2]);
        spt = dijkstra.getShortestPathTree(verticesIn[0][3], 20);
        v03 = spt.getVertex(verticesIn[0][3]);
        assertNull(v03);

        spt = dijkstra.getShortestPathTree(verticesIn[0][3], 130);
        v03 = spt.getVertex(verticesIn[0][3]);
        assertNotNull(v03);
        
        // test getShortcuts
        
        ContractionHierarchy testch = new ContractionHierarchy(graph, OptimizeType.QUICK, TraverseMode.WALK, 0.0);
        Vertex v = graph.getVertex("a(2, 2)");
        List<Shortcut> shortcuts = testch.getShortcuts(v, 5, true).shortcuts;
        
        assertEquals(16, shortcuts.size());
        
        v = graph.getVertex("(0, 0) in");
        shortcuts = testch.getShortcuts(v, 5, true).shortcuts;
        assertEquals(0, shortcuts.size());

        // test hierarchy construction
        ContractionHierarchy hierarchy = new ContractionHierarchy(graph, OptimizeType.QUICK, TraverseMode.WALK, 1.0);

        assertTrue(hierarchy.down.getVertices().size() == graphSize);
        assertTrue(hierarchy.up.getVertices().size() == graphSize);
        assertTrue(hierarchy.graph.getVertices().size() == 0);

        System.out.println("Contracted");

        State init = new State(1000000000);

        // test query
        GraphPath path = hierarchy.getShortestPath(verticesOut[0][0], verticesIn[N - 1][N - 1], init,
                options);
        assertNotNull(path);

        assertEquals((N - 1) * 2 + 1, path.edges.size());
        assertEquals(path.edges.size() + 1, path.vertices.size());

        SPTVertex lastVertex = path.vertices.get(0);
        for (int i = 0; i < path.edges.size(); ++i) {
            SPTEdge e = path.edges.get(i);
            assertSame(e.getFromVertex(), lastVertex);
            assertSame(lastVertex, path.vertices.get(i));
            lastVertex = e.getToVertex();
        }

        path = hierarchy.getShortestPath(verticesIn[1][1], verticesOut[2][2], init, options);
        if (path == null || path.edges.size() != 4) {
            path = hierarchy.getShortestPath(verticesIn[1][1], verticesOut[2][2], init, options);
        }
        
        options = new TraverseOptions();
        options.optimizeFor = OptimizeType.QUICK;
        options.speed = 1;
        for (int x1 = 0; x1 < N; ++x1) {
            for (int y1 = 0; y1 < N; ++y1) {
                for (int x2 = 0; x2 < N; ++x2) {
                    for (int y2 = 0; y2 < N; ++y2) {
                        if (x1 == x2 && y1 == y2) {
                            continue;
                        }
                        options.setArriveBy(false);
                        path = hierarchy.getShortestPath(verticesOut[y1][x1], verticesIn[y2][x2], init,
                                options);

                        assertNotNull(path);
                        assertEquals(Math.abs(x1 - x2) + Math.abs(y1 - y2) + 1, path.edges.size());
                        
                        options.setArriveBy(true);
                        path = hierarchy.getShortestPath(verticesOut[y1][x1], verticesIn[y2][x2], init,
                                options);

                        assertNotNull(path);
                        assertEquals(Math.abs(x1 - x2) + Math.abs(y1 - y2) + 1, path.edges.size());
                    }
                }
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUDG() {
        final int N = 50;

        Graph graph = new Graph();
        STRtree tree = new STRtree();

        Random random = new Random(1);

        ArrayList<Vertex> vertices = new ArrayList<Vertex>(N);
        for (int i = 0; i < N; ++i) {
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            Vertex v = new EndpointVertex("(" + x + ", " + y + ")", x, y);
            graph.addVertex(v);
            Envelope env = new Envelope(v.getCoordinate());
            tree.insert(env, v);
            vertices.add(v);
        }

        int i = 0;
        int expansion = 1;
        for (GraphVertex gv : graph.getVertices()) {
            Vertex v = gv.vertex;
            i++;
            final Coordinate c = v.getCoordinate();
            Envelope env = new Envelope(c);
            env.expandBy(50 * expansion);
            List<Vertex> nearby = tree.query(env);
            while (nearby.size() < 7) {
                env.expandBy(50);
                expansion += 1;
                nearby = tree.query(env);
            }
            Collections.sort(nearby, new Comparator<Vertex>() {
                public int compare(Vertex a, Vertex b) {
                    return (int) (a.distance(c) - b.distance(c));
                }
            });
            for (Vertex n : nearby.subList(1, 6)) {
                graph.addEdge(new FreeEdge(v, n));
                graph.addEdge(new FreeEdge(n, v));
            }
            Vertex badTarget = nearby.get(6);
            graph.addEdge(new ForbiddenEdge(badTarget, v));
        }

        // ensure that graph is connected
        DisjointSet<Vertex> components = new DisjointSet<Vertex>();
        Vertex last = null;
        for (Vertex v : vertices) {
            for (DirectEdge e: filter(graph.getOutgoing(v),DirectEdge.class)) {
                components.union(v, e.getToVertex());
            }
            last = v;
        }
        int lastKey = components.find(last);
        for (Vertex v : vertices) {
            int key = components.find(v);
            if (key != lastKey) {
                lastKey = components.union(v, last);
                last = v;
                Coordinate c = v.getCoordinate();
                graph.addEdge(new NonFreeEdge(v, last, last.distance(c)));
                graph.addEdge(new NonFreeEdge(last, v, last.distance(c)));
            }
        }

        ContractionHierarchy hierarchy = new ContractionHierarchy(graph, OptimizeType.QUICK, TraverseMode.WALK, 1.0);

        State init = new State(0);
        TraverseOptions options = new TraverseOptions();
        options.optimizeFor = OptimizeType.QUICK;
        options.walkReluctance = 1;
        options.speed = 1;
        GraphPath path = hierarchy.getShortestPath(vertices.get(0), vertices.get(1), init, options);
        assertNotNull(path);
        assertTrue(path.edges.size() != 0);
        
        long now = System.currentTimeMillis();
        for (Vertex start : vertices) {
            int j = (int) (Math.random() * vertices.size());
            Vertex end = vertices.get(j);
            if (start == end) {
                continue;
            }
            GraphPath path2 = hierarchy.getShortestPath(start, end, init, options);
            assertNotNull(path2);
        }
        System.out.println("time per query: " + (System.currentTimeMillis() - now) / 1000.0 / N);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNYC() throws Exception {

        long startTime = new GregorianCalendar(2010, 4, 4, 12, 0, 0).getTimeInMillis();
        State init = new State(startTime);
        GraphPath path;
        Graph graph = new Graph();
        ContractionHierarchy hierarchy;
      
        URL resource = getClass().getResource("nyc_streets/streets.shp");
        File file = null;
        if (resource != null) {
            file = new File(resource.getFile());
        } 
        if (file == null || !file.exists()) {
            System.out
                    .println("No New York City basemap; skipping; see comment in TestShapefileStreetGraphBuilderImpl for details");
            return;
        }

        ShapefileFeatureSourceFactoryImpl factory = new ShapefileFeatureSourceFactoryImpl(file);

        ShapefileStreetSchema schema = new ShapefileStreetSchema();
        schema.setIdAttribute("SegmentID");
        schema.setNameAttribute("Street");

        // only featuretyp=0 are streets
        CaseBasedBooleanConverter selector1 = new CaseBasedBooleanConverter("FeatureTyp", false);
        HashMap<String, Boolean> streets = new HashMap<String, Boolean>();
        streets.put("0", true);
        selector1.setValues(streets);

        // also, streets are sometime duplicated
        NullBooleanConverter selector2 = new NullBooleanConverter("SAFStPlace", true);

        schema.setFeatureSelector(new CompositeBooleanConverter(selector1, selector2));

        CaseBasedTraversalPermissionConverter perms = new CaseBasedTraversalPermissionConverter(
                "TrafDir", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        perms.addPermission("W", StreetTraversalPermission.ALL,
                StreetTraversalPermission.PEDESTRIAN);
        perms.addPermission("A", StreetTraversalPermission.PEDESTRIAN,
                StreetTraversalPermission.ALL);
        perms.addPermission("T", StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);

        schema.setPermissionConverter(perms);

        ShapefileStreetGraphBuilderImpl loader = new ShapefileStreetGraphBuilderImpl();
        loader.setFeatureSourceFactory(factory);
        loader.setSchema(schema);

        loader.buildGraph(graph);       
        
        // load gtfs 
        
        GtfsGraphBuilderImpl gtfsBuilder = new GtfsGraphBuilderImpl();
        GtfsBundle bundle = new GtfsBundle();
        bundle.setPath(new File("/home/novalis/Desktop/nyct_subway_100308.zip"));
        ArrayList<GtfsBundle> bundleList = new ArrayList<GtfsBundle>();
        bundleList.add(bundle); 
        GtfsBundles bundles = new GtfsBundles();
        bundles.setBundles(bundleList);
        gtfsBuilder.setGtfsBundles(bundles);
        
        gtfsBuilder.buildGraph(graph);

        NetworkLinker nl = new NetworkLinker(graph);
        nl.createLinkage();

        TraverseOptions options = new TraverseOptions();
        options.modes = new TraverseModeSet(TraverseMode.WALK, TraverseMode.SUBWAY);
        options.optimizeFor = OptimizeType.QUICK;
        
        CalendarServiceData data = graph.getService(CalendarServiceData.class);
        assertNotNull(data);
        CalendarServiceImpl calendarService = new CalendarServiceImpl();
        calendarService.setData(data);
        options.setCalendarService(calendarService);
        
        Vertex start1 = graph.getVertex("0072480");
        Vertex end1 = graph.getVertex("0032341");

        assertNotNull(end1);
        assertNotNull(start1);
        
        ShortestPathTree shortestPathTree = AStar.getShortestPathTree(graph, start1, end1, init, options);
        path = shortestPathTree.getPath(end1);
        assertNotNull(path);

        boolean subway1 = false;
        for (SPTEdge edge : path.edges) {
            if (TraverseMode.SUBWAY.equals(edge.getMode())) {
                subway1 = true;
                break;
            }
        }
        assertTrue("Path must take subway", subway1);

        ContractionHierarchySet chs = new ContractionHierarchySet();
        chs.addModeAndOptimize(new ModeAndOptimize(TraverseMode.WALK, OptimizeType.QUICK));
        chs.setContractionFactor(0.90);
        chs.setGraph(graph);
        chs.build();
        
        ContractionHierarchySerializationLibrary.writeGraph(chs, new File("/tmp/contracted"));

        chs = ContractionHierarchySerializationLibrary.readGraph(new File("/tmp/contracted"));
        hierarchy = chs.getHierarchy(options);
        assertNotNull(hierarchy);
        
        // find start and end vertices
        Vertex start = null;
        Vertex end = null;

        start = hierarchy.graph.getVertex("0072480");
        end = hierarchy.graph.getVertex("0032341");

        assertNotNull(start);
        assertNotNull(end);
        
        init = new State(0);
        path = hierarchy.getShortestPath(start, end, init, options);
        assertNotNull(path);

        init = new State(startTime);
        GraphPath pathWithSubways = hierarchy.getShortestPath(start, end, init, options);
        assertNotNull(pathWithSubways);
        boolean subway = false;
        for (SPTEdge edge : pathWithSubways.edges) {
            if (TraverseMode.SUBWAY.equals(edge.getMode())) {
                subway = true;
                break;
            }
        }
        assertTrue("Path must take subway", subway);

        
        //try reverse routing
        options.setArriveBy(true);
        pathWithSubways = hierarchy.getShortestPath(start, end, init, options);
        assertNotNull("Reverse path must be found", pathWithSubways);
        subway = false;
        for (SPTEdge edge : pathWithSubways.edges) {
            if (TraverseMode.SUBWAY.equals(edge.getMode())) {
                subway = true;
                break;
            }
        }
        assertTrue("Reverse path must take subway", subway);

        options.setArriveBy(false);
        
        // test max time 
        options.worstTime = startTime + 1000 * 60 * 90; //an hour and a half is too much time

        path = hierarchy.getShortestPath(start, end, new State(startTime),
                options);
        assertNotNull(path);
            
        options.worstTime = startTime + 1000 * 60; //but one minute is not enough

        path = hierarchy.getShortestPath(start, end, new State(startTime),
                options);
        assertNull(path);        
        
        
        long now = System.currentTimeMillis();
        int i = 0;
        int notNull = 0;
        ArrayList<GraphVertex> vertices = new ArrayList<GraphVertex>(hierarchy.up.getVertices());
               
        DisjointSet<Vertex> components = new DisjointSet<Vertex>();
        for (GraphVertex gv : vertices) {
            for (DirectEdge e: filter(gv.getOutgoing(),DirectEdge.class)) {
                components.union(gv.vertex, e.getToVertex());
            }
        }
        
        ArrayList<GraphVertex> verticesOut = new ArrayList<GraphVertex>();
        for (GraphVertex gv : vertices) {
            Vertex v = gv.vertex;
            if (components.size(components.find(v)) > vertices.size() / 2) {
                if (gv.getDegreeOut() != 0) {
                    verticesOut.add(gv);
                }
            }
        }
        
        //nyc has islands
        assertTrue(vertices.size() > verticesOut.size());
        vertices = verticesOut;
        
        Random random = new Random(0);
        
        for (GraphVertex gv1 : vertices) {
            Vertex v1 = gv1.vertex;
            if (++i == 100) {
                //only look at 100 pairs of vertices
                break; 
            }
            if (v1.getLabel().endsWith(" in")) {
                String label = v1.getLabel();
                v1 = hierarchy.up.getVertex(label.substring(0, label.length() - 3) + " out");
                if (v1 == null) {
                    --i;
                    continue;
                }
            }
            GraphVertex gv2 = null;
            Vertex v2 = null;
            while (v2 == null || gv2.getDegreeIn() == 0) {
                int j = Math.abs(random.nextInt()) % vertices.size();
                gv2 = vertices.get(j);
                v2 = gv2.vertex;
                if (v1 == v2) {
                    continue;
                }
                if (v2.getLabel().endsWith(" out")) {
                    String label = v2.getLabel();
                    gv2 = hierarchy.down.getGraphVertex(label.substring(0, label.length() - 4) + " in");
                    if (gv2 == null) {
                        v2 = null;
                        continue;
                    }
                    v2 = gv2.vertex;
                }
            }
            options.setArriveBy(i % 2 == 0); //half of all trips will be reverse trips just for fun
            GraphPath path2 = hierarchy.getShortestPath(v1, v2, init, options);
            //assertNotNull(path2);
            if (path2 != null) {
                notNull += 1;
            }
        }

        System.out.println("not null: " + notNull + " of " + i);
        System.out.println("time for 100 shortest paths (on a not-particularly-contracted graph): " + (System.currentTimeMillis() - now) / 1000.0);
        
        //occasionally, a few paths will be null because they start on bridges going out of the city
        assertTrue(notNull / (float) i > 0.95); 
    }

}
