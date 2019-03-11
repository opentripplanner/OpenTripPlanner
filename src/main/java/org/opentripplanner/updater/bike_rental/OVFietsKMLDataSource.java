package org.opentripplanner.updater.bike_rental;

import java.util.Map;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

public class OVFietsKMLDataSource extends GenericXmlBikeRentalDataSource {
    public OVFietsKMLDataSource() {
        super("//*[name()='Placemark']");
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = attributes.get("name")+attributes.get("Point").trim();
        String[] coordinates = attributes.get("Point").trim().split(",");
        brstation.x = Double.parseDouble(coordinates[0]);
        brstation.y = Double.parseDouble(coordinates[1]);
        if ( brstation.x == 0 || brstation.y == 0)
            return null;
        brstation.name = new NonLocalizedString(attributes.get("name"));
        return brstation;
    }
}
