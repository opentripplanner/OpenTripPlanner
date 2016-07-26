/* 
 Copyright (C) 2016 University of South Florida.
 All rights reserved.

 This program is free software: you can redistribute it and/or
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Fetch Bike Rental JSON feeds and pass each record on to the specific rental subclass
 *
 * @see BikeRentalDataSource
 */
public final class OpenDataBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(OpenDataBikeRentalDataSource.class);
    private String url;

    ArrayList<BikeRentalStation> stations = new ArrayList<BikeRentalStation>();

    public OpenDataBikeRentalDataSource() {
        super("data/bikes");
    }

    public void configure(Graph graph, JsonNode jsonNode) {
        String url = jsonNode.path("url").asText();
        if (url == null)
            throw new IllegalArgumentException("Missing mandatory 'url' configuration.");
        setUrl(url + "/free_bike_status.json");
    }

    public BikeRentalStation makeStation(JsonNode stationNode) {

         BikeRentalStation brStation = new BikeRentalStation();

         brStation.id = String.valueOf(stationNode.get("bike_id").textValue());
         brStation.name =  new NonLocalizedString(stationNode.get("name").textValue());

         brStation.x = stationNode.get("lon").doubleValue();// / 1000000.0;
         brStation.y = stationNode.get("lat").doubleValue();// / 1000000.0;

         brStation.bikesAvailable = 1;
         brStation.spacesAvailable = 0;

	 if (stationNode.get("is_reserved").intValue() != 0 || stationNode.get("is_disabled").intValue() != 0) 
		return null; // don't add a broken or unavailable bike

	return brStation;
    }

}
