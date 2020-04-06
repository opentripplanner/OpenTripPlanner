package org.opentripplanner.api.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AgencyAndId")
public class ApiFeedScopedId {
    public ApiFeedScopedId(String agency, String id) {
        this.agency = agency;
        this.id = id;
    }

    @XmlAttribute
    @JsonSerialize
    public String agency;

    @XmlAttribute
    @JsonSerialize
    public String id;
}
