/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.io.Serializable;
import java.time.LocalDate;

public final class FeedInfo implements Serializable {

  private final String id;

  private final String publisherName;

  private final String publisherUrl;

  private final String lang;

  private final LocalDate startDate;

  private final LocalDate endDate;

  private final String version;

  public FeedInfo(
    String id,
    String publisherName,
    String publisherUrl,
    String lang,
    LocalDate startDate,
    LocalDate endDate,
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

  public static FeedInfo dummyForTest(String id) {
    return new FeedInfo(id, "publisher", "www.z.org", "en", null, null, null);
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

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getVersion() {
    return version;
  }

  public String getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeedInfo feedInfo = (FeedInfo) o;
    return id.equals(feedInfo.id);
  }

  @Override
  public String toString() {
    return "FeedInfo{" + getId() + '}';
  }
}
