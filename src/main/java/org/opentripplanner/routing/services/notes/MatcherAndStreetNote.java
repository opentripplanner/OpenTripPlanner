package org.opentripplanner.routing.services.notes;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.model.StreetNote;

/**
 * A container for a pair (note matcher, note).
 *
 * @author laurent
 */
public class MatcherAndStreetNote implements Serializable {

  private static final long serialVersionUID = 1L;
  private final NoteMatcher matcher;
  private final StreetNote note;

  public MatcherAndStreetNote(NoteMatcher matcher, StreetNote note) {
    this.matcher = matcher;
    this.note = note;
  }

  public NoteMatcher getMatcher() {
    return matcher;
  }

  public StreetNote getNote() {
    return note;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MatcherAndStreetNote that = (MatcherAndStreetNote) o;
    return matcher.equals(that.matcher) && note.equals(that.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(matcher, note);
  }
}
