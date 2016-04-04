package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class ShareBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

	private static final Logger log = LoggerFactory.getLogger(ShareBikeRentalDataSource.class);

	private String networkID;

	public ShareBikeRentalDataSource() throws UnsupportedEncodingException, MalformedURLException {
		super("result/LiveStationData");
	}

	/**
	 * ShareBike format http://www.sharebike.com/
	 * 
	 * URL for configuration:
	 * http://map.webservice.sharebike.com:8888/json/MapService/LiveStationData?
	 * APIKey=<Your API Key>&SystemID=<Bike System ID>
	 * 
	 * Currently used in Norway (Oslo, Trondheim, Drammen) , Barcelona, Mexico
	 * City, Milan, Stockholm, Antwerpen among others
	 * 
	 * <pre>
	 * {
		  "version": "1.1",
		  "result": {
		    "LastUpdated": "2016-01-18T13:55:25.610",
		    "LastUpdatedUTC": "2016-01-18T12:55:25.610",
		    "LiveStationData": [
		      {
		        "Address": "[Offline] storgata",
		        "AvailableBikeCount": "0",
		        "AvailableSlotCount": "0",
		        "LastContact": "",
		        "LastContactSeconds": "0",
		        "LastContactUTC": "1899-12-30T00:00:00",
		        "Latitude": "59.92758",
		        "Longitude": "10.71004",
		        "MinAvailableBikes": "0",
		        "MinAvailableSlots": "0",
		        "Online": false,
		        "SlotCount": "0",
		        "StationID": 1,
		        "StationName": "storgata"
		      },
		      ....
	 * </pre>
	 */

	@Override
	public BikeRentalStation makeStation(JsonNode rentalStationNode) {

		if(networkID == null) {
			// Get SystemID url parameter as StationIDs are not globally unique for
			// the ShareBike system
			String url = getUrl();
			try {
				Map<String, List<String>> urlParameters = splitQuery(new URL(url));
				List<String> systemIDs = urlParameters.get("SystemID");
				if (systemIDs != null && systemIDs.size() == 1) {
					networkID = systemIDs.get(0);
					log.info("Extracted SystemID from sharebike url: "+networkID);
				} else {
					log.error("Unable to extract SystemID query parameter from sharebike url, using random");
					networkID = UUID.randomUUID().toString();
				}
			} catch (UnsupportedEncodingException | MalformedURLException e) {
				log.error("Unable to extract SystemID query parameter from sharebike url, using random",e);
				networkID = UUID.randomUUID().toString();
			}
		}
		
		if (!rentalStationNode.path("Online").asBoolean()) {
			log.debug("Station is offline: " + rentalStationNode.path("StationName").asText());

			return null;
		}

		BikeRentalStation brstation = new BikeRentalStation();

		
        brstation.networks = new HashSet<String>();
        brstation.networks.add(this.networkID);
		
		brstation.id = networkID+"_"+rentalStationNode.path("StationID").toString();
		brstation.x = rentalStationNode.path("Longitude").asDouble();
		brstation.y = rentalStationNode.path("Latitude").asDouble();
		brstation.name = new NonLocalizedString(rentalStationNode.path("StationName").asText("").trim());
		brstation.bikesAvailable = rentalStationNode.path("AvailableBikeCount").asInt();
		brstation.spacesAvailable = rentalStationNode.path("AvailableSlotCount").asInt();

		return brstation;
	}

	public static Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		final String[] pairs = url.getQuery().split("&");
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
			if (!query_pairs.containsKey(key)) {
				query_pairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1
					? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
			query_pairs.get(key).add(value);
		}
		return query_pairs;
	}

}
