package org.opentripplanner.geocoder.yahoo;

import flexjson.JSONDeserializer;

public class YahooJsonDeserializer {
	
	@SuppressWarnings("unchecked")
	private JSONDeserializer jsonDeserializer;
	
	@SuppressWarnings("unchecked")
	public YahooJsonDeserializer() {
		jsonDeserializer = new JSONDeserializer()
        	.use(null, YahooGeocoderResults.class);
	}

	public YahooGeocoderResults parseResults(String json) {
		return (YahooGeocoderResults) jsonDeserializer.deserialize(json);
	}
}
