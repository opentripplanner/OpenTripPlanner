package org.opentripplanner.analyst.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;

/**
 * A conveyor for an isochrone.
 * 
 * @author laurent
 */
public class IsochroneData implements Serializable {
    private static final long serialVersionUID = 1L;

    public int cutoffSec;

    public Geometry geometry;

    public transient Geometry debugGeometry;

    public IsochroneData(@JsonProperty("cutoffSec") int cutoffSec, @JsonProperty("geometry") Geometry geometry) {
        this.cutoffSec = cutoffSec;
        this.geometry = geometry;
    }

    public String toString() {
        return String.format("<isochrone %s sec>", cutoffSec);
    }
}
