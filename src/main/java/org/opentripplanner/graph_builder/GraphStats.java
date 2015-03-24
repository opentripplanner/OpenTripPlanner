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

package org.opentripplanner.graph_builder;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.geotools.referencing.GeodeticCalculator;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.csvreader.CsvWriter;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.geotools.math.Statistics;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.StreetTransitLink;

public class GraphStats {

    private static final Logger LOG = LoggerFactory.getLogger(GraphStats.class);

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

    private CommandPatternStats commandPatternStats = new CommandPatternStats(); 

    private CommandTransitLinkStats commandTransitLinkStats = new CommandTransitLinkStats();

    private JCommander jc;
    
    private Graph graph;
    
    private CsvWriter writer;
    
    public static void main(String[] args) {
        GraphStats graphStats = new GraphStats(args);
        graphStats.run();
    }
    
    private GraphStats(String[] args) {
        jc = new JCommander(this);
        jc.addCommand(commandEndpoints);
        jc.addCommand(commandSpeedStats);
        jc.addCommand(commandPatternStats);
        jc.addCommand(commandTransitLinkStats);
        
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
            LOG.error("Exception while loading graph from " + graphFile);
            return;
        }

        /* open output stream (same for all commands) */
        if (outPath != null) {
            try {
                writer = new CsvWriter(outPath, ',', Charset.forName("UTF8"));
            } catch (Exception e) {
                LOG.error("Exception while opening output file " + outPath);
                return;
            }
        } else {
            writer = new CsvWriter(System.out, ',', Charset.forName("UTF8"));
        }
        LOG.info("done loading graph.");
        
