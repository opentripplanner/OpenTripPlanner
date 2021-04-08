/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;


/**
 * This class is tha same as a GTFS Agency and Netex Authority.
 */
public final class Agency extends TransitEntity {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String timezone;

    private String url;

    private String lang;

    private String phone;

    private String fareUrl;

    private String brandingUrl;

    public Agency(FeedScopedId id, String name, String timezone) {
        super(id);
        this.name = name;
        this.timezone = timezone;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFareUrl() {
        return fareUrl;
    }

    public void setFareUrl(String fareUrl) {
        this.fareUrl = fareUrl;
    }

    public String getBrandingUrl() {
        return brandingUrl;
    }

    public void setBrandingUrl(String brandingUrl) {
        this.brandingUrl = brandingUrl;
    }

    public String toString() {
        return "<Agency " + getId() + ">";
    }
}
