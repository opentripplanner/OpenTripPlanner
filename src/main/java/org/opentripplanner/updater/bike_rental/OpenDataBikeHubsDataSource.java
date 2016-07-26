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
import java.util.HashMap;

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
public final class OpenDataBikeHubsDataSource extends GenericJsonBikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(OpenDataBikeHubsDataSource.class);

    private String jsonParsePath;

    ArrayList<BikeRentalStation> stations = new ArrayList<BikeRentalStation>();
   
    HashMap<String, BikeRentalStation> hubs = new HashMap<String, BikeRentalStation>();
 
    public OpenDataBikeHubsDataSource() {
    	jsonParsePath = "data/stations";
    }

    public boolean update() {
	String url = getUrl();
	// First get initial hub data (names, etc)
        try {
            InputStream data = HttpUtils.getData(url + "/station_information.json");
            if (data == null) {
                log.warn("Failed to get data from url " + url);
                return false;
            }
            parseStationInformation(data);
            data.close();
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing bike rental feed from " + url, e);
            return false;
        } catch (JsonProcessingException e) {
            log.warn("Error parsing bike rental feed from " + url + "(bad JSON of some sort)", e);
            return false;
        } catch (IOException e) {
            log.warn("Error reading bike rental feed from " + url, e);
            return false;
        }

	// Now get the hub status
        try {
            InputStream data = HttpUtils.getData(url + "/station_status.json");
            if (data == null) {
                log.warn("Failed to get data from url " + url);
                return false;
            }
            parseJSON(data);
            data.close();
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing bike rental feed from " + url, e);
            return false;
        } catch (JsonProcessingException e) {
            log.warn("Error parsing bike rental feed from " + url + "(bad JSON of some sort)", e);
            return false;
        } catch (IOException e) {
            log.warn("Error reading bike rental feed from " + url, e);
            return false;
        }

        return true;
    }

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        return stations;
    }

   private void parseStationInformation(InputStream dataStream) throws JsonProcessingException, IllegalArgumentException,
      IOException {

        String rentalString = convertStreamToString(dataStream);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(rentalString);

        if (!jsonParsePath.equals("")) {
            String delimiter = "/";
            String[] parseElement = jsonParsePath.split(delimiter);
            for(int i =0; i < parseElement.length ; i++) {
                rootNode = rootNode.path(parseElement[i]);
            }

            if (rootNode.isMissingNode()) {
                throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
              }
        }

        for (int i = 0; i < rootNode.size(); i++) {
            JsonNode node = rootNode.get(i);
            if (node == null) {
                continue;
            }

	    // get hub id, name, and x/y and save in local map
            makeStation(node);
	}
     
    }

    private void parseJSON(InputStream dataStream) throws JsonProcessingException, IllegalArgumentException,
      IOException {

        ArrayList<BikeRentalStation> out = new ArrayList<BikeRentalStation>();

        String rentalString = convertStreamToString(dataStream);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(rentalString);

        if (!jsonParsePath.equals("")) {
            String delimiter = "/";
            String[] parseElement = jsonParsePath.split(delimiter);
            for(int i =0; i < parseElement.length ; i++) {
                rootNode = rootNode.path(parseElement[i]);
            }

            if (rootNode.isMissingNode()) {
                throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
              }
        }

        for (int i = 0; i < rootNode.size(); i++) {
            JsonNode node = rootNode.get(i);
            if (node == null) {
                continue;
            }
            BikeRentalStation brstation = makeStation(node);
            if (brstation != null) {
                out.add(brstation);
	    }
        }
        synchronized(this) {
            stations = out;
        }
    }

    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    public BikeRentalStation makeStation(JsonNode stationNode) {

        BikeRentalStation brStation = null;

	if (stationNode.get("name") != null) {
		brStation = new BikeRentalStation();
	        brStation.id = String.valueOf(stationNode.get("station_id").textValue());
 	 	brStation.x = stationNode.get("lon").doubleValue();// / 1000000.0;
		brStation.y = stationNode.get("lat").doubleValue();// / 1000000.0;
	        brStation.name = new NonLocalizedString(stationNode.get("name").textValue());
		hubs.put( brStation.id, brStation );
		return null;
 	 }

 	 brStation = hubs.get( stationNode.get("station_id").textValue() );

         brStation.bikesAvailable = stationNode.get("num_bikes_available").intValue();
         brStation.spacesAvailable = stationNode.get("num_docks_available").intValue();

	 if (stationNode.get("is_renting").intValue() != 1) 
		return null; // don't add a broken or unavailable hub

	return brStation;
    }

}