        String command = jc.getParsedCommand();
        if (command.equals("endpoints")) {
            commandEndpoints.run();
        } else if (command.equals("speedstats")) {
            commandSpeedStats.run();
        } else if (command.equals("patternstats")) {
            commandPatternStats.run();
        } else if (command.equals("transitlinkstats")) {
            commandTransitLinkStats.run();
        }
        writer.close();

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
            LOG.info(String.format("Producing %d random endpoints within radius %2.2fm around %s.",
                    n, radius, useStops ? "stops" : "streets"));
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
            try {
                writer.writeRecord( new String[] {"n", "name", "lon", "lat"} );
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
                    String[] entries = new String[] {
                            Integer.toString(i), name, 
                            Double.toString(dest.getX()), Double.toString(dest.getY())
                    };
                    writer.writeRecord(entries);
                    i += 1;
                }
            } catch (IOException ioe) {
                LOG.error("Excpetion while writing CSV: {}", ioe.getMessage());
            }
            LOG.info("done."); 
        }
    }

    @Parameters(commandNames = "speedstats", commandDescription = "speed stats") 
    class CommandSpeedStats {

        public void run() {
            LOG.info("dumping hop info...");
            try {
                writer.writeRecord( new String[] {"route", "distance", "time", "speed"} );
                for (Vertex v : graph.getVertices()) {
                    for (PatternHop ph : Iterables.filter(v.getOutgoing(), PatternHop.class)) {
                        // Vertex fromv = ph.getFromVertex();
                        // Vertex tov = ph.getToVertex();
                        double distance = ph.getDistance();
                        if (distance < 3)
                            continue;
                        TripPattern ttp = ph.getPattern();
                        List<Trip> trips = ttp.getTrips();
                        int hop = ph.stopIndex;
                        String route = ttp.route.getId().toString();
                        for (int trip = 0; trip < trips.size(); trip++){
                            int time = ttp.scheduledTimetable.getTripTimes(trip).getRunningTime(hop);
                            double speed = distance / time;
                            if (Double.isInfinite(speed) || Double.isNaN(speed))
                                continue;
                            String[] entries = new String[] { 
                                    route, Double.toString(distance), Integer.toString(time), 
                                    Double.toString(speed)
                            };
                            writer.writeRecord(entries);
                        }
                    }
                }
            } catch (IOException e) {
                LOG.error("Exception writing CSV: {}", e.getMessage());
                return;
            }
            LOG.info("done.");
        }

    }

    @Parameters(commandNames = "patternstats", commandDescription = "trip pattern stats") 
    class CommandPatternStats {
        
        public void run() {
            LOG.info("counting number of trips per pattern...");
            try {
                writer.writeRecord( new String[] {
                        "nTripsInPattern", "frequency", 
                        "cumulativePatterns", "empiricalDistPatterns",
                        "cumulativeTrips", "empiricalDistTrips" } );
                Set<TripPattern> patterns = new HashSet<TripPattern>();
                for (Vertex v : graph.getVertices()) {
                    for (PatternHop ph : Iterables.filter(v.getOutgoing(), PatternHop.class)) {
                        TripPattern ttp = ph.getPattern();
                        patterns.add(ttp);
                    }
                }
                Multiset<Integer> counts = TreeMultiset.create();
                int nPatterns = patterns.size();
                LOG.info("total number of patterns is: {}", nPatterns);
                int nTrips = 0;
                for (TripPattern ttp : patterns) {
                    List<Trip> trips = ttp.getTrips();
                    counts.add(trips.size());
                    nTrips += trips.size();
                }
                LOG.info("total number of trips is: {}", nTrips);
                LOG.info("average number of trips per pattern is: {}", nTrips/nPatterns);
                int cPatterns = 0;
                int cTrips = 0;
                for (Multiset.Entry<Integer> count : counts.entrySet()) {
                    cPatterns += count.getCount();
                    cTrips += count.getCount() * count.getElement();
                    writer.writeRecord( new String[] {
                        count.getElement().toString(),
                        Integer.toString(count.getCount()),
                        Integer.toString(cPatterns),
                        Double.toString(cPatterns / (double) nPatterns),
                        Integer.toString(cTrips),
                        Double.toString(cTrips / (double) nTrips)
                    } );
                }
            } catch (IOException e) {
                LOG.error("Exception writing CSV: {}", e.getMessage());
                return;
            }
            LOG.info("done.");
        }

    }

    @Parameters(commandNames = "transitlinkstats", commandDescription = "Distances of transitLinks")
    class CommandTransitLinkStats {

        public void run() {
            //This is heuristics ratio between median length and current length
            //Which shows which links are suspiciosly long and are probably wrongly connected
            double ratio_good = 5.145294604844843/20.254192280700618;
            try {
                LOG.info("Distance of transitLinks");
                Statistics stats = new Statistics();
                TDoubleList distances = new TDoubleArrayList(1000);
                List<String> stops = new ArrayList<>(1000);
                writer.writeRecord(new String[]{"length", "transitStop", "should_check"});
                //Saves all the distances and transitStops to lists
                for (StreetTransitLink stl: Iterables.filter(graph.getEdges(), StreetTransitLink.class)) {
                    //This is to save only one link per transitStop direction
                    if (stl.getFromVertex() instanceof TransitStop) {
                        double d = SphericalDistanceLibrary.distance(stl.getFromVertex().getCoordinate(), stl.getToVertex().getCoordinate());
                        stats.add(d);
                        distances.add(d);
                        stops.add(stl.getTransitStop().toString());
                    }
                }
                //We need sort for calculating median
                distances.sort();
                double median;
                if(distances.size()%2==1) {
                    median = distances.get(distances.size()/2);
                } else {
                    median = (distances.get(distances.size()/2) + distances.get(distances.size()/2 -1))/2;
                }
                System.out.print(stats.toString());
                System.out.println("Median:             "+ Double.toString(median));
                //Here we calculate heuristics which distance is too long to be correct stret to transit link
                //This links should be checked by hand because it is very likely that they are wrongly connected
                double longest_correct_distance = median/ratio_good;
                int to_check = 0;
                for (int i = 0; i < distances.size(); i++) {
                    double current = distances.get(i);
                    String should_check = "0";
                    if (current > longest_correct_distance) {
                        should_check = "1";
                        to_check++;
                    }
                    writer.writeRecord(new String[]{Double.toString(current), stops.get(i), should_check});
                }
                for (String line: stats.toString().split("\n")) {
                    writer.writeComment(line);
                }
                writer.writeComment("Median:             " + Double.toString(median));
                writer.writeComment("To check:           " + Integer.toString(to_check));
                System.out.println("To check:            " + Integer.toString(to_check));

            } catch (IOException e) {
                LOG.error("Exception writing CSV: {}", e.getMessage());
                return;
            }
            LOG.info("done.");
        }
    }

}


