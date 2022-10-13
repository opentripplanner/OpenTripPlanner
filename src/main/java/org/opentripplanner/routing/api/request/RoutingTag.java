package org.opentripplanner.routing.api.request;

import java.io.Serializable;
import java.util.Objects;

/**
 * A collection of request tags. The intended use of tags are for cross-cutting concerns like
 * performance timing, debugging and logging. Currently, we only use tags for performance timing;
 * Hence only one getter method {@code #getTimingTags}.
 */
public class RoutingTag implements Serializable {

  /**
   * The micrometer standard group tags by category to provide filtering on a pr. group bases.
   */
  public enum Category {
    TestCaseCategory(true),
    TestCaseSample(true);

    Category(boolean micrometerTiming) {
      this.micrometerTiming = micrometerTiming;
    }

    /**
     * Flag to indicate that this tag should be reported to as a Micrometer Timing tag.
     */
    public final boolean micrometerTiming;
  }

  /** Tags are categorized in groups, this allow  */
  private final Category category;

  /** We only need one set of tags since we support timingTags only */
  private final String tag;

  private RoutingTag(Category category, String tag) {
    this.category = category;
    this.tag = tag;
  }

  public static RoutingTag testCaseCategory(String tag) {
    return new RoutingTag(Category.TestCaseCategory, tag);
  }

  public static RoutingTag testCaseSample(String tag) {
    return new RoutingTag(Category.TestCaseSample, tag);
  }

  public Category getCategory() {
    return category;
  }

  public String getTag() {
    return tag;
  }

  public boolean includeInMicrometerTiming() {
    return category.micrometerTiming;
  }

  @Override
  public String toString() {
    return category + ": " + tag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RoutingTag that = (RoutingTag) o;
    return category == that.category && Objects.equals(tag, that.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, tag);
  }
}
