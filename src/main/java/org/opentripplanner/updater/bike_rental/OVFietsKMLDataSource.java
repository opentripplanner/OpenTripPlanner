package org.opentripplanner.updater.bike_rental;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Map;

public class OVFietsKMLDataSource extends GenericXmlBikeRentalDataSource {
    public OVFietsKMLDataSource() {
        super("//*[name()='Placemark']");
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = attributes.get("name") + attributes.get("Point").trim();
        String[] coordinates = attributes.get("Point").trim().split(",");
        brstation.longitude = Double.parseDouble(coordinates[0]);
        brstation.latitude = Double.parseDouble(coordinates[1]);
        if (brstation.longitude == 0 || brstation.latitude == 0)
            return null;
        brstation.name = new NonLocalizedString(attributes.get("name"));
        return brstation;
    }
}
