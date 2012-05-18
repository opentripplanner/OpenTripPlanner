package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RouterIdList")
public class RouterIdList {
    @XmlElements(value = { @XmlElement(name="routerId") })
    public List<String> routerIds;
    
    public RouterIdList(Collection<String> graphIds) {
        this.routerIds = new ArrayList<String>(graphIds);
    }

    public RouterIdList() {}
}
