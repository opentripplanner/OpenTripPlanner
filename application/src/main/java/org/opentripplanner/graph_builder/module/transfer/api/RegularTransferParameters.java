package org.opentripplanner.graph_builder.module.transfer.api;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.routing.api.request.RouteRequest;

public final class RegularTransferParameters {

  private final Duration maxDuration;
  private final List<RouteRequest> requests;

  public static RegularTransferParameters DEFAULT = new RegularTransferParameters();

  private RegularTransferParameters() {
    this.maxDuration = Duration.ofMinutes(30);
    this.requests = List.of();
  }

  public RegularTransferParameters(Duration maxDuration, List<RouteRequest> requests) {
    this.maxDuration = Objects.requireNonNull(maxDuration);
    this.requests = List.copyOf(requests);
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Duration maxDuration() {
    return maxDuration;
  }

  public List<RouteRequest> requests() {
    return requests;
  }

  public static class Builder {

    private Duration maxDuration;
    private List<RouteRequest> requests;

    private Builder(RegularTransferParameters original) {
      this.maxDuration = original.maxDuration;
      this.requests = original.requests;
    }

    public Builder withMaxDuration(Duration maxDuration) {
      this.maxDuration = maxDuration;
      return this;
    }

    public Builder withRequests(List<RouteRequest> requests) {
      this.requests = requests;
      return this;
    }

    public RegularTransferParameters build() {
      return new RegularTransferParameters(maxDuration, requests);
    }
  }
}
