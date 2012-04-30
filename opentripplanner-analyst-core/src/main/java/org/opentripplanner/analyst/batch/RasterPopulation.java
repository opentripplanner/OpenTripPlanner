package org.opentripplanner.analyst.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RasterPopulation extends Population {

    private static final long serialVersionUID = 20120201L; //YYYYMMDD
    private static final Logger LOG = LoggerFactory.getLogger(RasterPopulation.class);
    public final int width, height;

    public RasterPopulation(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public RasterPopulation(int width, int height, Individual... individuals) {
        super(individuals);
        // TODO: check array dimension
        this.width = width;
        this.height = height;
    }

}
