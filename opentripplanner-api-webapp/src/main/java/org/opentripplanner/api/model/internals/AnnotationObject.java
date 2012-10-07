package org.opentripplanner.api.model.internals;

import javax.xml.bind.annotation.XmlAttribute;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

public class AnnotationObject {

    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlAttribute
    public String vertex;

    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlAttribute
    public Integer edge;

    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlAttribute
    public String message;

    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlAttribute
    public String agency;

    @JsonSerialize(include=Inclusion.NON_NULL)
    @XmlAttribute
    public String id;

}
