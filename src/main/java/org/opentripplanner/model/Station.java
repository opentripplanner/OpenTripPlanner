package org.opentripplanner.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * A grouping of stops in GTFS or the lowest level grouping in NeTEx. It can be a train station, a
 * bus terminal, or a bus station (with a bus stop at each side of the road). Equivalent to GTFS
 * stop location type 1 or NeTEx monomodal StopPlace.
 */
public class Station extends TransitEntity<FeedScopedId> implements StopCollection {
        private static final long serialVersionUID = 1L;

        private FeedScopedId id;

        private String name;

        private WgsCoordinate coordinate;

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

        private Set<Stop> childStops = new HashSet<>();

        public Station() {}

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
                return coordinate.latitude();
        }

        public WgsCoordinate getCoordinate() {
                return coordinate;
        }

        public void setCoordinate(WgsCoordinate coordinate) {
                this.coordinate = coordinate;
        }

        public double getLon() {
                return coordinate.longitude();
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
                return childStops;
        }

        public void addChildStop(Stop stop) {
                this.childStops.add(stop);
        }

        @Override
        public String toString() {
                return "<Station " + this.id + ">";
        }
}
