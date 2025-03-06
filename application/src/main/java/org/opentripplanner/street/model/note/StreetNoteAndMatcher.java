package org.opentripplanner.street.model.note;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A container for a pair (note and note matcher).
 *
 * @author laurent
 */
@SuppressWarnings("ClassCanBeRecord")
public class StreetNoteAndMatcher implements Serializable {

  private final StreetNote note;
  private final StreetNoteMatcher matcher;

  public StreetNoteAndMatcher(StreetNote note, StreetNoteMatcher matcher) {
    this.matcher = matcher;
    this.note = note;
  }

  public StreetNoteMatcher matcher() {
    return matcher;
  }

  public StreetNote note() {
    return note;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StreetNoteAndMatcher that = (StreetNoteAndMatcher) o;
    return note.equals(that.note) && matcher.equals(that.matcher);
  }

  @Override
  public int hashCode() {
    return Objects.hash(note, matcher);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(StreetNoteAndMatcher.class)
      .addObj("note", note)
      .addObj("matcher", matcher)
      .toString();
  }
}
