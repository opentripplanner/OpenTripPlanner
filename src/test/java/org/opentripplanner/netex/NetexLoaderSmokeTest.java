package org.opentripplanner.netex;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.configure.NetexConfig;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.standalone.config.OTPConfiguration;

import java.io.File;
import java.util.ArrayList;

/**
 * Load a small NeTEx file set without failing. This is just a smoke test
 * and should be excluded from line coverage. The focus of this test is
 * to test that the different parts of the NeTEx works together.
 */
public class NetexLoaderSmokeTest {

    private static final String NETEX_DIR = "src/test/resources/netex";
    private static final String NETEX_FILENAME = "netex_minimal.zip";

    @Test
    public void smokeTestOfNetexLoadData() throws Exception {
        // Given

        GraphBuilderParameters builderParameters = createBuilderParameters();

        NetexBundle netexBundle = createNetexBundle(builderParameters);


        // Run the check to make sure it does not throw an exception
        netexBundle.checkInputs();

        // When
        OtpTransitServiceBuilder otpBuilder = netexBundle.loadBundle();

        System.out.println(otpBuilder);


        // Then - smoke testing
        Assert.assertEquals(1, otpBuilder.getAgenciesById().values().size());
        ArrayList<Route> routesNetex = new ArrayList<>(otpBuilder.getRoutes().values());
        Assert.assertEquals(2, routesNetex.size());
        Assert.assertEquals(8, otpBuilder.getStopTimesSortedByTrip().valuesAsSet().size());
        Assert.assertEquals(24, otpBuilder.getCalendarDates().size());
    }


    /* private methods */

    private static GraphBuilderParameters createBuilderParameters() {
        JsonNode buildConfig = new OTPConfiguration(null)
                .getGraphConfig(new File(NETEX_DIR))
                .builderConfig();

        return new GraphBuilderParameters(buildConfig);
    }

    private static NetexBundle createNetexBundle(GraphBuilderParameters builderParameters) {
        return NetexConfig.netexBundleForTest(
                builderParameters,
                new File(NETEX_DIR, NETEX_FILENAME)
        );
    }
}