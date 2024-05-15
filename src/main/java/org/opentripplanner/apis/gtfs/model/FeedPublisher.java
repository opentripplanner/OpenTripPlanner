package org.opentripplanner.apis.gtfs.model;

import java.util.Objects;

public record FeedPublisher(String name, String url) {
  public FeedPublisher {
    Objects.requireNonNull(name);
    Objects.requireNonNull(url);
  }
}
