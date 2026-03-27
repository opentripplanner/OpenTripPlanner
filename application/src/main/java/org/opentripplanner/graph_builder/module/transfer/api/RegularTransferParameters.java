package org.opentripplanner.graph_builder.module.transfer.api;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;

public final class RegularTransferParameters {

  private final Duration maxDuration;
  private final List<FeedScopedId> includeStops;
  private final Map<StreetMode, TransferParametersForMode> parametersForMode;
  private final List<RouteRequest> requests;

  public static RegularTransferParameters DEFAULT = new RegularTransferParameters();

  private RegularTransferParameters() {
    this.maxDuration = Duration.ofMinutes(30);
    this.includeStops = List.of();
    this.parametersForMode = Map.of();
    this.requests = List.of();
  }

  public RegularTransferParameters(
    Duration maxDuration,
    List<FeedScopedId> includeStops,
    Map<StreetMode, TransferParametersForMode> parametersForMode,
    List<RouteRequest> requests
  ) {
    this.maxDuration = Objects.requireNonNull(maxDuration);
    this.includeStops = List.copyOf(includeStops);
    this.parametersForMode = Map.copyOf(parametersForMode);
    this.requests = List.copyOf(requests);
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Duration maxDuration() {
    return maxDuration;
  }

  public List<FeedScopedId> includeStops() {
    return includeStops;
  }

  public Map<StreetMode, TransferParametersForMode> parametersForMode() {
    return parametersForMode;
  }

  public List<RouteRequest> requests() {
    return requests;
  }

  public static class Builder {

    private Duration maxDuration;
    private List<FeedScopedId> includeStops;
    private Map<StreetMode, TransferParametersForMode> parametersForMode = new HashMap<>();
    private List<RouteRequest> requests;

    private Builder(RegularTransferParameters original) {
      this.maxDuration = original.maxDuration;
      this.includeStops = original.includeStops;
      this.parametersForMode.putAll(original.parametersForMode);
      this.requests = original.requests;
    }

    public Builder withMaxDuration(Duration maxDuration) {
      this.maxDuration = maxDuration;
      return this;
    }

    public Builder withIncludeStops(List<FeedScopedId> includeStops) {
      this.includeStops = includeStops;
      return this;
    }

    /// Note! This method replaces/overwrites any existing content
    public Builder withParametersForMode(Map<StreetMode, TransferParametersForMode> map) {
      this.parametersForMode.clear();
      this.parametersForMode.putAll(map);
      return this;
    }

    /// This method add values to the existing map and leave values for other modes as is.
    public Builder addParametersForMode(StreetMode mode, TransferParametersForMode value) {
      this.parametersForMode.put(mode, value);
      return this;
    }

    public Builder withRequests(List<RouteRequest> requests) {
      this.requests = requests;
      return this;
    }

    public RegularTransferParameters build() {
      return new RegularTransferParameters(maxDuration, includeStops, parametersForMode, requests);
    }
  }
}
