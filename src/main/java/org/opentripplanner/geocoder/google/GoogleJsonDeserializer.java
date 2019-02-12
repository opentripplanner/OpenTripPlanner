package org.opentripplanner.geocoder.google;

import flexjson.JSONDeserializer;

public class GoogleJsonDeserializer {
	
	private JSONDeserializer<GoogleGeocoderResults> jsonDeserializer;
	
	public GoogleJsonDeserializer() {
		jsonDeserializer = new JSONDeserializer<GoogleGeocoderResults>()
        	.use(null, GoogleGeocoderResults.class);
	}

	public GoogleGeocoderResults parseResults(String content) {
		return (GoogleGeocoderResults) jsonDeserializer.deserialize(content);
	}
}
