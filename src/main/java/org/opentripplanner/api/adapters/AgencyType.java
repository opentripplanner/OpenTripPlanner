package org.opentripplanner.api.adapters;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.model.Agency;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

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
    public String id;

    @XmlAttribute
    @JsonSerialize
    public String name;

    @XmlAttribute
    @JsonSerialize
    public String url;

    @XmlAttribute
    @JsonSerialize
    public String timezone;

    @XmlAttribute
    @JsonSerialize
    public String lang;

    @XmlAttribute
    @JsonSerialize
    public String phone;

    @XmlAttribute
    @JsonSerialize
    public String fareUrl;

}