package org.opentripplanner.netex;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.netex.loader.NetexLoader;
import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.standalone.config.OTPConfiguration;

import java.io.File;
import java.util.ArrayList;


public class NetexLoaderIntegrationTest {
    private static final String NETEX_DIR = "src/test/resources/netex";
    private static final String NETEX_FILENAME = "netex_minimal.zip";

    private static OtpTransitServiceBuilder otpBuilder;

    @BeforeClass public static void setUpNetexMapping() throws Exception {
        NetexBundle netexBundle = createNetexBundle();
        NetexLoader netexLoader = new NetexLoader(netexBundle);
        otpBuilder = netexLoader.loadBundle();
    }

    static NetexBundle createNetexBundle() {
        JsonNode buildConfig = new OTPConfiguration(null)
                .getGraphConfig(new File(NETEX_DIR))
                .builderConfig();
        return new NetexBundle(
                new File(NETEX_DIR, NETEX_FILENAME),
                new GraphBuilderParameters(buildConfig)
        );
    }

    @Test public void checkInput() {
        NetexBundle netexBundle = createNetexBundle();
        netexBundle.checkInputs();
    }

    @Test public void testAgencies() {
        // TODO OTP2 - This is not very robust
        Assert.assertEquals(1, otpBuilder.getAgenciesById().values().size());
    }

    @Test public void testNetexRoutes() {
        // TODO OTP2 - This is not very robust
        ArrayList<Route> routesNetex = new ArrayList<>(otpBuilder.getRoutes().values());
        Assert.assertEquals(2, routesNetex.size());
    }

    @Test public void testNetexStopTimes() {
        // TODO OTP2 - This is not very robust
        Assert.assertEquals(8, otpBuilder.getStopTimesSortedByTrip().valuesAsSet().size());
    }

    @Test public void testNetexCalendar() {
        Assert.assertEquals(24, otpBuilder.getCalendarDates().size());
    }
}