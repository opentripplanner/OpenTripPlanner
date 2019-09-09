package org.opentripplanner.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * A level above a monomodal StopPlace (station) in NeTEx. It has no equivalent in GTFS.
 */
public class MultiModalStation extends IdentityBean<FeedScopedId>  implements StopCollection {
        private static final long serialVersionUID = 1L;

        private FeedScopedId id;

        private String name;

        private double lat;

        private double lon;

        /**
         * Public facing station code (short text or number)
         */
        private String code;

        /**
         * Additional information about the station (if needed)
         */
        private String description;

        /**
         * URL to a web page containing information about this particular station
         */
        private String url;

        private TimeZone timezone;

        private Set<Station> childStations = new HashSet<>();

        public MultiModalStation() {}

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

        public TimeZone getTimezone() {
                return timezone;
        }

        public void setTimezone(TimeZone timezone) {
                this.timezone = timezone;
        }

        public Collection<Stop> getChildStops() {
                return this.childStations.stream()
                        .flatMap(s -> s.getChildStops().stream())
                        .collect(Collectors.toList());
        }

        public Collection<Station> getChildStations() {
                return childStations;
        }

        public void addChildStation(Station station) {
                this.childStations.add(station);
        }
}
