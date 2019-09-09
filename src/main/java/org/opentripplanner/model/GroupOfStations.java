package org.opentripplanner.model;

import java.util.Set;

/**
 * A group consisting of Stations and MultiModalStations. This is used in NeTEx to link several
 * major StopPlaces into a hub. It has no equivalent in GTFS.
 */
public class GroupOfStations extends IdentityBean<FeedScopedId> {

        private static final long serialVersionUID = 1L;

        private FeedScopedId id;

        private String name;

        private String purposeOfGrouping;

        @Override public FeedScopedId getId() {
                return id;
        }

        @Override public void setId(FeedScopedId id) {
                this.id = id;
        }

        private Set<StopCollection> stations;

}
