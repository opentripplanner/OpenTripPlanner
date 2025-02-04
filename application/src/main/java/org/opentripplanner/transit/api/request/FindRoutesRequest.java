package org.opentripplanner.transit.api.request;

import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;

/**
 * A request for finding {@link Route}s.
 * </p>
 * This request is used to retrieve Routes that match the provided filter values.
 * At least one filter value must be provided.
 */
public class FindRoutesRequest {

  private final boolean flexibleOnly;
  private final String longName;
  private final String shortName;
  private final FilterValues<String> shortNames;
  private final FilterValues<TransitMode> transitModes;
  private final FilterValues<String> agencyIds;

  protected FindRoutesRequest(
    boolean flexibleOnly,
    String longName,
    String shortName,
    FilterValues<String> shortNames,
    FilterValues<TransitMode> transitModes,
    FilterValues<String> agencyIds
  ) {
    this.flexibleOnly = flexibleOnly;
    this.longName = longName;
    this.shortName = shortName;
    this.shortNames = shortNames;
    this.transitModes = transitModes;
    this.agencyIds = agencyIds;
  }

  public static FindRoutesRequestBuilder of() {
    return new FindRoutesRequestBuilder();
  }

  public boolean flexibleOnly() {
    return flexibleOnly;
  }

  public String longName() {
    return longName;
  }

  public String shortName() {
    return shortName;
  }

  public FilterValues<String> shortNames() {
    return shortNames;
  }

  public FilterValues<TransitMode> transitModes() {
    return transitModes;
  }

  public FilterValues<String> agencies() {
    return agencyIds;
  }
}
