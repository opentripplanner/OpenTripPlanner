package org.opentripplanner.graph_builder;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.geotools.referencing.GeodeticCalculator;
import org.onebusaway.gtfs.model.Trip;
import org.opengis.geometry.DirectPosition;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

public class GraphStats {

    @Parameter(names = { "-v", "--verbose" }, description = "Verbose output")
    private boolean verbose = false;
   
    @Parameter(names = { "-d", "--debug"}, description = "Debug mode")
    private boolean debug = false;

    @Parameter(names = { "-h", "--help"}, description = "Print this help message and exit", help = true)
    private boolean help;

    @Parameter(names = { "-g", "--graph"}, description = "path to the graph file", required = true)
    private String graphPath;

    @Parameter(names = { "-o", "--out"}, description = "output file")
    private String outPath;

    private CommandEndpoints commandEndpoints = new CommandEndpoints(); 
    
    private CommandSpeedStats commandSpeedStats = new CommandSpeedStats();  

    private JCommander jc;
    
    private Graph graph;
    
    private PrintStream out;
    
    public static void main(String[] args) {
        GraphStats graphStats = new GraphStats(args);
        graphStats.run();
    }
    
    private GraphStats(String[] args) {
        jc = new JCommander(this);
        jc.addCommand(commandEndpoints);
        jc.addCommand(commandSpeedStats);
        
        try {
            jc.parse(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }
        
        if (help || jc.getParsedCommand() == null) {
            jc.usage();
            System.exit(0);
        }
    }
   
    private void run() {

        /* open input graph (same for all commands) */
        File graphFile = new File(graphPath);
        try {
            graph = Graph.load(graphFile, Graph.LoadLevel.FULL);
        } catch (Exception e) {
            System.out.println("Exception while loading graph from " + graphFile);
            return;
        }

        /* open output stream (same for all commands) */
        if (outPath != null) {
            File outFile = new File(outPath);
            try {
                out = new PrintStream(outFile);
            } catch (Exception e) {
                System.out.println("Exception while opening output file " + outFile);
                return;
            }
        } else {
            out = System.out;
        }
        System.out.println("done loading graph.");
        
        String command = jc.getParsedCommand();
        if (command.equals("endpoints")) {
            commandEndpoints.run();
        } else if (command.equals("speedstats")) {
            commandSpeedStats.run();
        }
        out.close();

    }

    @Parameters(commandNames = "endpoints", commandDescription = "Generate random endpoints for performance testing") 
    class CommandEndpoints {

        @Parameter(names = { "-r", "--radius"}, description = "perturbation radius in meters")
        private double radius = 100;

        @Parameter(names = { "-n", "--number"}, description = "number of endpoints to generate")
        private int n = 20;

        @Parameter(names = { "-s", "--stops"}, description = "choose endpoints near stops not street vertices")
        private boolean useStops = false;

        @Parameter(names = { "-rs", "--seed"}, description = "random seed, allows reproducible results")
        private Long seed = null;

        // go along road then random
        public void run() {
            System.out.printf("Producing %d random endpoints within radius %2.2fm around %s.\n", 
                    n, radius, useStops ? "stops" : "streets");
            List<Vertex> vertices = new ArrayList<Vertex>();
            GeodeticCalculator gc = new GeodeticCalculator();
            Class<?> klasse = useStops ? TransitStop.class : StreetVertex.class;
            for (Vertex v : graph.getVertices())
                if (klasse.isInstance(v))
                    vertices.add(v);
            Random random = new Random();
            if (seed != null)
                random.setSeed(seed);
            Collections.shuffle(vertices, random);
            vertices = vertices.subList(0, n);
            out.printf("n,name,lat,lon\n");
            int i = 0;
            for (Vertex v : vertices) {
                Coordinate c;
                if (v instanceof StreetVertex) {
                    LineString ls = ((StreetVertex)v).getOutgoing().iterator().next().getGeometry();
                    int numPoints = ls.getNumPoints();
                    LocationIndexedLine lil = new LocationIndexedLine(ls);
                    int seg = random.nextInt(numPoints);
                    double frac = random.nextDouble();
                    LinearLocation ll = new LinearLocation(seg, frac);
                    c = lil.extractPoint(ll);
                } else {
                    c = v.getCoordinate();
                }
                // perturb
                double distance = random.nextDouble() * radius;
                double azimuth = random.nextDouble() * 360 - 180;
                // double x = c.x + r * Math.cos(theta);
                // double y = c.y + r * Math.sin(theta);
                gc.setStartingGeographicPoint(c.x, c.y);
                gc.setDirection(azimuth, distance);
                Point2D dest = gc.getDestinationGeographicPoint();
                String name = v.getName();
                out.printf("%d,%s,%f,%f\n", i, name, dest.getY(), dest.getX());
                i += 1;
            }
            System.out.printf("done.\n"); 
        }
    }

    @Parameters(commandNames = "speedstats", commandDescription = "speed stats") 
    class CommandSpeedStats {

        public void run() {
            System.out.println("dumping hop info...");
            out.println("route,distance,time,speed");
            for (Vertex v : graph.getVertices()) {
                for (PatternHop ph : IterableLibrary.filter(v.getOutgoing(), PatternHop.class)) {
                    // Vertex fromv = ph.getFromVertex();
                    // Vertex tov = ph.getToVertex();
                    double distance = ph.getDistance();
                    if (distance < 3)
                        continue;
                    TableTripPattern ttp = ph.getPattern();
                    List<Trip> trips = ttp.getTrips();
                    int hop = ph.stopIndex;
                    String route = ttp.getExemplar().getRoute().getId().toString();
                    for (int trip = 0; trip < trips.size(); trip++){
                        int time = ttp.getRunningTime(hop, trip);
                        double speed = distance / time;
                        if (Double.isInfinite(speed) || Double.isNaN(speed))
                            continue;
                        out.printf("%s,%f,%d,%f\n", route, distance, time, speed);
                    }
                }
            }
            System.out.println("done...");
        }

    }

}


