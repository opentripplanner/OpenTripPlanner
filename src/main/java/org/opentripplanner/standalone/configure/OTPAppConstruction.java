package org.opentripplanner.standalone.configure;

import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.OTPConfiguration;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.OTPApplication;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.OTPFeature;

import javax.ws.rs.core.Application;

/**
 * This class is responsible for creating the top level services like {@link OTPConfiguration}
 * and {@link OTPServer}. The purpose of this class is to wire the
 * application, creating the necessary Services and modules and putting them together.
 * It is NOT responsible for starting or running the application. The whole idea of this
 * class is to separate application construction from running it.
 *
 * <p> The top level construction class(this class) may delegate to other construction classes
 * to inject configuration and services into sub-modules.
 *
 * <p> THIS CLASS IS NOT THREAD SAFE - THE APPLICATION SHOULD BE CREATED IN ONE THREAD. This
 * should be really fast, since the only IO operations are reading config files and logging.
 * Loading transit or map data should NOT happen during this phase.
 */
public class OTPAppConstruction {

    private final OTPConfiguration configuration;
    private final CommandLineParameters commandLineParameters;

    private Router router = null;
    private OTPServer server = null;

    /**
     * Create a new OTP configuration instance for a given directory.
     */
    public OTPAppConstruction(CommandLineParameters commandLineParameters) {
        this.commandLineParameters = commandLineParameters;
        this.configuration = new OTPConfiguration(commandLineParameters.getGraphDirectory());

        // Initialize features from configuration file.
        OTPFeature.configure(configuration().otpConfig());
    }

    /**
     * Create a new Grizzly server - call this method once, the new instance is created
     * every time this method is called.
     */
    public GrizzlyServer createGrizzlyServer() {
        return new GrizzlyServer(commandLineParameters, createApplication());
    }

    /**
     * Create the default graph builder.
     */
    public GraphBuilder createDefaultGraphBuilder() {
        return GraphBuilder.create(
                commandLineParameters,
                configuration().getGraphConfig(commandLineParameters.getGraphDirectory())
        );
    }

    /**
     * Return OTP application file configuration.
     * <p>
     * The method is {@code public} to allow test access.
     */
    public OTPConfiguration configuration() {
        return configuration;
    }

    /**
     * Create the top-level objects that represent the OTP server. There is one server and it
     * is created lazy at the first invocation of this method.
     * <p>
     * The method is {@code public} to allow test access.
     */
    public OTPServer server() {
        if (server == null) {
            server = new OTPServer(commandLineParameters, router);
        }
        return server;
    }

    private Application createApplication() {
        return new OTPApplication(server(), !commandLineParameters.insecure);
    }

    public void setRouter (Router router) {
        this.router = router;
    }

}
