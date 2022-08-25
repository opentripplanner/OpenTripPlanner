package org.opentripplanner.transit.model.basic;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This is an element that originates from the NeTEx specification and is described as "Text-based
 * notification describing circumstances which cannot be modelled as structured data." Any NeTEx
 * element can have a notice attached, although not all are supported in OTP.
 */
public class Notice extends AbstractTransitEntity<Notice, NoticeBuilder> {

  private final String text;
  private final String publicCode;

  Notice(NoticeBuilder builder) {
    super(builder.getId());
    this.publicCode = builder.publicCode();
    this.text = builder.text();
  }

  public static NoticeBuilder of(FeedScopedId id) {
    return new NoticeBuilder(id);
  }

  public String text() {
    return text;
  }

  public String publicCode() {
    return publicCode;
  }

  @Override
  public boolean sameAs(@Nonnull Notice other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(publicCode, other.publicCode) &&
      Objects.equals(text, other.text)
    );
  }

  @Override
  @Nonnull
  public NoticeBuilder copy() {
    return new NoticeBuilder(this);
  }
}
