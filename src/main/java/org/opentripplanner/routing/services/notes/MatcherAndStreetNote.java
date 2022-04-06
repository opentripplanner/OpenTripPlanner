package org.opentripplanner.routing.services.notes;

import java.io.Serializable;
import org.opentripplanner.model.StreetNote;

/**
 * A container for a pair (note matcher, note).
 *
 * @author laurent
 */
public class MatcherAndStreetNote implements Serializable {

  private static final long serialVersionUID = 1L;

  public MatcherAndStreetNote(NoteMatcher matcher, StreetNote note) {
    this.matcher = matcher;
    this.note = note;
  }

  private final NoteMatcher matcher;

  private final StreetNote note;

  public NoteMatcher getMatcher() {
    return matcher;
  }

  public StreetNote getNote() {
    return note;
  }
}
