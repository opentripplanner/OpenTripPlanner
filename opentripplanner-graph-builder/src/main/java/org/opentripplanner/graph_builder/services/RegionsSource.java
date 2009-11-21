package org.opentripplanner.graph_builder.services;

import com.vividsolutions.jts.geom.Envelope;

public interface RegionsSource {
    public Iterable<Envelope> getRegions();
}
