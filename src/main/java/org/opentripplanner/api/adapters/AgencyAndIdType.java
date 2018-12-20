package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "AgencyAndId")
public class AgencyAndIdType {
    public AgencyAndIdType(String agency, String id) {
        this.agency = agency;
        this.id = id;
    }

    public AgencyAndIdType() {
    }

    @XmlAttribute
    @JsonSerialize
    String agency;

    @XmlAttribute
    @JsonSerialize
    String id;

}
