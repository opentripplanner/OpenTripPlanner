package org.opentripplanner.routing.bike_rental;

import javax.xml.bind.annotation.XmlAttribute;

public class BikeRentalStation {
    @XmlAttribute
    public String id;
    @XmlAttribute
    public String name;
    @XmlAttribute
    public double x, y; //latitude, longitude
    @XmlAttribute
    public int bikesAvailable;
    @XmlAttribute
    public int spacesAvailable;
    
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
