/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Route extends TransitEntity {

    private static final long serialVersionUID = 1L;

    private static final int MISSING_VALUE = -999;

    private Agency agency;

    private Operator operator;

    private String shortName;

    private String longName;

    private int type;

    private TransitMode mode;

    private String desc;

    private String url;

    private String color;

    private String textColor;

    private BikeAccess bikesAllowed = BikeAccess.UNKNOWN;

    private int sortOrder = MISSING_VALUE;

    private String brandingUrl;

    private String flexibleLineType;

    public Route(FeedScopedId id) {
        super(id);
    }

    /**
     * The 'agency' property represent a GTFS Agency and NeTEx the Authority.
     * Note that Agency does NOT map 1-1 to Authority, it is rather a mix
     * between Authority and Operator.
     */
    public Agency getAgency() {
        return agency;
    }

    public void setAgency(Agency agency) {
        this.agency = agency;
    }

    /**
     * NeTEx Operator, not in use when importing GTFS files.
     */
    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
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

    public TransitMode getMode() {
        return mode;
    }

    public void setMode(TransitMode mode) {
        this.mode = mode;
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

    public BikeAccess getBikesAllowed() {
        return bikesAllowed;
    }

    public void setBikesAllowed(BikeAccess bikesAllowed) {
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

    /**
     * Pass-through information from NeTEx FlexibleLineType. This information is not used by OTP.
     */
    public String getFlexibleLineType() {
        return flexibleLineType;
    }

    public void setFlexibleLineType(String flexibleLineType) {
        this.flexibleLineType = flexibleLineType;
    }

    /** @return the route's short name, or the long name if the short name is null. */
    public String getName() {
        return  shortName != null ? shortName : longName;
    }

    @Override
    public String toString() {
        return "<Route " + getId() + " " + shortName + ">";
    }
}
