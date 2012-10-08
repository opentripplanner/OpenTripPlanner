package org.opentripplanner.api.model.internals;

import javax.xml.bind.annotation.XmlAttribute;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class AnnotationObject {

    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @XmlAttribute
    public String vertex;

    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @XmlAttribute
    public Integer edge;

    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @XmlAttribute
    public String message;

    @JsonSerialize
    @XmlAttribute
    public String agency;

    @JsonSerialize
    @XmlAttribute
    public String id;

}
