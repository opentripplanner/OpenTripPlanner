/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or
 * a platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class Stop extends TransitEntity<FeedScopedId> implements StationElement {

    private static final long serialVersionUID = 1L;

    private FeedScopedId id;

    /**
     * Name of the stop if provided. Usually only code is used.
     */
    private String name;

    /** Center point/location for stop. */
    private WgsCoordinate coordinate;

    /**
     * Public facing stop code (short text or number).
     */
    private String code;

    /**
     * Additional information about the stop (if needed).
     */
    private String description;

    /**
     * Used for GTFS fare information.
     */
    private String zone;

    /**
     * URL to a web page containing information about this particular stop.
     */
    private String url;

    private Station parentStation;

    private WheelChairBoarding wheelchairBoarding;

    private String levelName;

    private double levelIndex;

    private HashSet<BoardingArea> boardingAreas;

    public Stop() {}

    public Stop(FeedScopedId id) {
        this.id = id;
    }

    @Override public FeedScopedId getId() {
        return id;
    }

    @Override public void setId(FeedScopedId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return coordinate == null ? 0 : coordinate.latitude();
    }

    public double getLon() {
        return coordinate == null ? 0 : coordinate.longitude();
    }

    public WgsCoordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(WgsCoordinate coordinate) {
        this.coordinate = coordinate;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Station getParentStation() {
        return parentStation;
    }

    public void setParentStation(Station parentStation) {
        this.parentStation = parentStation;
    }

    public WheelChairBoarding getWheelchairBoarding() {
        return wheelchairBoarding;
    }

    public void setWheelchairBoarding(WheelChairBoarding wheelchairBoarding) {
        this.wheelchairBoarding = wheelchairBoarding;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public double getLevelIndex() {
        return levelIndex;
    }

    public void setLevelIndex(double levelIndex) {
        this.levelIndex = levelIndex;
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
