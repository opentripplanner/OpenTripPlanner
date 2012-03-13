package org.opentripplanner.api.model.analysis;

import javax.xml.bind.annotation.XmlAttribute;

public class AnnotationObject {

    @XmlAttribute
    public String vertex;

    @XmlAttribute
    public Integer edge;

    @XmlAttribute
    public String message;

    @XmlAttribute
    public String agency;

    @XmlAttribute
    public String id;

}
