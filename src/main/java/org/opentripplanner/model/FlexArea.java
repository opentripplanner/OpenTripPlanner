/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public class FlexArea extends IdentityBean<FeedScopedId> {
    private static final long serialVersionUID = 1L;

    private FeedScopedId id;

    private String wkt;

    public FlexArea() {

    }

    public FlexArea(FlexArea a) {
        this.id = a.id;
        this.wkt = a.wkt;
    }


    public String getAreaId() {
        return id.getId();
    }

    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId areaId) {
        this.id = areaId;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

}
