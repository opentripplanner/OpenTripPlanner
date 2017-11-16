package org.opentripplanner.netex;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.graph_builder.model.NetexBundle;
import org.opentripplanner.graph_builder.module.NetexModule;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// TODO - The mapping needs better tests
public class MappingTest {
    private static final String NETEX_FILENAME = "src/test/resources/netex/netex_minimal.zip";

    private static OtpTransitServiceBuilder otpBuilder;

    @BeforeClass
    public static void setUpNetexMapping() throws Exception {

        NetexBundle netexBundle = new NetexBundle(new File(NETEX_FILENAME));
        NetexModule netexModule = new NetexModule(Collections.singletonList(netexBundle));
        otpBuilder = netexModule.getOtpDao().stream().findFirst().orElseThrow(IllegalStateException::new);
    }

    @Test public void testNetexRoutes() {
        ArrayList<Route> routesNetex = new ArrayList<>(otpBuilder.getRoutes().values());
        Assert.assertEquals(2, routesNetex.size());
    }

    @Test public void testNetexStopTimes() {
        Set<StopTime> stopTimesNetex = new HashSet<>(
                otpBuilder.getStopTimesSortedByTrip().valuesAsSet());
        Assert.assertEquals(0, stopTimesNetex.size());
    }

    @Test public void testNetexCalendar() {
        for (ServiceCalendarDate serviceCalendarDate : otpBuilder.getCalendarDates()) {
            String newId = convertServiceIdFormat(serviceCalendarDate.getServiceId().getId());
            serviceCalendarDate.getServiceId().setId(newId);
        }

        final Collection<ServiceCalendarDate> datesNetex = new ArrayList<>(
                otpBuilder.getCalendarDates());

        Assert.assertEquals(24, datesNetex.size());
    }

    private static String convertServiceIdFormat(String netexServiceId) {
        StringBuilder gtfsServiceId = new StringBuilder();
        boolean first = true;

        String[] splitId = netexServiceId.split("\\+");
        Arrays.sort(splitId);

        for (String singleId : splitId) {
            if (first) {
                gtfsServiceId.append(singleId);
                first = false;
            } else {
                gtfsServiceId.append("-").append(singleId.split(":")[2]);
            }
        }
        return gtfsServiceId.toString();
    }
}