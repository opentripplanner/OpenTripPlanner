package org.opentripplanner.routing.core;


public class GenericStreetIntersectionVertex extends GenericVertex implements StreetIntersectionVertex {

    private static final long serialVersionUID = -4775427904985183660L;

    public GenericStreetIntersectionVertex(String label, double x, double y) {
        super(label, x, y);
    }

    public GenericStreetIntersectionVertex(String label, double x, double y, String name) {
        super(label, x, y, name);
    }

}
