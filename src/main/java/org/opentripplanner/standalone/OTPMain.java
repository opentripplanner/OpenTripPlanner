package org.opentripplanner.standalone;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphScanner;
import org.opentripplanner.routing.impl.MemoryGraphSource;
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

        if (params.build == null && !params.visualize && !params.server && params.scriptFile == null) {
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
     *         true - if the OTPServer starts successfully. If "Run an OTP API server" has been requested, this method will return when the web server shuts down;
     *         false - if an error occurs while loading the graph;
     */
    private static boolean startOTPServer(CommandLineParameters params) {
        OTPAppConstruction appConstruction = new OTPAppConstruction(params);
        // TODO do params.infer() here to ensure coherency?

        /* Start graph builder if requested */
        if (params.build != null) {
            GraphBuilder graphBuilder = appConstruction.createDefaultGraphBuilder();
            if (graphBuilder != null) {
                graphBuilder.run();
                /* If requested, hand off the graph to the server as the default graph using an in-memory GraphSource. */
                if (params.inMemory || params.preFlight) {
                    Graph graph = graphBuilder.getGraph();
                    graph.index(new DefaultStreetVertexIndexFactory());
                    // FIXME set true router IDs
                    appConstruction.graphService().registerGraph("", new MemoryGraphSource("", graph));
                }
            } else {
                LOG.error("An error occurred while building the graph.");
                return false;
            }
        }

        /* Scan for graphs to load from disk if requested */
        // FIXME eventually router IDs will be present even when just building a graph.
        if ((params.routerIds != null && params.routerIds.size() > 0) || params.autoScan) {
            /* Auto-register pre-existing graph on disk, with optional auto-scan. */
            GraphScanner graphScanner = appConstruction.createDefaultGraphScanner();

            if (params.routerIds != null && params.routerIds.size() > 0) {
                graphScanner.defaultRouterId = params.routerIds.get(0);
            }
            graphScanner.autoRegister = params.routerIds;
            graphScanner.startup();
        }

        /* Start visualizer if requested */
        if (params.visualize) {
            Router defaultRouter = appConstruction.graphService().getRouter();
            defaultRouter.graphVisualizer = new GraphVisualizer(defaultRouter);
            defaultRouter.graphVisualizer.run();
            defaultRouter.timeouts = new double[] {60}; // avoid timeouts due to search animation
        }

        /* Start web server if requested */
        if (params.server) {
            GrizzlyServer grizzlyServer = appConstruction.createGrizzlyServer();
            while (true) { // Loop to restart server on uncaught fatal exceptions.
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
