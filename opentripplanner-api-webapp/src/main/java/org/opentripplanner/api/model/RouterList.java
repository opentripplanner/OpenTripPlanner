package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RouterList")
public class RouterList {
    @XmlElements(value = { @XmlElement(name="routerInfo") })
    public List<RouterInfo> routerInfo = new ArrayList<RouterInfo>();
    
}
