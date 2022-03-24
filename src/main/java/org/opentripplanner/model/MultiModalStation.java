package org.opentripplanner.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.opentripplanner.util.I18NString;


/**
 * The next level grouping of stops above Station. Equivalent to NeTEx multimodal StopPlace. As
 * a Station (NeTEx StopPlace) only supports a single transit mode, you are required to group
 * several Stations together using a MultiModalStation in order to support several modes. This
 * entity is not part of GTFS.
 */
public class MultiModalStation extends TransitEntity implements StopCollection {
    private static final long serialVersionUID = 1L;

    private final Collection<Station> childStations;

    private I18NString name;

    private WgsCoordinate coordinate;

    private String code;

    private String description;

    private I18NString url;

    /**
     * Create a new multi modal station with the given list of child stations.
     */
    public MultiModalStation(FeedScopedId id, Collection<Station> children) {
        super(id);
        this.childStations = Collections.unmodifiableCollection(new ArrayList<>(children));
    }

    public I18NString getName() {
        return name;
    }

    public void setName(I18NString name) {
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
    public I18NString getUrl() {
        return url;
    }

    public void setUrl(I18NString url) {
        this.url = url;
    }

    public Collection<StopLocation> getChildStops() {
        return this.childStations.stream()
                .flatMap(s -> s.getChildStops().stream())
                .collect(Collectors.toUnmodifiableList());
    }

    public Collection<Station> getChildStations() {
        return this.childStations;
    }

    @Override
    public String toString() {
        return "<MultiModal station " + getId() + ">";
    }
}
