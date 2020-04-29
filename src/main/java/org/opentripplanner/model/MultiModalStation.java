package org.opentripplanner.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;


/**
 * The next level grouping of stops above Station. Equivalent to NeTEx multimodal StopPlace. As
 * a Station (NeTEx StopPlace) only supports a single transit mode, you are required to group
 * several Stations together using a MultiModalStation in order to support several modes. This
 * entity is not part of GTFS.
 */
public class MultiModalStation extends TransitEntity<FeedScopedId> implements StopCollection {
    private static final long serialVersionUID = 1L;

    private final FeedScopedId id;

    private final Collection<Station> childStations;

    private String name;

    private WgsCoordinate coordinate;

    private String code;

    private String description;

    private String url;

    /**
     * Create a new multi modal station with the given list of child stations.
     */
    public MultiModalStation(FeedScopedId feedScopedId, Collection<Station> children) {
        this.id = feedScopedId;
        this.childStations = Collections.unmodifiableCollection(new ArrayList<>(children));
    }

    @Override
    public FeedScopedId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return coordinate.latitude();
    }

    public double getLon() {
        return coordinate.longitude();
    }

    public void setCoordinate(WgsCoordinate coordinate) {
        this.coordinate = coordinate;
    }

    /**
     * Public facing station code (short text or number)
     */
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Additional information about the station (if needed)
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * URL to a web page containing information about this particular station
     */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Collection<Stop> getChildStops() {
        return this.childStations.stream()
                .flatMap(s -> s.getChildStops().stream())
                .collect(Collectors.toUnmodifiableList());
    }

    public Collection<Station> getChildStations() {
        return this.childStations;
    }

    @Override
    public String toString() {
        return "<MultiModal station " + this.id + ">";
    }
}
