package org.opentripplanner.analyst;

import com.vividsolutions.jts.geom.Coordinate;
import org.joda.time.LocalDate;
import org.mapdb.*;
import org.opentripplanner.analyst.cluster.ResultEnvelope;
import org.opentripplanner.analyst.cluster.TaskStatistics;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.RaptorWorker;
import org.opentripplanner.profile.RaptorWorkerData;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.common.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private static final Logger LOG = LoggerFactory.getLogger(RepeatedRaptorComparison.class);

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
        BTreeMap<Fun.Tuple3<String, String, ResultEnvelope.Which>, Integer> comparison = null;

        // open the comparison file, if we have one.
        if (args.length > 1) {
            comparisonDb = DBMaker.newFileDB(new File(args[1]))
                    .readOnly()
                    .transactionDisable()
                    .closeOnJvmShutdown()
                    .cacheSize(24)
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

        final BTreeMap<Fun.Tuple3<String, String, ResultEnvelope.Which>, Integer> output = outputDb.createTreeMap("results")
                .valueSerializer(Serializer.JAVA)
                .makeOrGet();

        // if we have a comparison file, get the pointset from it. Otherwise choose some randomly.
        Collection<String> vertexLabels;
        PointSet pset;

        if (comparison != null) {
            // clooge, pointset is stored in its own map in db.
            pset = comparisonDb.<String, PointSet>getTreeMap("pointset").get("pointset");
        }
        else {
            // choose some vertices
            List<Vertex> vertices = graph.getVertices().stream()
                    // only use OSM nodes, because they are stable. e.g. splittervertices may not have stable identifiers between builds.
                    .filter(v -> v.getLabel().startsWith("osm:node:"))
                    .limit(1000)
                    .collect(Collectors.toList());

            // make a pointset
            pset = new PointSet(vertices.size());
            int featIdx = 0;
            for (Vertex v : vertices) {
                PointFeature pf = new PointFeature();
                pf.setId(v.getLabel());
                pf.setLat(v.getLat() + OFFSET_Y);
                pf.setLon(v.getLon() + OFFSET_X);
                pset.addFeature(pf, featIdx++);
            }

            outputDb.createTreeMap("pointset")
                    .<String, PointSet>make().put("pointset", pset);
        }

        SampleSet ss = new SampleSet(pset, graph.getSampleFactory());

        final  BTreeMap<Fun.Tuple3<String, String, ResultEnvelope.Which>, Integer> comparisonResults = comparison;

        Histogram bestCaseHisto = new Histogram("Best case");
        Histogram avgCaseHisto = new Histogram("Average");
        Histogram worstCaseHisto = new Histogram("Worst case");

        ProfileRequest template = new ProfileRequest();
        template.accessModes = new QualifiedModeSet("WALK");
        template.analyst = true;
        template.maxWalkTime = 20 * 60;
        template.walkSpeed = 1.3f;
        template.fromTime = 7 * 3600;
        template.toTime = 9 * 3600;

        template.date = new LocalDate(2015, 8, 4);

        RaptorWorkerData data = RepeatedRaptorProfileRouter.getRaptorWorkerData(template, graph, ss, new TaskStatistics());

        // do the computation and comparison
        IntStream.range(0, pset.featureCount()).parallel()
            .forEach(idx -> {

                if (idx % 100 == 0)
                    System.out.println(idx + " points complete");

                Coordinate coord = pset.getCoordinate(idx);
                String origin = pset.getFeature(idx).getId();

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
                // 7 to 9 AM
                req.fromTime = 7 * 3600;
                req.toTime = 9 * 3600;
                req.transitModes = new TraverseModeSet("TRANSIT");

                RepeatedRaptorProfileRouter rrpr = new RepeatedRaptorProfileRouter(graph, req, ss);
                rrpr.raptorWorkerData = data;
                rrpr.includeTimes = true;
                // TODO we really want to disable both isochrone and accessibility generation here.
                // Because a sampleSet is provided it's going to make accessibility information (not isochrones).

                ResultEnvelope results = new ResultEnvelope();
                try {
                    results = rrpr.route();
                } catch (Exception e) {
                    LOG.error("Exception during routing", e);
                    return;
                }

                for (ResultEnvelope.Which which : new ResultEnvelope.Which[] {
                        ResultEnvelope.Which.BEST_CASE, ResultEnvelope.Which.AVERAGE,
                        ResultEnvelope.Which.WORST_CASE }) {
                    Histogram histogram;
                    ResultSet resultSet;

                    switch (which) {
                    case BEST_CASE:
                        histogram = bestCaseHisto;
                        resultSet = results.bestCase;
                        break;
                    case WORST_CASE:
                        histogram = worstCaseHisto;
                        resultSet = results.worstCase;
                        break;
                    case AVERAGE:
                        histogram = avgCaseHisto;
                        resultSet = results.avgCase;
                        break;
                    default:
                        histogram = null;
                        resultSet = null;
                    }

                    // now that we have the proper histogram and result set, save them and do the
                    // comparison.
                    for (int i = 0; i < resultSet.times.length; i++) {
                        int time = resultSet.times[i];
                        // TODO this is creating a PointFeature obj to hold the id at each call
                        // Cache?
                        String dest = pset.getFeature(i).getId();

                        Fun.Tuple3<String, String, ResultEnvelope.Which> key = new Fun.Tuple3<>(
                                origin, dest, which);
                        output.put(key, time);

                        if (time < 0) {
                            LOG.error("Path from {}  to {} has negative time {}", origin, dest,
                                    time);
                        }

                        if (comparisonResults != null) {
                            int time0 = comparisonResults.get(key);

                            int deltaMinutes;

                            if (time0 == RaptorWorker.UNREACHED && time != RaptorWorker.UNREACHED)
                                deltaMinutes = (time / 60) - 120;
                            else if (time == RaptorWorker.UNREACHED
                                    && time0 != RaptorWorker.UNREACHED)
                                deltaMinutes = 120 - (time0 / 60);
                            else
                                deltaMinutes = (time - time0) / 60;

                            // histograms are not threadsafe
                            synchronized (histogram) {
                                histogram.add(deltaMinutes);
                            }
                        }
                    }
                }
            });

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
}
