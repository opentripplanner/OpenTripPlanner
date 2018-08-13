package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.model.Agency;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "Agency")
public class AgencyType {

    public AgencyType(String id, String name, String url, String timezone, String lang,
            String phone, String fareUrl) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.timezone = timezone;
        this.lang = lang;
        this.phone = phone;
        this.fareUrl = fareUrl;
    }

    public AgencyType(Agency arg) {
        this.id = arg.getId();
        this.name = arg.getName();
        this.url = arg.getUrl();
        this.timezone = arg.getTimezone();
        this.lang = arg.getLang();
        this.phone = arg.getPhone();
        this.fareUrl = arg.getFareUrl();
    }

    public AgencyType() {
    }

    @XmlAttribute
    @JsonSerialize
    String id;

    @XmlAttribute
    @JsonSerialize
    String name;

    @XmlAttribute
    @JsonSerialize
    String url;

    @XmlAttribute
    @JsonSerialize
    String timezone;

    @XmlAttribute
    @JsonSerialize
    String lang;

    @XmlAttribute
    @JsonSerialize
    String phone;

    @XmlAttribute
    @JsonSerialize
    String fareUrl;

}