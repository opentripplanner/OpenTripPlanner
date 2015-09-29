/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import java.util.Map;
import java.util.regex.Pattern;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.services.notes.NoteMatcher;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.TranslatedString;
import org.opentripplanner.util.i18n.T;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

//Currently unused since notes are disabled in DefaultWayPropertySetSource
public class NoteProperties {

    private static final Pattern patternMatcher = Pattern.compile("\\{(.*?)\\}");

    public String notePattern;

    private T translatablePattern;

    public NoteMatcher noteMatcher;

    /**
     * Used only if String is "{tag}" for other uses use {@link #NoteProperties(T, NoteMatcher)} constructor
     *
     * Values in {} are tag values. Translations for this values are also extracted from OSM if they exist.
     * @param notePattern "{note}", "{notes}", "{wheelchair:description}
     * @param noteMatcher When to match a note
     */
    public NoteProperties(String notePattern, NoteMatcher noteMatcher) {
        this.notePattern = notePattern;
        this.noteMatcher = noteMatcher;
    }

    public NoteProperties(T patternKey, NoteMatcher matcher) {
        this.notePattern = patternKey.msgid;
        this.noteMatcher = matcher;
        this.translatablePattern = patternKey;
    }

    /**
     *
     * Note can be generated with  pattern "{tagname}" where all tagname:languages
     * are also saved as translations. And correct translation is returned on usage if it exits.
     *
     * Or pattern can be "text ", "text {tagname}" where gettext translation with
     * {@link LocalizedString} is used to get the translations of text.
     * And only default value of tagname is used. No translations are supported currently.
     * @param way From which tag values are read
     * @return
     */
    public T2<Alert, NoteMatcher> generateNote(OSMWithTags way) {
        Alert note = new Alert();
        //TODO: this could probably be made without patternMatch for {} since all notes (at least currently) have {note} as notePattern
        if (patternMatcher.matcher(notePattern).matches()) {
            //This gets language -> translation of notePattern and all tags (which can have translations name:en for example)
            Map<String, String> noteText = TemplateLibrary.generateI18N(notePattern, way);
            note.alertHeaderText = TranslatedString.getI18NString(noteText);
        } else {
            if (translatablePattern != null) {
                // This uses new Gettext translation
                note.alertHeaderText = new LocalizedString(translatablePattern, way);
            } else {
                //This is used for Notepatterns like "text {tag}" which shouldn't happen
                throw new NotImplementedException();
                //"Note pattern: " + notePattern + " should use Gettext translation instead of text");

            }
        }
        return new T2<>(note, noteMatcher);
    }
}
