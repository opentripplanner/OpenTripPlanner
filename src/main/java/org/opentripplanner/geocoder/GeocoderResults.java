package org.opentripplanner.geocoder;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;


public class GeocoderResults {

    private String error;
    private Collection<GeocoderResult> results;
    
    public GeocoderResults() {}
    
    public GeocoderResults(String error) {
        this.error = error;
    }
    
    public GeocoderResults(Collection<GeocoderResult> results) {
        this.results = results;
    }

    public String getError() {
        return error;
    }

    
    public void setError(String error) {
        this.error = error;
    }

    @JsonProperty(value="results")
    public Collection<GeocoderResult> getResults() {
        return results;
    }

    
    public void setResults(Collection<GeocoderResult> results) {
        this.results = results;
    }
    
    public void addResult(GeocoderResult result) {
        if (results == null)
            results = new ArrayList<GeocoderResult>();
        results.add(result);
    }

    public int getCount() {
        return results != null ? results.size() : 0;
    }
    
}
