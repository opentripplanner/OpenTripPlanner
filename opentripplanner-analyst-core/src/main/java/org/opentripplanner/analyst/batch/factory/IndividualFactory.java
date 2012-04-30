package org.opentripplanner.analyst.batch.factory;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IndividualFactory {

    @Autowired
    private SampleFactory sampleFactory;
    
    public Individual build(double lon, double lat) {
        Sample sample = sampleFactory.getSample(lon, lat);
        return new Individual("none", sample, lon, lat, 0);
    }

    public Individual build(String id, double lon, double lat, double data) {
        Sample sample = sampleFactory.getSample(lon, lat);
        return new Individual(id, sample, lon, lat, data);
    }

}
