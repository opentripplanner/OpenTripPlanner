package org.opentripplanner.updater.bike_rental;

import java.util.Map;

public class BixiBikeRentalDataSource extends GenericXmlBikeRentalDataSource {
    public BixiBikeRentalDataSource() {
        super("//stations/station");
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        if (!"true".equals(attributes.get("installed"))) {
            return null;
        }
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = attributes.get("id");
        brstation.x = Double.parseDouble(attributes.get("long"));
        brstation.y = Double.parseDouble(attributes.get("lat"));
        brstation.name = attributes.get("name");
        brstation.bikesAvailable = Integer.parseInt(attributes.get("nbBikes"));
        brstation.spacesAvailable = Integer.parseInt(attributes.get("nbEmptyDocks"));
        return brstation;
    }
}
