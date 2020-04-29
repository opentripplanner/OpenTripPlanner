/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;


/**
 * This class is tha same as a GTFS Agency and Netex Authority.
 */
public final class Agency extends TransitEntity<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private FeedScopedId id;

    private String name;

    private String url;

    private String timezone;

    private String lang;

    private String phone;

    private String fareUrl;

    private String brandingUrl;

    public Agency() {
    }

    public Agency(Agency a) {
        this.id = a.id;
        this.name = a.name;
        this.url = a.url;
        this.timezone = a.timezone;
        this.lang = a.lang;
        this.phone = a.phone;
        this.brandingUrl = a.brandingUrl;
    }

    @Override
    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setTimezone(String timezone) {
        this.timezone = timezone;
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
        return "<Agency " + this.id + ">";
    }
}
