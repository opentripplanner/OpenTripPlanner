package org.opentripplanner.updater.bike_rental.datasources;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Map;

class KeolisRennesBikeRentalDataSource extends GenericXmlBikeRentalDataSource {
    public KeolisRennesBikeRentalDataSource(BikeRentalDataSourceParameters config) {
        super(config,"//opendata/answer/data/station");
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        //FIXME: I have no idea what state actually means
        if (!"1".equals(attributes.get("state"))) {
            return null;
        }
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = attributes.get("number");
        brstation.x = Double.parseDouble(attributes.get("longitude"));
        brstation.y = Double.parseDouble(attributes.get("latitude"));
        brstation.name = new NonLocalizedString(attributes.get("name"));
        brstation.bikesAvailable = Integer.parseInt(attributes.get("bikesavailable"));
        brstation.spacesAvailable = Integer.parseInt(attributes.get("slotsavailable"));
        return brstation;
    }
}
