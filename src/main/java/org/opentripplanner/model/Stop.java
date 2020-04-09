/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or
 * a platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class Stop extends StationElement {

    private static final long serialVersionUID = 2L;

    /**
     * Used for GTFS fare information.
     */
    private String zone;

    /**
     * URL to a web page containing information about this particular stop.
     */
    private String url;

    private HashSet<BoardingArea> boardingAreas;

    public Stop() {}

    public Stop(FeedScopedId id) {
        this.id = id;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void addBoardingArea(BoardingArea boardingArea) {
        if (boardingAreas == null) {
          boardingAreas = new HashSet<>();
        }
        boardingAreas.add(boardingArea);
    }

    public Collection<BoardingArea> getBoardingAreas() {
      return boardingAreas != null ? boardingAreas : Collections.emptySet();
    }

    @Override
    public String toString() {
        return "<Stop " + this.id + ">";
    }
}
