package org.opentripplanner.updater.bike_rental;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import com.google.common.base.Strings;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

/**
 * NextBike bike rental data source.
 * url: https://nextbike.net/maps/nextbike-live.xml?city=<city uid>
 * Check https://nextbike.net/maps/nextbike-live.xml full feed to find the city uid
 * to use for your data location.
 */
public class NextBikeRentalDataSource extends GenericXmlBikeRentalDataSource {
    
    private String networkName;
    
    public NextBikeRentalDataSource(String networkName) {
        super("//city/place");
        // this feed sets values on place node attributes, rather than in child elements
        this.setReadAttributes(true);
        this.networkName = Strings.isNullOrEmpty(networkName) ? "NextBike" : networkName;
    }
    
    public BikeRentalStation makeStation(Map<String, String> attributes) {
        
        // some place entries appear to actually be checked-out bikes, not stations
        if (attributes.get("bike") != null) {
            return null;
        }
        
        BikeRentalStation station = new BikeRentalStation();
        station.networks = new HashSet<>(Collections.singleton(this.networkName));
        station.id = attributes.get("number");
        station.x = Double.parseDouble(attributes.get("lng"));
        station.y = Double.parseDouble(attributes.get("lat"));
        station.name = new NonLocalizedString(attributes.get("name"));
        station.spacesAvailable = getAvailableSpaces(attributes);
        station.bikesAvailable  = getAvailableBikes(attributes);
        station.state = "Station on";
        return station;
    }

    private int getAvailableSpaces(Map<String, String> attributes) {
        String freeRacks = attributes.get("free_racks");
        
        if (freeRacks == null)
            return Integer.parseInt(attributes.get("bike_racks"));
        
        return Integer.parseInt(freeRacks);
    }
    
    private int getAvailableBikes(Map<String, String> attributes) {
        // number of available bikes is reported as "5+" if >= 5
        String bikes = attributes.get("bikes");
        
        if (bikes.equals("5+"))
            return 5;
        
        return Integer.parseInt(bikes);
    }
    
}
