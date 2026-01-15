package org.opentripplanner.service.streetdetails.model;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents level information for a vertex. The {@link Level} is nullable because sometimes only
 * the vertical order is known. The osmNodeId is necessary to reliably map the level to the correct
 * vertex.
 */
public class VertexLevelInfo implements Serializable {

  @Nullable
  private final Level level;

  private final long osmNodeId;

  public VertexLevelInfo(@Nullable Level level, long osmNodeId) {
    this.level = level;
    this.osmNodeId = osmNodeId;
  }

  @Nullable
  public Level level() {
    return this.level;
  }

  public long osmNodeId() {
    return this.osmNodeId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null || o.getClass() != getClass()) {
      return false;
    }
    VertexLevelInfo that = (VertexLevelInfo) o;
    return Objects.equals(this.level, that.level) && osmNodeId == that.osmNodeId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(level, osmNodeId);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
      .addObj("level", level)
      .addNum("osmNodeId", osmNodeId)
      .toString();
  }
}
