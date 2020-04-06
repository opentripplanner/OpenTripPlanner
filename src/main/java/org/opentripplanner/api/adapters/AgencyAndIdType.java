package org.opentripplanner.api.adapters;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

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
    public String agency;

    @XmlAttribute
    @JsonSerialize
    public String id;

}
