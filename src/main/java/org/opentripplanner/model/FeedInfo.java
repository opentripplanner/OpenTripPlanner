/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

public final class FeedInfo extends IdentityBean<Integer> {

    private static final long serialVersionUID = 1L;

    private int id;

    private String publisherName;

    private String publisherUrl;

    private String lang;

    private ServiceDate startDate;

    private ServiceDate endDate;

    private String version;

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public String getPublisherUrl() {
        return publisherUrl;
    }

    public void setPublisherUrl(String publisherUrl) {
        this.publisherUrl = publisherUrl;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public ServiceDate getStartDate() {
        return startDate;
    }

    public void setStartDate(ServiceDate startDate) {
        this.startDate = startDate;
    }

    public ServiceDate getEndDate() {
        return endDate;
    }

    public void setEndDate(ServiceDate endDate) {
        this.endDate = endDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String toString() {
        return "<FeedInfo " + getId() + ">";
    }

}
