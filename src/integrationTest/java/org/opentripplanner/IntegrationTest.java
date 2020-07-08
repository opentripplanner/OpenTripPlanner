package org.opentripplanner;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.opentripplanner.api.model.JSONObjectMapperProvider;
import org.opentripplanner.routing.impl.GraphScanner;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPApplication;
import org.opentripplanner.standalone.OTPMain;
import org.opentripplanner.standalone.OTPServer;

import javax.ws.rs.core.Application;

public abstract class IntegrationTest extends JerseyTest {

    private final static int port = 9111;

    private final static String[] args = new String[]{
            "--basePath",
            "./src/integrationTest/resources/",
            "--inMemory",
            "--router",
            "bydgoszcz",
            "--port",
            Integer.toString(port),
    };


    @Override
    public Application configure() {
        set(TestProperties.CONTAINER_PORT, port);
        System.setProperty("sharedVehiclesApi", "http://ns3114244.ip-54-38-192.eu:8888/get_standard_vehicle_positions"); // mocked database

        CommandLineParameters params = OTPMain.parseCommandLineParams(args);
        GraphService graphService = new GraphService(false, params.graphDirectory);
        OTPServer otpServer = new OTPServer(params, graphService);

        //init router
        GraphScanner graphScanner = new GraphScanner(graphService, params.graphDirectory, params.autoScan);
        graphScanner.basePath = params.graphDirectory;
        if (params.routerIds != null && params.routerIds.size() > 0) {
            graphScanner.defaultRouterId = params.routerIds.get(0);
        }
        graphScanner.autoRegister = params.routerIds;
        graphScanner.startup();

        return new OTPApplication(otpServer, false);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config
                .register(new JacksonJsonProvider())
                .register(new JSONObjectMapperProvider());
    }
}
