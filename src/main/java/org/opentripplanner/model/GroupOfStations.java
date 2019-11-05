package org.opentripplanner.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupOfStations extends TransitEntity<FeedScopedId> implements StopCollection {
        private static final long serialVersionUID = 1L;

        private FeedScopedId id;

        private String name;

        // TODO Map from NeTEx
        private PurposeOfGrouping purposeOfGrouping;

        private double lat;

        private double lon;

        private Set<StopCollection> childStations = new HashSet<>();

        public GroupOfStations() {}

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

        public Collection<Stop> getChildStops() {
                return this.childStations.stream()
                        .flatMap(s -> s.getChildStops().stream())
                        .collect(Collectors.toList());
        }

        public Collection<StopCollection> getChildStations() {
                return this.childStations;
        }

        public void addChildStation(StopCollection station) {
                this.childStations.add(station);
        }

        /**
         * Categorization for the grouping
         */
        public PurposeOfGrouping getPurposeOfGrouping() {
                return purposeOfGrouping;
        }

        public void setPurposeOfGrouping(PurposeOfGrouping purposeOfGrouping) {
                this.purposeOfGrouping = purposeOfGrouping;
        }

        @Override
        public String toString() {
                return "<GroupOfStations " + this.id + ">";
        }

        public enum PurposeOfGrouping {
                /** Grouping of prominent stop places with a superordinate Place, for example, a
                 * town, or city centre */
                GENERALIZATION,
                /**
                 * Stop places in proximity to each other which have a natural geospatial- or
                 * public transport related relationship.
                 */
                CLUSTER; }
}