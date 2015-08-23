package org.opentripplanner.streets;

/**
 * Represents a potential split point along an existing edge, retaining some geometric calculation state so that
 * once the best candidate is found more detailed calculations can continue.
 */
public class Split {

    double distSquared = Double.POSITIVE_INFINITY;
    int edge = -1;
    int seg = 0; // the segment within the edge that is closest to the search point
    double frac = 0; // the fraction along that segment where a link should occur
    double fLon; // the x coordinate of the link point along the edge
    double fLat; // the y coordinate of the link point along the edge

    // The following fields require more calculations and are only set once a best edge is found.
    double lengthBefore = 0; // the accumulated distance along the edge geometry up to the split point

    public void setFrom(Split other) {
        distSquared = other.distSquared;
        edge = other.edge;
        seg = other.seg;
        frac = other.frac;
        fLon = other.fLon;
        fLat = other.fLat;
    }

}