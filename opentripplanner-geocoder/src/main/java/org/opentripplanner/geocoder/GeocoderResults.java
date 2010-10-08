package org.opentripplanner.geocoder;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
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

    @XmlElement(required=false)
    public String getError() {
        return error;
    }

    
    public void setError(String error) {
        this.error = error;
    }

    
    @XmlElementWrapper(name="results")
    @XmlElement(name="result")
    public Collection<GeocoderResult> getResults() {
        return results;
    }

    
    public void setResults(Collection<GeocoderResult> results) {
        this.results = results;
    }
    
    public void addResult(GeocoderResult result) {
        results.add(result);
    }

    @XmlElement(name="count")
    public int getCount() {
        return results != null ? results.size() : 0;
    }
    
}
