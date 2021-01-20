package org.opentripplanner.standalone.configure;

import com.google.gson.Gson;
import fi.metatavu.airquality.configuration_parsing.GenericFileConfiguration;
import fi.metatavu.airquality.configuration_parsing.RequestParameters;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.configure.DataStoreFactory;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.OTPApplication;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.Application;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.stream.Collectors;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

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

    private static final Logger LOG = LoggerFactory.getLogger(OTPAppConstruction.class);

    private final OTPConfiguration config;

    private OtpDataStore store = null;
    private OTPServer server = null;
    private GraphBuilderDataSources graphBuilderDataSources = null;
    private GenericFileConfiguration[] configurations;


    /**
     * Create a new OTP configuration instance for a given directory.
     */
    public OTPAppConstruction(CommandLineParameters commandLineParameters) {
        this.config = new OTPConfiguration(commandLineParameters);
        initializeOtpFeatures();
    }

    /**
     * Create or retrieve a data store witch provide access to files, remote or local.
     */
    public OtpDataStore store() {
        if(store == null) {
            this.store = new DataStoreFactory(config.createDataStoreConfig()).open();
        }
        return store;
    }

    /**
     * Create a new Grizzly server - call this method once, the new instance is created
     * every time this method is called.
     */
    public GrizzlyServer createGrizzlyServer(Router router) {
        return new GrizzlyServer(config.getCli(), createApplication(router));
    }

    public void validateConfigAndDataSources() {
        // Load Graph Builder Data Sources to validate it.
        graphBuilderDataSources();
    }

    /**
     * Create the default graph builder.
     * @param baseGraph the base graph to add more data on to of.
     */
    public GraphBuilder createGraphBuilder(Graph baseGraph) {
        LOG.info("Wiring up and configuring graph builder task.");
        return GraphBuilder.create(
                config.buildConfig(),
                graphBuilderDataSources(),
                baseGraph
        );
    }

    /**
     * The output data source to use for saving the serialized graph.
     * <p>
     * This method will return {@code null} if the graph should NOT be saved. The
     * business logic to make that decision is in the {@link GraphBuilderDataSources}.
     */
    @Nullable
    public DataSource graphOutputDataSource() {
        return graphBuilderDataSources().getOutputGraph();
    }

    /**
     * Return router configuration as loaded from the 'router-config.json' file.
     * <p>
     * The method is {@code public} to allow test access.
     */
    public OTPConfiguration config() {
        return config;
    }

    public GenericFileConfiguration [] genericFileConfigurations(){
        if (this.configurations != null){
            return this.configurations;
        }
        DataSource dataSettings = store().getDataSettings();
        if (dataSettings.exists()) {
            GenericFileConfiguration[] configurations = new Gson().fromJson(new InputStreamReader(dataSettings.asInputStream()),
                    GenericFileConfiguration[].class);
            //validate the configurations
            for (GenericFileConfiguration configuration : configurations){
                RequestParameters[] requestParametersList = configuration.getRequestParameters();
                Arrays.stream(requestParametersList)
                        .collect(Collectors.groupingBy(RequestParameters::getVariable))
                        .forEach((k, v) -> {
                            if (v.size() != 2)
                                throw new IllegalArgumentException("The settings file has incorrect request parameters");
                        });


            }
            return configurations;
        }
        else return null;
    }
    /**
     * Create the top-level objects that represent the OTP server. There is one server and it
     * is created lazy at the first invocation of this method.
     * <p>
     * The method is {@code public} to allow test access.
     */
    public OTPServer server(Router router) {
        if (server == null) {
            server = new OTPServer(config.getCli(), router);
        }
        return server;
    }

    public void setOtpConfigVersionsOnServerInfo() {
        projectInfo().otpConfigVersion = config.otpConfig().configVersion;
        projectInfo().buildConfigVersion = config.buildConfig().configVersion;
        projectInfo().routerConfigVersion = config.routerConfig().getConfigVersion();
    }

    private GraphBuilderDataSources graphBuilderDataSources() {
        if(graphBuilderDataSources == null) {
            graphBuilderDataSources = GraphBuilderDataSources.create(
                    config.getCli(),
                    config.buildConfig(),
                    store()
            );
        }
        return graphBuilderDataSources;
    }

    private void initializeOtpFeatures() {
        OTPFeature.enableFeatures(config.otpConfig().otpFeatures);
        OTPFeature.logFeatureSetup();
    }

    private Application createApplication(Router router) {
        return new OTPApplication(server(router));
    }
}
