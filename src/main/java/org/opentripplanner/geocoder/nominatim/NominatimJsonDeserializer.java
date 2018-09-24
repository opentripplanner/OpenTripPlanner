package org.opentripplanner.geocoder.nominatim;

import flexjson.JSONDeserializer;
import java.util.List;

public class NominatimJsonDeserializer {
    
    private JSONDeserializer<List<NominatimGeocoderResult>> jsonDeserializer;
    
    public NominatimJsonDeserializer() {
            jsonDeserializer = new JSONDeserializer<List<NominatimGeocoderResult>>().use("values", NominatimGeocoderResult.class);
    }
    
    public List<NominatimGeocoderResult> parseResults(String content) {
            return jsonDeserializer.deserialize(content);
    }
}
