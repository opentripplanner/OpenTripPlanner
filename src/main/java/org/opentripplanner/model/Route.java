/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Route extends IdentityBean<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private static final int MISSING_VALUE = -999;

    private FeedScopedId id;

    private Agency agency;

    private String shortName;

    private String longName;

    private int type;

    private String desc;

    private String url;

    private String color;

    private String textColor;

    @Deprecated private int routeBikesAllowed = 0;

    /**
     * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    private int bikesAllowed = 0;

    private int sortOrder = MISSING_VALUE;

    private String brandingUrl;

    private int eligibilityRestricted = MISSING_VALUE;

    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public Agency getAgency() {
        return agency;
    }

    public void setAgency(Agency agency) {
        this.agency = agency;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    @Deprecated
    public int getRouteBikesAllowed() {
        return routeBikesAllowed;
    }

    @Deprecated
    public void setRouteBikesAllowed(int routeBikesAllowed) {
        this.routeBikesAllowed = routeBikesAllowed;
    }

    /**
     * @return 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    public int getBikesAllowed() {
        return bikesAllowed;
    }

    /**
     * @param bikesAllowed 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes
     *          NOT allowed
     */
    public void setBikesAllowed(int bikesAllowed) {
        this.bikesAllowed = bikesAllowed;
    }

    public boolean isSortOrderSet() {
        return sortOrder != MISSING_VALUE;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getBrandingUrl() {
        return brandingUrl;
    }

    public void setBrandingUrl(String brandingUrl) {
        this.brandingUrl = brandingUrl;
    }

    public boolean hasEligibilityRestricted() {
        return eligibilityRestricted != MISSING_VALUE;
    }

    public int getEligibilityRestricted() {
        return eligibilityRestricted;
    }

    public void setEligibilityRestricted(int eligibilityRestricted) {
        this.eligibilityRestricted = eligibilityRestricted;
    }

    @Override
    public String toString() {
        return "<Route " + id + " " + shortName + ">";
    }
}
