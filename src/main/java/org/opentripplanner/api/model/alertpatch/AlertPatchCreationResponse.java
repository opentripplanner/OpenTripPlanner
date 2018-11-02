package org.opentripplanner.api.model.alertpatch;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AlertPatchCreationResponse")
public class AlertPatchCreationResponse {
    @XmlElement
    public String status;
}
