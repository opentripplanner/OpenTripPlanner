package org.opentripplanner.api.resource;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class BikeRentalStationList {
    public List<BikeRentalStation> stations = new ArrayList<BikeRentalStation>();
}
