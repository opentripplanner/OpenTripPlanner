package org.opentripplanner.standalone;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphLoader;
import org.opentripplanner.standalone.config.GraphConfig;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main entry point to OpenTripPlanner. It allows both building graphs and starting up
 * an OTP server depending on command line options. OTPMain is a concrete class making it possible
 * to construct one with custom CommandLineParameters and use its graph builder construction method
 * from web services or scripts, not just from the
 * static main function below.
 */
public class OTPMain {

    private static final Logger LOG = LoggerFactory.getLogger(OTPMain.class);


    /**
     * ENTRY POINT: This is the main method that is called when running otp.jar from the command line.
     */
    public static void main(String[] args) {
        CommandLineParameters params = parseAndValidateCmdLine(args);

        if (!startOTPServer(params)) {
            System.exit(-1);
        }
    }

    /**
     * Parse and validate command line parameters. If the arguments is invalid the
     * method uses {@code System.exit()} to exit the application.
     */
    private static CommandLineParameters parseAndValidateCmdLine(String[] args) {
        CommandLineParameters params = new CommandLineParameters();
        try {
            JCommander jc = new JCommander(params, args);
            if (params.version) {
                System.out.println(MavenVersion.VERSION.getLongVersionString());
                System.exit(0);
            }
            if (params.help) {
                System.out.println(MavenVersion.VERSION.getShortVersionString());
                jc.setProgramName("java -Xmx[several]G -jar otp.jar");
                jc.usage();
                System.exit(0);
            }
            params.infer();
        } catch (ParameterException pex) {
            System.out.println(MavenVersion.VERSION.getShortVersionString());
            LOG.error("Parameter error: {}", pex.getMessage());
            System.exit(1);
        }
        if (params.build == null && !params.visualize && !params.serve && params.scriptFile == null) {
            LOG.info("Nothing to do. Use --help to see available tasks.");
            System.exit(-1);
        }
        return params;
    }

    /**
     * Making OTPMain a concrete class and placing this logic an instance method instead of embedding it in the static
     * main method makes it possible to build graphs from web services or scripts, not just from the command line.
     *
     * @return
     *         true - if the OTPServer starts successfully. If "Run an OTP API server" has been requested, this method
     *                will return when the web server shuts down;
     *         false - if an error occurs while loading the graph;
     */
    private static boolean startOTPServer(CommandLineParameters params) {
        OTPAppConstruction appConstruction = new OTPAppConstruction(params);
        Router router = null;

        /* Start graph builder if requested. */
        if (params.build != null) {
            GraphBuilder graphBuilder = appConstruction.createDefaultGraphBuilder();
            if (graphBuilder != null) {
                graphBuilder.run();
                /* If requested, hand off the graph to the server as the default graph using an in-memory GraphSource. */
                if (params.inMemory || params.preFlight) {
                    Graph graph = graphBuilder.getGraph();
                    graph.index(new DefaultStreetVertexIndexFactory());
                    // In-memory graph handoff. FIXME: This router config retrieval is too complex.
                    router = new Router(graph);
                    router.startup(appConstruction.configuration().getGraphConfig(params.build).routerConfig());
                }
            } else {
                LOG.error("An error occurred while building the graph.");
                return false;
            }
        }

        /* Load graph from disk if one is not present from build. */
        if (params.load != null) {
            GraphConfig graphConfig = appConstruction.configuration().getGraphConfig(params.load);
            router = GraphLoader.loadGraph(graphConfig);
        }

        /* Bail out if we have no graph (router) to work with. */
        if (router == null) {
            LOG.error("Did not build or load a graph. Exiting.");
            return false;
        }

        /* Start visualizer if requested. */
        if (params.visualize) {
            router.graphVisualizer = new GraphVisualizer(router);
            router.graphVisualizer.run();
            router.timeouts = new double[] {60}; // avoid timeouts due to search animation
        }

        /* Start web server if requested. */
        // We could start the server first so it can report build/load progress to a load balancer.
        // This would also avoid the awkward call to set the router on the appConstruction after it's constructed.
        // However, currently the server runs in a blocking way and waits for shutdown, so has to run last.
        if (params.serve) {
            appConstruction.setRouter(router);
            GrizzlyServer grizzlyServer = appConstruction.createGrizzlyServer();
            // Loop to restart server on uncaught fatal exceptions.
            while (true) {
                try {
                    grizzlyServer.run();
                    return true;
                } catch (Throwable throwable) {
                    LOG.error("An uncaught {} occurred inside OTP. Restarting server.",
                            throwable.getClass().getSimpleName(), throwable);
                }
            }
        }
        return true;
    }
}
