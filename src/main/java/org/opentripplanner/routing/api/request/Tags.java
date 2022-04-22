package org.opentripplanner.routing.api.request;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * A collection of request tags. The intended use of tags are for cross-cutting concerns like
 * performance timing, debugging and logging. Currently, we only use tags for performance timing;
 * Hence only one getter method {@code #getTimingTags}.
 */
public class Tags implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /** We only need one set of tags since we support timingTags only */
  private final Set<String> tags;

  private Tags(Collection<String> tags) {
    this.tags = tags == null ? Set.of() : Set.copyOf(tags);
  }

  public static Tags of() {
    return new Tags(null);
  }

  public static Tags of(Collection<String> tags) {
    return new Tags(tags);
  }

  /** Currently all tags are used as Micrometer timing tags */
  public Set<String> getTimingTags() {
    return tags;
  }

  @Override
  public String toString() {
    return String.join(", ", tags);
  }
}
