package org.opentripplanner.osm.wayproperty;

import java.util.Map;
import java.util.regex.Pattern;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;
import org.opentripplanner.street.model.note.StreetNoteMatcher;

public class NoteProperties {

  private static final Pattern patternMatcher = Pattern.compile("\\{(.*?)}");

  private final String notePattern;

  private final StreetNoteMatcher noteMatcher;

  public NoteProperties(String notePattern, StreetNoteMatcher noteMatcher) {
    this.notePattern = notePattern;
    this.noteMatcher = noteMatcher;
  }

  public StreetNoteAndMatcher generateNote(OsmEntity way) {
    I18NString text;
    //TODO: this could probably be made without patternMatch for {} since all notes (at least currently) have {note} as notePattern
    if (patternMatcher.matcher(notePattern).matches()) {
      //This gets language -> translation of notePattern and all tags (which can have translations name:en for example)
      Map<String, String> noteText = way.generateI18NForPattern(notePattern);
      text = TranslatedString.getI18NString(noteText, true, false);
    } else {
      text = LocalizedStringMapper.getInstance().map(notePattern, way);
    }
    StreetNote note = new StreetNote(text);

    return new StreetNoteAndMatcher(note, noteMatcher);
  }
}
