package org.opentripplanner.osm;

import java.io.Serializable;

public class Node extends Tagged implements Serializable {

    private static final long serialVersionUID = 1L;

    // do with int16s
    public float lat;
    public float lon;
    
}
