package org.opentripplanner.api.model.alertpatch;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.alertpatch.AlertPatch;

@XmlRootElement
public class AlertPatchResponse {
    @XmlElementWrapper
    @XmlElements({
        @XmlElement(name = "AlertPatch", type = AlertPatch.class)
        })
    public List<AlertPatch> alertPatches;

    public void addAlertPatch(AlertPatch alertPatch) {
        if (alertPatches == null) {
            alertPatches = new ArrayList<AlertPatch>();
        }
        alertPatches.add(alertPatch);
    }
}
