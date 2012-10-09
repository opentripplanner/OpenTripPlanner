package org.opentripplanner.routing.bike_rental;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class BikeRentalStation implements Serializable {
    private static final long serialVersionUID = 8311460609708089384L;

    @XmlAttribute
    @JsonSerialize
    public String id;
    @XmlAttribute
    @JsonSerialize
    public String name;
    @XmlAttribute
    @JsonSerialize
    public double x, y; //longitude, latitude
    @XmlAttribute
    @JsonSerialize
    public int bikesAvailable = Integer.MAX_VALUE;
    @XmlAttribute
    @JsonSerialize
    public int spacesAvailable = Integer.MAX_VALUE;
    
    public boolean equals(Object o) {
        if (!(o instanceof BikeRentalStation)) {
            return false;
        }
        BikeRentalStation other = (BikeRentalStation) o;
        return other.id.equals(id);
    }
    
    public int hashCode() {
        return id.hashCode() + 1;
    }
}
