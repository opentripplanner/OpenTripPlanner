package org.opentripplanner.routing.vehicle_rental;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.util.I18NString;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Set;

public abstract class RentalStation {
    @XmlAttribute
    @JsonSerialize
    public String id;

    @XmlTransient
    @JsonIgnore
    public I18NString name;

    @XmlAttribute
    @JsonSerialize
    public double x, y; //longitude, latitude

    @XmlAttribute
    @JsonSerialize
    public boolean allowDropoff = true;

    @XmlAttribute
    @JsonSerialize
    public boolean allowPickup = true;

    /**
     * List of compatible network names. Null (default) to be compatible with all.
     */
    @XmlAttribute
    @JsonSerialize
    public Set<String> networks = null;
}

