package org.opentripplanner.geocoder.yahoo;

import java.util.List;

public class YahooGeocoderResultSet {
	
    private List<YahooGeocoderResult> Results;

	public List<YahooGeocoderResult> getResults() {
		return Results;
	}

	public void setResults(List<YahooGeocoderResult> results) {
		Results = results;
	}
    
}
