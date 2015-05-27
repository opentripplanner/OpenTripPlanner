/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
        super("//*[name()='ms:CI_VCUB_P']");
    }

    public BikeRentalStation makeStation(Map<String, String> attributes) {
        BikeRentalStation brstation = new BikeRentalStation();
        brstation.id = attributes.get("ms:GID").trim();
        String[] coordinates = attributes.get("ms:msGeometry").trim().split(" ");
        if (coordinates.length >= 2) {
            brstation.x = Double.parseDouble(coordinates[1]);
            brstation.y = Double.parseDouble(coordinates[0]);
        }
        if (brstation.x == 0 || brstation.y == 0)
            return null;
        brstation.name = new NonLocalizedString(attributes.get("ms:NOM"));
        boolean connected = "CONNECTEE".equalsIgnoreCase(attributes.get("ms:ETAT"));
        brstation.realTimeData = connected;
        String nbPlaces = attributes.get("ms:NBPLACES");
        if (nbPlaces != null)
            brstation.spacesAvailable = Integer.parseInt(nbPlaces);
        String nbVelos = attributes.get("ms:NBVELOS");
        if (nbVelos != null)
            brstation.bikesAvailable = Integer.parseInt(nbVelos);
        @SuppressWarnings("unused")
        String type = attributes.get("ms:TYPE");
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
