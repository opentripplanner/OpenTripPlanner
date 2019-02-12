package org.opentripplanner.updater.bike_rental;

import java.util.HashSet;
import java.util.Map;

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

        if (networkName != null && !networkName.isEmpty()) {
            this.networkName = networkName;
        } else {
            this.networkName = "NextBike";
        }
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {

        // some place entries appear to actually be checked-out bikes, not stations
        if (attributes.get("bike") != null) {
            return null;
        }

        BikeRentalStation brstation = new BikeRentalStation();

        brstation.networks = new HashSet<String>();
        brstation.networks.add(this.networkName);

        brstation.id = attributes.get("number");
        brstation.x = Double.parseDouble(attributes.get("lng"));
        brstation.y = Double.parseDouble(attributes.get("lat"));
        brstation.name = new NonLocalizedString(attributes.get("name"));
        brstation.spacesAvailable = Integer.parseInt(attributes.get("bike_racks"));

        // number of bikes available is reported as "5+" if >= 5
        String numBikes = attributes.get("bikes");
        if (numBikes.equals("5+")) {
            brstation.bikesAvailable = 5;
        } else {
            brstation.bikesAvailable = Integer.parseInt(numBikes);
        }
        
        return brstation;
    }
}
