package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

public class ApiAgency implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id;

    public String name;

    public String url;

    public String timezone;

    public String lang;

    public String phone;

    public String fareUrl;

    public String brandingUrl;


    public String toString() {
        return "<Agency " + this.id + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ApiAgency apiAgency = (ApiAgency) o;
        return Objects.equals(id, apiAgency.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
