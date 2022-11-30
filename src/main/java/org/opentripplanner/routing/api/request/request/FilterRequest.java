package org.opentripplanner.routing.api.request.request;

public class FilterRequest {
  // TODO: 2022-11-30 clone

  private SelectRequest include = new SelectRequest();
  private SelectRequest exclude = new SelectRequest();

  public SelectRequest getInclude() {
    return include;
  }

  public void setInclude(SelectRequest include) {
    this.include = include;
  }

  public SelectRequest getExclude() {
    return exclude;
  }

  public void setExclude(SelectRequest exclude) {
    this.exclude = exclude;
  }
}
