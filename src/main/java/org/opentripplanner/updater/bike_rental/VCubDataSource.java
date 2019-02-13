package org.opentripplanner.updater.bike_rental;

import java.util.HashSet;
import java.util.Map;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Bike-rental station data source for the "Communauté d'Agglomération de Bordeaux" (CUB) VCub (aka
 * V^3) bike-rental network.
 * 
 * URL: http://data.lacub.fr/wfs?key=<your-API-key>&request=getfeature&service=wfs&version=1.1.0&
 * typename=CI_VCUB_P&srsname=epsg:4326
 * 
 * 
 * @author laurent
 */
public class VCubDataSource extends GenericXmlBikeRentalDataSource {

    public VCubDataSource() {
        super("//*[name()='bm:CI_VCUB_P']");
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = attributes.get("bm:GID").trim();
        String[] coordinates = attributes.get("bm:geometry").trim().split(" ");
        if (coordinates.length >= 2) {
            brstation.x = Double.parseDouble(coordinates[1]);
            brstation.y = Double.parseDouble(coordinates[0]);
        }
        if (brstation.x == 0 || brstation.y == 0)
            return null;
        brstation.name = new NonLocalizedString(attributes.get("bm:NOM"));
        boolean connected = "CONNECTEE".equalsIgnoreCase(attributes.get("bm:ETAT"));
        brstation.realTimeData = connected;
        String nbPlaces = attributes.get("bm:NBPLACES");
        if (nbPlaces != null)
            brstation.spacesAvailable = Integer.parseInt(nbPlaces);
        String nbVelos = attributes.get("bm:NBVELOS");
        if (nbVelos != null)
            brstation.bikesAvailable = Integer.parseInt(nbVelos);
        @SuppressWarnings("unused")
        String type = attributes.get("bm:TYPE");
        /*
         * Please see http://www.vcub.fr/stations-vcub-1 for more information on rules of VCUB vs
         * VCUB+. Apparently both network are compatible, VCUB+ only offer more renting options
         * which are not handled by OTP anyway.
         */
        brstation.networks = new HashSet<String>();
        brstation.networks.add("VCUB");
        brstation.networks.add("VCUB+");
        return brstation;
    }
}
