package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "RouterList")
public class RouterList {
    @XmlElements(value = { @XmlElement(name="routerInfo") })
    public List<ApiRouterInfo> routerInfo = new ArrayList<>();
    
}
