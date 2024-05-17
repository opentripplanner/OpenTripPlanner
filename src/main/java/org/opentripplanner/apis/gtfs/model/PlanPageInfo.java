package org.opentripplanner.apis.gtfs.model;

import graphql.relay.ConnectionCursor;
import java.time.Duration;
import java.util.Objects;

public class PlanPageInfo {

  private final ConnectionCursor startCursor;
  private final ConnectionCursor endCursor;
  private final boolean hasPreviousPage;
  private final boolean hasNextPage;
  private final Duration searchWindowUsed;

  public PlanPageInfo(
    ConnectionCursor startCursor,
    ConnectionCursor endCursor,
    boolean hasPreviousPage,
    boolean hasNextPage,
    Duration searchWindowUsed
  ) {
    this.startCursor = startCursor;
    this.endCursor = endCursor;
    this.hasPreviousPage = hasPreviousPage;
    this.hasNextPage = hasNextPage;
    this.searchWindowUsed = searchWindowUsed;
  }

  public ConnectionCursor startCursor() {
    return startCursor;
  }

  public ConnectionCursor endCursor() {
    return endCursor;
  }

  public boolean hasPreviousPage() {
    return hasPreviousPage;
  }

  public boolean hasNextPage() {
    return hasNextPage;
  }

  public Duration searchWindowUsed() {
    return searchWindowUsed;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      startCursor != null ? startCursor.getValue() : null,
      endCursor != null ? endCursor.getValue() : null,
      hasPreviousPage,
      hasNextPage,
      searchWindowUsed
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlanPageInfo that = (PlanPageInfo) o;
    return (
      (
        (startCursor == null && that.startCursor == null) ||
        (
          startCursor != null &&
          that.startCursor != null &&
          Objects.equals(startCursor.getValue(), that.startCursor.getValue())
        )
      ) &&
      (
        (endCursor == null && that.endCursor == null) ||
        (
          endCursor != null &&
          that.endCursor != null &&
          Objects.equals(endCursor.getValue(), that.endCursor.getValue())
        )
      ) &&
      Objects.equals(searchWindowUsed, that.searchWindowUsed) &&
      hasPreviousPage == that.hasPreviousPage &&
      hasNextPage == that.hasNextPage
    );
  }
}
