package org.opentripplanner;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.opentripplanner.api.model.JSONObjectMapperProvider;
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

    private static CommandLineParameters params;
    private static OTPServer otpServer;
    private static GraphService graphService;

    @ClassRule
    public static WireMockClassRule wireMockClassRule = new WireMockClassRule(new WireMockConfiguration().port(8888).usingFilesUnderDirectory("src/integrationTest/resources/"));

    @Rule
    public WireMockClassRule wireMockInstance = wireMockClassRule;


    @Override
    public Application configure() {
        return new OTPApplication(otpServer, false);
    }


    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty(TestProperties.CONTAINER_PORT, Integer.toString(port));
        System.setProperty("sharedVehiclesApi", "http://localhost:8888/query_db"); // mocked database

        params = OTPMain.parseCommandLineParams(args);
        graphService = new GraphService(false, params.graphDirectory);
        otpServer = new OTPServer(params, graphService);
        System.out.printf("How many times it runs?\n");

        OTPMain.registerRouters(params, graphService);

        try {
            Thread.sleep(3000); // wait for initialisation
            System.out.printf("Hopefully it finished initialising vehicles\n");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void configureClient(ClientConfig config) {
        config
                .register(new JacksonJsonProvider())
                .register(new JSONObjectMapperProvider());
    }
}
