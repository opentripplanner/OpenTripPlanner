package org.opentripplanner.api.model.transit;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AgencyList {
    @XmlElementWrapper
    public Collection<String> agencyIds;
}
