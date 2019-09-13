package org.opentripplanner.api.model.alertpatch;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.routing.alertpatch.AlertPatch;

public class AlertPatchResponse {
    public List<AlertPatch> alertPatches;

    public void addAlertPatch(AlertPatch alertPatch) {
        if (alertPatches == null) {
            alertPatches = new ArrayList<AlertPatch>();
        }
        alertPatches.add(alertPatch);
    }
}
