/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.Graph;

public class AlertPatchServiceImplTest {
    private class TestAlertPatch extends AlertPatch {
        private static final long serialVersionUID = 1L;

        @Override
        public void apply(Graph graph) {
            // NO-OP
        }

        @Override
        public void remove(Graph graph) {
            // NO-OP
        }
    }

    private TestAlertPatch[] alerts;
    private AgencyAndId testStop = new AgencyAndId("A", "A");
    private AgencyAndId testRoute = new AgencyAndId("B", "B");

    @Before
    public void setup() {
        alerts = new TestAlertPatch[] {new TestAlertPatch(), new TestAlertPatch(),
                new TestAlertPatch(), new TestAlertPatch()};
        alerts[0].setRoute(testRoute);
        alerts[0].setStop(testStop);

        alerts[0].setAlert(new Alert());
        alerts[1].setAlert(new Alert());
        alerts[2].setAlert(new Alert());
        alerts[3].setAlert(new Alert());

        alerts[0].setId("0");
        alerts[1].setId("1");
        alerts[2].setId("2");
        alerts[3].setId("3");
    }

    private AlertPatchServiceImpl getAlertPatchServiceImpl() {
        AlertPatchServiceImpl alertPatchService = new AlertPatchServiceImpl(new Graph());
        return alertPatchService;
    }

    @Test
    public void testApplyAndExpire() {
        AlertPatchServiceImpl instance = getAlertPatchServiceImpl();
        instance.apply(alerts[0]);

        assertTrue(instance.getStopPatches(testStop).contains(alerts[0]));
        assertTrue(instance.getRoutePatches(testRoute).contains(alerts[0]));

        instance.expire(Collections.singleton(alerts[0].getId()));

        assertTrue(instance.getStopPatches(testStop).isEmpty());
        assertTrue(instance.getRoutePatches(testRoute).isEmpty());
    }

    @Test
    public void testExpire() {
        Set<String> purge = new HashSet<String>();
        AlertPatchServiceImpl instance = getAlertPatchServiceImpl();
        for(TestAlertPatch alert : alerts) {
            instance.apply(alert);
        }

        purge.add(alerts[0].getId());
        purge.add(alerts[1].getId());

        instance.expire(purge);

        assertEquals(2, instance.getAllAlertPatches().size());
        assertFalse(instance.getAllAlertPatches().contains(alerts[0]));
        assertFalse(instance.getAllAlertPatches().contains(alerts[1]));
        assertTrue(instance.getAllAlertPatches().contains(alerts[2]));
        assertTrue(instance.getAllAlertPatches().contains(alerts[3]));
    }

    @Test
    public void testExpireAll() {
        Set<String> purge = new HashSet<String>();
        AlertPatchServiceImpl instance = getAlertPatchServiceImpl();
        for(TestAlertPatch alert : alerts) {
            purge.add(alert.getId());
            instance.apply(alert);
        }

        instance.expireAll();

        assertTrue(instance.getAllAlertPatches().isEmpty());
    }

    @Test
    public void testExpireAllExcept() {
        AlertPatchServiceImpl instance = getAlertPatchServiceImpl();
        for(TestAlertPatch alert : alerts) {
            instance.apply(alert);
        }

        instance.expireAllExcept(Collections.singleton(alerts[0].getId()));

        assertEquals(1, instance.getAllAlertPatches().size());
        assertTrue(instance.getAllAlertPatches().contains(alerts[0]));
    }
}
