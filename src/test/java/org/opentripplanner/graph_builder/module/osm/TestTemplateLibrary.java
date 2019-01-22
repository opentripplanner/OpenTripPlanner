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
