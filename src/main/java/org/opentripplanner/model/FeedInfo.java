/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

import java.io.Serializable;

public final class FeedInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;

    private final String publisherName;

    private final String publisherUrl;

    private final String lang;

    private final ServiceDate startDate;

    private final ServiceDate endDate;

    private final String version;

    public FeedInfo(
        String id,
        String publisherName,
        String publisherUrl,
        String lang,
        ServiceDate startDate,
        ServiceDate endDate,
        String version
    ) {
        this.id = id;
        this.publisherName = publisherName;
        this.publisherUrl = publisherUrl;
        this.lang = lang;
        this.startDate = startDate;
        this.endDate = endDate;
        this.version = version;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public String getPublisherUrl() {
        return publisherUrl;
    }

    public String getLang() {
        return lang;
    }

    public ServiceDate getStartDate() {
        return startDate;
    }

    public ServiceDate getEndDate() {
        return endDate;
    }

    public String getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "<FeedInfo " + getId() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        FeedInfo feedInfo = (FeedInfo) o;
        return id.equals(feedInfo.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static FeedInfo dummyForTest(String id) {
        return new FeedInfo(
            id,
            "publisher",
            "www.z.org",
            "en",
            null,
            null,
            null
        );
    }
}
