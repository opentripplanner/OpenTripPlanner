/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * @author laurent
 */
public class TestTemplateLibrary extends TestCase {

    public void testTemplate() {
        OSMWithTags osmTags = new OSMWithTags();
        osmTags.addTag("note", "Note EN");
        osmTags.addTag("description:fr", "Description FR");
        osmTags.addTag("wheelchair:description", "Wheelchair description EN");
        osmTags.addTag("wheelchair:description:fr", "Wheelchair description FR");

        assertEquals(null, TemplateLibrary.generate(null, osmTags));
        assertEquals("", TemplateLibrary.generate("", osmTags));
        assertEquals("Static text", TemplateLibrary.generate("Static text", osmTags));
        assertEquals("Note: Note EN", TemplateLibrary.generate("Note: {note}", osmTags));
        assertEquals("Inexistant: ",
                TemplateLibrary.generate("Inexistant: {foobar:description}", osmTags));
        assertEquals("Wheelchair note: Wheelchair description EN",
                TemplateLibrary.generate("Wheelchair note: {wheelchair:description}", osmTags));

        assertEquals(null, TemplateLibrary.generateI18N(null, osmTags));
        Map<String, String> expected = new HashMap<>();

        expected.put(null, "");
        assertEquals(expected, TemplateLibrary.generateI18N("", osmTags));

        expected.clear();
        expected.put(null, "Note: Note EN");
        assertEquals(expected, TemplateLibrary.generateI18N("Note: {note}", osmTags));

        expected.clear();
        expected.put(null, "Desc: Description FR");
        expected.put("fr", "Desc: Description FR");
        assertEquals(expected, TemplateLibrary.generateI18N("Desc: {description}", osmTags));

        expected.clear();
        expected.put(null, "Note: Note EN, Wheelchair description EN");
        expected.put("fr", "Note: Note EN, Wheelchair description FR");
        assertEquals(expected, TemplateLibrary.generateI18N(
                "Note: {note}, {wheelchair:description}", osmTags));

        expected.clear();
        expected.put(null, "Note: Note EN, Wheelchair description EN, ");
        expected.put("fr", "Note: Note EN, Wheelchair description FR, ");
        assertEquals(expected, TemplateLibrary.generateI18N(
                "Note: {note}, {wheelchair:description}, {foobar:description}", osmTags));

    }

}
