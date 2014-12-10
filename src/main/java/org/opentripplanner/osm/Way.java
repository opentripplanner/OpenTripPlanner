package org.opentripplanner.osm;

import java.io.Serializable;

public class Way extends Tagged implements Serializable {

    private static final long serialVersionUID = 1L;

    public long[] nodes;
    
    @Override
    public String toString() {
        return String.format("Way with tags %s and nodes %s", tags, nodes);
    }
    
}
