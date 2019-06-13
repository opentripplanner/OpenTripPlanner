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

// TODO OTP2 - This tests need to be changed into a compleate integration test an focus on
// TODO OTP2 - that. It is not a replacement for all the Mapping tests - they test the mapping
// TODO OTP2 - but this test should focus on that all parts of a netex file is consumed and
// TODO OTP2 - mapped - verifieying that the mapping is correct is the responsibility of he
// TODO OTP2 - mapping tests.
public class MappingTest {
    private static final String NETEX_DIR = "src/test/resources/netex";
    private static final String NETEX_FILENAME = "netex_minimal.zip";

    private static OtpTransitServiceBuilder otpBuilder;

    @BeforeClass public static void setUpNetexMapping() throws Exception {
        JsonNode buildConfig = new OTPConfiguration(null)
                .getGraphConfig(new File(NETEX_DIR))
                .builderConfig();
        NetexBundle netexBundle = new NetexBundle(
                new File(NETEX_DIR, NETEX_FILENAME),
                new GraphBuilderParameters(buildConfig)
        );
        otpBuilder = new NetexLoader(netexBundle).loadBundle();
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