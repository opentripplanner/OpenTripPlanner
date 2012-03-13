package org.opentripplanner.api.model.analysis;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

public class Annotations {

    @XmlElements(value = { @XmlElement(name = "annotation") })
    public List<Annotation> annotations;

}
