package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.services.notes.NoteMatcher;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.TranslatedString;

import java.util.Map;
import java.util.regex.Pattern;

//Currently unused since notes are disabled in DefaultWayPropertySetSource
public class NoteProperties {

    private static final Pattern patternMatcher = Pattern.compile("\\{(.*?)\\}");

    public String notePattern;

    public NoteMatcher noteMatcher;

    public NoteProperties(String notePattern, NoteMatcher noteMatcher) {
        this.notePattern = notePattern;
        this.noteMatcher = noteMatcher;
    }

    public T2<StreetNote, NoteMatcher> generateNote(OSMWithTags way) {
        I18NString text;
        //TODO: this could probably be made without patternMatch for {} since all notes (at least currently) have {note} as notePattern
        if (patternMatcher.matcher(notePattern).matches()) {
            //This gets language -> translation of notePattern and all tags (which can have translations name:en for example)
            Map<String, String> noteText = TemplateLibrary.generateI18N(notePattern, way);
            text = TranslatedString.getI18NString(noteText);
        } else {
            text = new LocalizedString(notePattern, way);
        }
        StreetNote note = new StreetNote(text);

        return new T2<>(note, noteMatcher);
    }
}
