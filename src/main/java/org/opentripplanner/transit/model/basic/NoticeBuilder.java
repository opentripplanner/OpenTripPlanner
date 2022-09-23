package org.opentripplanner.transit.model.basic;

import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class NoticeBuilder extends AbstractEntityBuilder<Notice, NoticeBuilder> {

  private String publicCode;
  private String text;

  NoticeBuilder(FeedScopedId id) {
    super(id);
  }

  NoticeBuilder(Notice original) {
    super(original);
    this.publicCode = original.publicCode();
    this.text = original.text();
  }

  public String publicCode() {
    return publicCode;
  }

  public NoticeBuilder withPublicCode(String publicCode) {
    this.publicCode = publicCode;
    return this;
  }

  public String text() {
    return text;
  }

  public NoticeBuilder withText(String text) {
    this.text = text;
    return this;
  }

  @Override
  protected Notice buildFromValues() {
    return new Notice(this);
  }
}
