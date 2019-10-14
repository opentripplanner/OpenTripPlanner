package org.opentripplanner.routing.impl;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.Graph;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlertPatchServiceImplTest {
    private TestAlertPatch alertOnStopAndRoute_A = new TestAlertPatch();
    private TestAlertPatch alert_B = new TestAlertPatch();
    private TestAlertPatch alert_C = new TestAlertPatch();

    private FeedScopedId testStopId = new FeedScopedId("A", "A");
    private FeedScopedId testRouteId = new FeedScopedId("B", "B");

    @Before
    public void setup() {
        alertOnStopAndRoute_A = new TestAlertPatch();
        alertOnStopAndRoute_A.setId("A");
        alertOnStopAndRoute_A.setStop(testStopId);
        alertOnStopAndRoute_A.setRoute(testRouteId);
        alertOnStopAndRoute_A.setAlert(new Alert());

        alert_B = new TestAlertPatch();
        alert_B.setId("B");
        alert_B.setAlert(new Alert());

        alert_C = new TestAlertPatch();
        alert_C.setId("C");
        alert_C.setAlert(new Alert());
    }

    private AlertPatchServiceImpl getAlertPatchServiceImpl() {
        AlertPatchServiceImpl alertPatchService = new AlertPatchServiceImpl(new Graph());
        return alertPatchService;
    }

    @Test
    public void testApplyAndExpire() {
        TestAlertPatch alert = new TestAlertPatch();
        alert.setId("A");
        alert.setRoute(testRouteId);
        alert.setStop(testStopId);

        AlertPatchServiceImpl instance = getAlertPatchServiceImpl();

        instance.apply(alert);
        assertTrue(instance.getStopAndRoutePatches(testStopId, testRouteId).contains(alert));

        instance.expire(Collections.singleton(alert.getId()));
        assertTrue(instance.getStopAndRoutePatches(testStopId, testRouteId).isEmpty());
    }

    @Test
    public void testExpire() {
        AlertPatchServiceImpl instance = getAlertPatchServiceImpl();
        instance.apply(alertOnStopAndRoute_A);
        instance.apply(alert_B);
        instance.apply(alert_C);

        instance.expire(List.of(alertOnStopAndRoute_A.getId(), alert_B.getId()));

        assertEquals(1, instance.getAllAlertPatches().size());
        assertFalse(instance.getAllAlertPatches().contains(alertOnStopAndRoute_A));
        assertFalse(instance.getAllAlertPatches().contains(alert_B));
        assertTrue(instance.getAllAlertPatches().contains(alert_C));
    }

    @Test
    public void testExpireAll() {
        AlertPatchServiceImpl instance = getAlertPatchServiceImpl();
        instance.apply(alertOnStopAndRoute_A);
        instance.apply(alert_B);

        instance.expireAll();
        assertTrue(instance.getAllAlertPatches().isEmpty());
    }

    @Test
    public void testExpireAllExcept() {
        AlertPatchServiceImpl instance = getAlertPatchServiceImpl();
        instance.apply(alertOnStopAndRoute_A);
        instance.apply(alert_B);
        instance.apply(alert_C);

        instance.expireAllExcept(List.of(alert_C.getId()));

        assertEquals(1, instance.getAllAlertPatches().size());
        assertTrue(instance.getAllAlertPatches().contains(alert_C));
    }

    private static class TestAlertPatch extends AlertPatch {
        @Override
        public void apply(Graph graph) {
            // NO-OP
        }

        @Override
        public void remove(Graph graph) {
            // NO-OP
        }
    }
}
