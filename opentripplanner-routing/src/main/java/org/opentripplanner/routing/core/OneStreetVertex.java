package org.opentripplanner.routing.core;

import java.io.Serializable;

import org.opentripplanner.routing.edgetype.Street;

public abstract class OneStreetVertex implements Vertex, Serializable {

    private static final long serialVersionUID = -4494481001416132418L;
    public Street inStreet, outStreet;
}
