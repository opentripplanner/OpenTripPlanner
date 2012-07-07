package org.opentripplanner.graph_builder;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

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

        public void run() {
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


