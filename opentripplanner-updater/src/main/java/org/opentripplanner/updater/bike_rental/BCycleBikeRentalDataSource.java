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

import org.codehaus.jackson.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

/**
 * Build a BikeRentalStation object from a B-Cycle data source JsonNode object.
 *
 * @see GenericJSONBikeRentalDataSource
 */
public class BCycleBikeRentalDataSource extends GenericJSONBikeRentalDataSource {
   public BCycleBikeRentalDataSource() {
       super("d/list");
   }

   public BikeRentalStation makeStation(JsonNode kioskNode) {

       if (!kioskNode.path("Status").getTextValue().equals("Active")) {
           return null;
       }


       BikeRentalStation brstation = new BikeRentalStation();

       brstation.id = kioskNode.path("Id").toString();
       brstation.x = kioskNode.path("Location").path("Longitude").asDouble();
       brstation.y = kioskNode.path("Location").path("Latitude").asDouble();
       brstation.name =  kioskNode.path("Name").getTextValue();
       brstation.bikesAvailable = kioskNode.path("BikesAvailable").asInt();
       brstation.spacesAvailable = kioskNode.path("DocksAvailable").asInt();

       return brstation;
   }
}
