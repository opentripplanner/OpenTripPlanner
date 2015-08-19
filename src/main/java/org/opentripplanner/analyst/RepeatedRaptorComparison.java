package org.opentripplanner.analyst;

import com.vividsolutions.jts.geom.Coordinate;
import org.joda.time.LocalDate;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.opentripplanner.analyst.cluster.ResultEnvelope;
import org.opentripplanner.analyst.cluster.TaskStatistics;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.RaptorWorkerData;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.streets.Histogram;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Compare results from Repeated RAPTOR runs. This is used to detect algorithmic changes that have
 * had an effect on output.
 *
 * Usage:
 *  graph_directory [compare_file] [output_file]
 *
 * Graph directory is the directory from which to build the graph. Compare file is the previously saved
 * file; if omitted it will not be used. Output file is the MapDB file in which
 * to save output. If left blank it defaults to the OTP commit hash.
 *
 */
public class RepeatedRaptorComparison {

    // amounts to offset origins from vertices to test linking
    public static final double OFFSET_X = 1e-4, OFFSET_Y = 1e-4;

    public static void main (String... args) {
        if (args.length == 0) {
            System.err.println("too few arguments.");
            return;
        }

        // build a graph
        File graphDir = new File(args[0]);
        Graph graph = buildGraph(graphDir);

        DB comparisonDb = null;
        BTreeMap<String, ResultEnvelope> comparison = null;

        // open the comparison file, if we have one.
        if (args.length > 1) {
            comparisonDb = DBMaker.newFileDB(new File(args[1]))
                    .readOnly()
                    .transactionDisable()
                    .closeOnJvmShutdown()
                    .cacheSize(48)
                    .asyncWriteEnable()
                    .make();

            comparison = comparisonDb.getTreeMap("results");
        }

        String outputName = args.length > 2 ? args[2] : MavenVersion.VERSION.commit + ".db";

        DB outputDb = DBMaker.newFileDB(new File(outputName))
                .transactionDisable()
                .cacheSize(48)
                .closeOnJvmShutdown()
                .make();

        final BTreeMap<String, ResultEnvelope> output = outputDb.createTreeMap("results")
                .valueSerializer(Serializer.JAVA)
                .makeStringMap();

        // if we have a comparison file, get the vertices from it. Otherwise choose some randomly.
        Collection<String> vertexLabels;
        PointSet pset;

        if (comparison != null) {
            vertexLabels = comparison.keySet();
            // clooge, pointset is stored in its own map in db.
            pset = comparisonDb.<String, PointSet>getTreeMap("pointset").get("pointset");
        }
        else {
            // choose some vertices
            List<String> labelList = graph.getVertices().stream()
                    .map(Vertex::getLabel)
                    // only use OSM nodes, because they are stable. e.g. splits may change name.
                    .filter(s -> s.startsWith("osm:node:"))
                    .collect(Collectors.toList());

            if (labelList.size() > 10000) {
                Collections.shuffle(labelList);
                vertexLabels = labelList.stream().limit(10000).collect(Collectors.toList());
            }
            else {
                vertexLabels = labelList;
            }

            // make a pointset
            pset = PointSet.regularGrid(graph.getExtent(), 100);

            outputDb.createTreeMap("pointset")
                    .<String, PointSet>make().put("pointset", pset);
        }

        SampleSet ss = new SampleSet(pset, graph.getSampleFactory());

        final BTreeMap<String, ResultEnvelope> comparisonResults = comparison;

        org.opentripplanner.streets.Histogram bestCaseHisto = new Histogram("Best case");
        org.opentripplanner.streets.Histogram avgCaseHisto = new Histogram("Average");
        org.opentripplanner.streets.Histogram worstCaseHisto = new Histogram("Worst case");

        ProfileRequest template = new ProfileRequest();
        template.accessModes = new QualifiedModeSet("WALK");
        template.analyst = true;
        // This means results will vary slightly, which is why we plot a histogram when we're done.
        //req.boardingAssumption = RaptorWorkerTimetable.BoardingAssumption.RANDOM;
        template.maxWalkTime = 20 * 60;
        template.fromTime = 7 * 3600;
        template.toTime = 9 * 3600;

        template.date = new LocalDate(2015, 8, 4);

        RaptorWorkerData data = RepeatedRaptorProfileRouter.getRaptorWorkerData(template, graph, ss, new TaskStatistics());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2,
                10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000), new ThreadPoolExecutor.CallerRunsPolicy());

        // do the computation and comparison
        vertexLabels.stream()
                .forEach(label -> {
                            executor.execute(() -> {
                                OsmVertex v = (OsmVertex) graph.getVertex(label);

                                // offset a deterministic amount to test linking
                                Coordinate coord = new Coordinate(v.getX() + OFFSET_X, v.getY() + OFFSET_Y);

                                ProfileRequest req;
                                try {
                                    req = template.clone();
                                } catch (CloneNotSupportedException e) {
                                    /* can't happen */
                                    throw new RuntimeException(e);
                                }

                                req.maxWalkTime = 20 * 60;
                                req.fromLat = req.toLat = coord.y;
                                req.fromLon = req.toLon = coord.x;

                                RepeatedRaptorProfileRouter rrpr = new RepeatedRaptorProfileRouter(
                                        graph, req, ss);
                                rrpr.raptorWorkerData = data;
                                rrpr.route();

                                ResultSet.RangeSet results = rrpr.makeResults(true, true, false);
                                ResultEnvelope env = new ResultEnvelope();
                                env.bestCase = results.min;
                                env.worstCase = results.max;
                                env.avgCase = results.avg;
                                env.id = label;
                                env.profile = true;

                                if (comparisonResults != null) {
                                    ResultEnvelope compare = comparisonResults.get(label);
                                    compare(env.bestCase, compare.bestCase, bestCaseHisto);
                                    compare(env.worstCase, compare.worstCase, worstCaseHisto);
                                    compare(env.avgCase, compare.avgCase, avgCaseHisto);
                                }

                                output.put(env.id, env);
                            });
                        });

        // wait for execution to complete
        try {
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            System.err.println("interrupted");
        }

        output.close();
        if (comparisonDb != null) {
            comparisonDb.close();

            bestCaseHisto.displayHorizontal();
            System.out.println("mean: " + bestCaseHisto.mean());

            avgCaseHisto.displayHorizontal();
            System.out.println("mean: " + avgCaseHisto.mean());

            worstCaseHisto.displayHorizontal();
            System.out.println("mean: " + worstCaseHisto.mean());
        }
    }

    private static Graph buildGraph(File directory) {
        CommandLineParameters params = new CommandLineParameters();
        params.build = directory;
        params.inMemory = true;
        GraphBuilder graphBuilder = GraphBuilder.forDirectory(params, params.build);
        graphBuilder.run();
        Graph graph = graphBuilder.getGraph();
        graph.routerId = "GRAPH";
        graph.index(new DefaultStreetVertexIndexFactory());
        graph.index.clusterStopsAsNeeded();
        return graph;
    }

    private static void compare(ResultSet current, ResultSet compare, Histogram histogram) {
        // histograms aren't threadsafe
        synchronized (histogram) {
            for (int i = 0; i < current.times.length; i++) {
                histogram.add((int) Math.round((current.times[i] - compare.times[i]) / 60d));
            }
        }
    }
}
