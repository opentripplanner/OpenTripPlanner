package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/*
 * A request for trips.
 *
 * This request is used to retrieve Trips that match the provided criteria.
 */
public class TripRequest {

  private final List<FeedScopedId> authorities;
  private final List<FeedScopedId> lines;
  private final List<String> privateCodes;
  private final List<LocalDate> activeDates;

  protected TripRequest(
    List<FeedScopedId> authorities,
    List<FeedScopedId> lines,
    List<String> privateCodes,
    List<LocalDate> activeDates
  ) {
    this.authorities = ListUtils.nullSafeImmutableList(authorities);
    this.lines = ListUtils.nullSafeImmutableList(lines);
    this.privateCodes = ListUtils.nullSafeImmutableList(privateCodes);
    this.activeDates = ListUtils.nullSafeImmutableList(activeDates);
  }

  public static TripRequestBuilder of() {
    return new TripRequestBuilder();
  }

  public List<FeedScopedId> authorities() {
    return authorities;
  }

  public List<FeedScopedId> lines() {
    return lines;
  }

  public List<String> privateCodes() {
    return privateCodes;
  }

  public List<LocalDate> activeDates() {
    return activeDates;
  }
}
