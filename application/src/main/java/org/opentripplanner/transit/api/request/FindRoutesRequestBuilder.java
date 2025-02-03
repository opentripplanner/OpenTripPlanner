package org.opentripplanner.transit.api.request;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;

public class FindRoutesRequestBuilder {

  private boolean flexibleOnly;
  private String longName;
  private String shortName;
  private FilterValues<String> shortNames = FilterValues.ofEmptyIsEverything(
    "shortNames",
    List.of()
  );
  private FilterValues<TransitMode> transitModes = FilterValues.ofEmptyIsEverything(
    "transitModes",
    List.of()
  );
  private FilterValues<String> agencies = FilterValues.ofEmptyIsEverything("agencies", List.of());

  protected FindRoutesRequestBuilder() {}

  public FindRoutesRequestBuilder withAgencies(FilterValues<String> agencies) {
    this.agencies = agencies;
    return this;
  }

  public FindRoutesRequestBuilder withFlexibleOnly(boolean flexibleOnly) {
    this.flexibleOnly = flexibleOnly;
    return this;
  }

  public FindRoutesRequestBuilder withLongName(@Nullable String longName) {
    this.longName = longName;
    return this;
  }

  public FindRoutesRequestBuilder withShortName(@Nullable String shortName) {
    this.shortName = shortName;
    return this;
  }

  public FindRoutesRequestBuilder withShortNames(FilterValues<String> shortNames) {
    this.shortNames = shortNames;
    return this;
  }

  public FindRoutesRequestBuilder withTransitModes(FilterValues<TransitMode> transitModes) {
    this.transitModes = transitModes;
    return this;
  }

  public FindRoutesRequest build() {
    return new FindRoutesRequest(
      flexibleOnly,
      longName,
      shortName,
      shortNames,
      transitModes,
      agencies
    );
  }
}
