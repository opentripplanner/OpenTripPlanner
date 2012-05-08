package org.opentripplanner.api.ws;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;

@XmlRootElement(name="BikeRentalStationList")
public class BikeRentalStationList {
    @XmlElements(value = { @XmlElement(name="station") })
    public List<BikeRentalStation> stations = new ArrayList<BikeRentalStation>();
}
