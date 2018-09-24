package org.opentripplanner.api.model.alertpatch;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.alertpatch.AlertPatch;

@XmlRootElement(name="AlertPatchSet")
public class AlertPatchSet {
    @XmlElements({
        @XmlElement(name = "AlertPatch", type = AlertPatch.class)
    })
    public List<AlertPatch> alertPatches;
}
