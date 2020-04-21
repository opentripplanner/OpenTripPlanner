package org.opentripplanner.standalone;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.configure.OTPAppConstruction;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.OtpAppException;
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
        try {
            OtpStartupInfo.logInfo();
            CommandLineParameters params = parseAndValidateCmdLine(args);
            startOTPServer(params);
        }
        catch (OtpAppException ae) {
            LOG.error(ae.getMessage());
            System.exit(100);
        }
        catch (Exception e) {
            LOG.error("An uncaught error occurred inside OTP: {}", e.getLocalizedMessage(), e);
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
        return params;
    }

    /**
     * All startup logic is in an instance method instead of the static main method so it is possible to build graphs
     * from web services or scripts, not just from the command line. If options cause an OTP API server to start up,
     * this method will return when the web server shuts down.
     *
     * @throws RuntimeException if an error occurs while loading the graph.
     */
    private static void startOTPServer(CommandLineParameters params) {
        LOG.info("Searching for configuration and input files in {}", params.getBaseDirectory());

        Graph graph = null;
        OTPAppConstruction app = new OTPAppConstruction(params);

        // Validate data sources, command line arguments and config before loading and
        // processing input data to fail early
        app.validateConfigAndDataSources();

        /* Load graph from disk if one is not present from build. */
        if (params.doLoadGraph() || params.doLoadStreetGraph()) {
            DataSource inputGraph = params.doLoadGraph()
                    ? app.store().getGraph()
                    : app.store().getStreetGraph();
            SerializedGraphObject obj = SerializedGraphObject.load(inputGraph);
            graph = obj.graph;
            app.config().updateConfigFromSerializedGraph(obj.buildConfig, obj.routerConfig);
        }

        /* Start graph builder if requested. */
        if (params.doBuildStreet() || params.doBuildTransit()) {
            // Abort building a graph if the file can not be saved
            SerializedGraphObject.verifyTheOutputGraphIsWritableIfDataSourceExist(
                    app.graphOutputDataSource()
            );

            GraphBuilder graphBuilder = app.createGraphBuilder(graph);
            if (graphBuilder != null) {
                graphBuilder.run();
                // Hand off the graph to the server as the default graph
                graph = graphBuilder.getGraph();
            } else {
                throw new IllegalStateException("An error occurred while building the graph.");
            }
            // Store graph and config used to build it, also store router-config for easy deployment
            // with using the embedded router config.
            new SerializedGraphObject(graph, app.config().buildConfig(), app.config().routerConfig())
                    .save(app.graphOutputDataSource());
        }

        if(graph == null) {
            LOG.error("Nothing to do, no graph loaded or build. Exiting.");
            System.exit(101);
        }

        if(!params.doServe()) {
            LOG.info("Done building graph. Exiting.");
            return;
        }

        // Index graph for travel search
        graph.index();

        Router router = new Router(graph, app.config().routerConfig());
        router.startup();

        /* Start visualizer if requested. */
        if (params.visualize) {
            router.graphVisualizer = new GraphVisualizer(router);
            router.graphVisualizer.run();
        }

        /* Start web server if requested. */
        // We could start the server first so it can report build/load progress to a load balancer.
        // This would also avoid the awkward call to set the router on the appConstruction after it's constructed.
        // However, currently the server runs in a blocking way and waits for shutdown, so has to run last.
        if (params.doServe()) {
            GrizzlyServer grizzlyServer = app.createGrizzlyServer(router);
            // Loop to restart server on uncaught fatal exceptions.
            while (true) {
                try {
                    grizzlyServer.run();
                    return;
                } catch (Throwable throwable) {
                    LOG.error(
                        "An uncaught error occurred inside OTP. Restarting server. Error was: {}",
                        ThrowableUtils.detailedString(throwable)
                    );
                }
            }
        }
    }
}
