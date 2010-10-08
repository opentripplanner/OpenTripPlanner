package org.opentripplanner.geocoder.google;

import flexjson.JSONDeserializer;

public class GoogleJsonDeserializer {
	
	@SuppressWarnings("unchecked")
	private JSONDeserializer jsonDeserializer;
	
	@SuppressWarnings("unchecked")
	public GoogleJsonDeserializer() {
		jsonDeserializer = new JSONDeserializer()
        	.use(null, GoogleGeocoderResults.class);
	}

	public GoogleGeocoderResults parseResults(String content) {
		return (GoogleGeocoderResults) jsonDeserializer.deserialize(content);
	}
}
