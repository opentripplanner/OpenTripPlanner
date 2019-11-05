package org.opentripplanner.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;


// TODO OTP2 MMSP - JavaDoc
public class MultiModalStation extends TransitEntity<FeedScopedId> implements StopCollection {
    private static final long serialVersionUID = 1L;

    private final FeedScopedId id;

    private final Collection<Station> childStations;

    private String name;

    private double lat;

    private double lon;

    // TODO OTP2 MMSP - move doc to getter
    /**
     * Public facing station code (short text or number)
     */
    private String code;

    // TODO OTP2 MMSP - move doc to getter
    /**
     * Additional information about the station (if needed)
     */
    private String description;

    // TODO OTP2 MMSP - move doc to getter
    /**
     * URL to a web page containing information about this particular station
     */
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
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
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
