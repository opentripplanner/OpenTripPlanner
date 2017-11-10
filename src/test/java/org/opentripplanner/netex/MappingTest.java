package org.opentripplanner.netex;

import org.junit.Assert;
import org.junit.Before;
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
    private OtpTransitServiceBuilder otpBuilderFromNetex;

    private File netexFile = new File("src/test/resources/netex/netex_minimal.zip");

    @Before public void setUpNetexMapping() throws Exception {
        NetexBundle netexBundle = new NetexBundle(netexFile);
        NetexModule netexModule = new NetexModule(Collections.singletonList(netexBundle));
        this.otpBuilderFromNetex = netexModule.getOtpDao().stream().findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    @Test public void testNetexRoutes() {
        ArrayList<Route> routesNetex = new ArrayList<>(otpBuilderFromNetex.getRoutes().values());
        Assert.assertEquals(2, routesNetex.size());
    }

    @Test public void testNetexStopTimes() {
        Set<StopTime> stopTimesNetex = new HashSet<>(
                otpBuilderFromNetex.getStopTimesSortedByTrip().valuesAsSet());
        Assert.assertEquals(0, stopTimesNetex.size());
    }

    @Test public void testNetexCalendar() {
        for (ServiceCalendarDate serviceCalendarDate : otpBuilderFromNetex.getCalendarDates()) {
            String newId = convertServiceIdFormat(serviceCalendarDate.getServiceId().getId());
            serviceCalendarDate.getServiceId().setId(newId);
        }

        final Collection<ServiceCalendarDate> datesNetex = new ArrayList<>(
                otpBuilderFromNetex.getCalendarDates());

        Assert.assertEquals(22, datesNetex.size());
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