package org.opentripplanner.analyst.batch;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IndividualFactory {

    private static final Logger LOG = LoggerFactory.getLogger(IndividualFactory.class);

    @Autowired private SampleFactory sampleFactory;
    
    public Individual build(double lon, double lat) {
        Sample sample = sampleFactory.getSample(lon, lat);
        return new Individual("none", sample, lon, lat, 0);
    }

    public Individual build(String label, double lon, double lat, double input) {
        Sample sample = sampleFactory.getSample(lon, lat);
        return new Individual(label, sample, lon, lat, input);
    }

}
