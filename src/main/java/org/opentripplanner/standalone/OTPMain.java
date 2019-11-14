package org.opentripplanner.standalone;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphLoader;
import org.opentripplanner.standalone.config.GraphConfig;
import org.opentripplanner.util.ThrowableUtils;
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
            // It is tempting to use JCommander's command syntax: http://jcommander.org/#_more_complex_syntaxes_commands
            // But this seems to lead to confusing switch ordering and more difficult subsequent use of the
            // parsed commands, since there will be three separate objects.
            JCommander jc = JCommander.newBuilder().addObject(params).args(args).build();
            if (params.version) {
                System.out.println(MavenVersion.VERSION.getLongVersionString());
                System.exit(0);
            }
            if (params.help) {
                System.out.println(MavenVersion.VERSION.getShortVersionString());
                jc.setProgramName("java -Xmx4G -jar otp.jar");
                jc.usage();
                System.exit(0);
            }
            params.inferAndValidate();
        } catch (ParameterException pex) {
            System.out.println(MavenVersion.VERSION.getShortVersionString());
            LOG.error("Parameter error: {}", pex.getMessage());
            System.exit(1);
        }
        if ( ! (params.build || params.load || params.serve)) {
            LOG.info("Nothing to do. Use --help to see available options.");
            System.exit(-1);
        }
        return params;
    }

    /**
     * All startup logic is in an instance method instead of the static main method so it is possible to build graphs
     * from web services or scripts, not just from the command line. If options cause an OTP API server to start up,
     * this method will return when the web server shuts down.
     *
     * @return true if the OTPServer starts successfully; false if an error occurs while loading the graph.
     */
    private static boolean startOTPServer(CommandLineParameters params) {
        OTPAppConstruction appConstruction = new OTPAppConstruction(params);
        Router router = null;

        /* Start graph builder if requested. */
        if (params.build) {
            GraphBuilder graphBuilder = appConstruction.createDefaultGraphBuilder();
            if (graphBuilder != null) {
                graphBuilder.run();
                /* If requested, hand off the graph to the server as the default graph using an in-memory GraphSource. */
                if (params.inMemory) {
                    Graph graph = graphBuilder.getGraph();
                    graph.index(new DefaultStreetVertexIndexFactory());
                    // FIXME: This router config retrieval is too complex.
                    router = new Router(graph);
                    router.startup(appConstruction.configuration().getGraphConfig(params.getGraphDirectory()).routerConfig());
                } else {
                    LOG.info("Done building graph. Exiting.");
                    return true;
                }
            } else {
                LOG.error("An error occurred while building the graph.");
                return false;
            }
        }

        /* Load graph from disk if one is not present from build. */
        if (params.load) {
            GraphConfig graphConfig = appConstruction.configuration().getGraphConfig(params.getGraphDirectory());
            router = GraphLoader.loadGraph(graphConfig);
        }

        /* Bail out if we have no router to work with. */
        if (router == null) {
            LOG.error("No router available for server or visualizer. Exiting.");
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
                    LOG.error(
                        "An uncaught error occurred inside OTP. Restarting server. Error was: {}",
                        ThrowableUtils.detailedString(throwable)
                    );
                }
            }
        }
        return true;
    }

}
