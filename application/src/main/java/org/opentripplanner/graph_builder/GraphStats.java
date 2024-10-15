package org.opentripplanner.graph_builder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.csvreader.CsvWriter;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphStats {

  private static final Logger LOG = LoggerFactory.getLogger(GraphStats.class);

  @Parameter(names = { "-v", "--verbose" }, description = "Verbose output")
  private final boolean verbose = false;

  @Parameter(names = { "-d", "--debug" }, description = "Debug mode")
  private final boolean debug = false;

  private final CommandEndpoints commandEndpoints = new CommandEndpoints();
  private final CommandPatternStats commandPatternStats = new CommandPatternStats();
  private final JCommander jc;

  @Parameter(
    names = { "-h", "--help" },
    description = "Print this help message and exit",
    help = true
  )
  private boolean help;

  @Parameter(names = { "-g", "--graph" }, description = "path to the graph file", required = true)
  private String graphPath;

  @Parameter(names = { "-o", "--out" }, description = "output file")
  private String outPath;

  private Graph graph;

  private TransitModel transitModel;

  private CsvWriter writer;

  private GraphStats(String[] args) {
    jc = new JCommander(this);
    jc.addCommand(commandEndpoints);
    jc.addCommand(commandPatternStats);

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

  public static void main(String[] args) {
    GraphStats graphStats = new GraphStats(args);
    try {
      graphStats.run();
    } catch (OtpAppException ignore) {
      // The error is handled at a lover level
    }
  }

  private void run() {
    /* open input graph (same for all commands) */
    File graphFile = new File(graphPath);
    SerializedGraphObject serializedGraphObject = SerializedGraphObject.load(graphFile);
    graph = serializedGraphObject.graph;
    transitModel = serializedGraphObject.transitModel;

    /* open output stream (same for all commands) */
    if (outPath != null) {
      try {
        writer = new CsvWriter(outPath, ',', StandardCharsets.UTF_8);
      } catch (Exception e) {
        LOG.error("Exception while opening output file " + outPath);
        return;
      }
    } else {
      writer = new CsvWriter(System.out, ',', StandardCharsets.UTF_8);
    }
    LOG.info("done loading graph.");

    String command = jc.getParsedCommand();
    if (command.equals("endpoints")) {
      commandEndpoints.run();
    } else if (command.equals("patternstats")) {
      commandPatternStats.run();
    }
    writer.close();
  }

  @Parameters(
    commandNames = "endpoints",
    commandDescription = "Generate random endpoints for performance testing"
  )
  class CommandEndpoints {

    @Parameter(names = { "-r", "--radius" }, description = "perturbation radius in meters")
    private final double radius = 100;

    @Parameter(names = { "-n", "--number" }, description = "number of endpoints to generate")
    private final int n = 20;

    @Parameter(
      names = { "-s", "--stops" },
      description = "choose endpoints near stops not street vertices"
    )
    private final boolean useStops = false;

    @Parameter(
      names = { "-rs", "--seed" },
      description = "random seed, allows reproducible results"
    )
    private final Long seed = null;

    // go along road then random
    public void run() {
      LOG.info(
        String.format(
          "Producing %d random endpoints within radius %2.2fm around %s.",
          n,
          radius,
          useStops ? "stops" : "streets"
        )
      );
      List<Vertex> vertices = new ArrayList<>();
      GeodeticCalculator gc = new GeodeticCalculator();
      Class<?> klasse = useStops ? TransitStopVertex.class : StreetVertex.class;
      for (Vertex v : graph.getVertices()) if (klasse.isInstance(v)) vertices.add(v);
      Random random = new Random();
      if (seed != null) random.setSeed(seed);
      Collections.shuffle(vertices, random);
      vertices = vertices.subList(0, n);
      try {
        writer.writeRecord(new String[] { "n", "name", "lon", "lat" });
        int i = 0;
        for (Vertex v : vertices) {
          Coordinate c;
          if (v instanceof StreetVertex) {
            LineString ls = ((StreetVertex) v).getOutgoing().iterator().next().getGeometry();
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
          String name = v.getDefaultName();
          String[] entries = new String[] {
            Integer.toString(i),
            name,
            Double.toString(dest.getX()),
            Double.toString(dest.getY()),
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

  @Parameters(commandNames = "patternstats", commandDescription = "trip pattern stats")
  class CommandPatternStats {

    public void run() {
      LOG.info("counting number of trips per pattern...");
      try {
        writer.writeRecord(
          new String[] {
            "nTripsInPattern",
            "frequency",
            "cumulativePatterns",
            "empiricalDistPatterns",
            "cumulativeTrips",
            "empiricalDistTrips",
          }
        );
        Collection<TripPattern> patterns = transitModel.getAllTripPatterns();
        Multiset<Integer> counts = TreeMultiset.create();
        int nPatterns = patterns.size();
        LOG.info("total number of patterns is: {}", nPatterns);
        int nTrips = 0;
        for (TripPattern ttp : patterns) {
          int patternNumTrips = (int) ttp.scheduledTripsAsStream().count();
          counts.add(patternNumTrips);
          nTrips += patternNumTrips;
        }
        LOG.info("total number of trips is: {}", nTrips);
        LOG.info("average number of trips per pattern is: {}", nTrips / nPatterns);
        int cPatterns = 0;
        int cTrips = 0;
        for (Multiset.Entry<Integer> count : counts.entrySet()) {
          cPatterns += count.getCount();
          cTrips += count.getCount() * count.getElement();
          writer.writeRecord(
            new String[] {
              count.getElement().toString(),
              Integer.toString(count.getCount()),
              Integer.toString(cPatterns),
              Double.toString(cPatterns / (double) nPatterns),
              Integer.toString(cTrips),
              Double.toString(cTrips / (double) nTrips),
            }
          );
        }
      } catch (IOException e) {
        LOG.error("Exception writing CSV: {}", e.getMessage());
        return;
      }
      LOG.info("done.");
    }
  }
}
