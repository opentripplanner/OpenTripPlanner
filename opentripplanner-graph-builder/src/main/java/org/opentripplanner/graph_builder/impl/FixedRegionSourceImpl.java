package org.opentripplanner.graph_builder.impl;

import java.util.Arrays;

import org.opentripplanner.graph_builder.services.RegionsSource;

import com.vividsolutions.jts.geom.Envelope;

public class FixedRegionSourceImpl implements RegionsSource {

    private double latFrom;

    private double lonFrom;

    private double latTo;

    private double lonTo;

    public void setLatFrom(double latFrom) {
        this.latFrom = latFrom;
    }

    public void setLonFrom(double lonFrom) {
        this.lonFrom = lonFrom;
    }

    public void setLatTo(double latTo) {
        this.latTo = latTo;
    }

    public void setLonTo(double lonTo) {
        this.lonTo = lonTo;
    }

    @Override
    public Iterable<Envelope> getRegions() {
        Envelope region = new Envelope(lonFrom, lonTo, latFrom, latTo);
        return Arrays.asList(region);
    }
}
