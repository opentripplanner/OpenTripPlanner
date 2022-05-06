package org.opentripplanner.transit.model.organization;

import org.opentripplanner.transit.model.basic.FeedScopedId;

public class OperatorBuilder {

  private FeedScopedId id;

  private String name;

  private String url;

  private String phone;

  OperatorBuilder(FeedScopedId id) {
    this.id = id;
  }

  OperatorBuilder(Operator operator) {
    this(operator.getId());
    this.name = operator.getName();
    this.url = operator.getUrl();
    this.phone = operator.getPhone();
  }

  public Operator build() {
    return new Operator(this);
  }

  public FeedScopedId getId() {
    return id;
  }

  public OperatorBuilder setId(FeedScopedId id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public OperatorBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public OperatorBuilder setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getPhone() {
    return phone;
  }

  public OperatorBuilder setPhone(String phone) {
    this.phone = phone;
    return this;
  }
}
