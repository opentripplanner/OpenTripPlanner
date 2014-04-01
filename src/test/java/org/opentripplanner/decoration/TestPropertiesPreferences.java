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

package org.opentripplanner.decoration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.prefs.Preferences;

import org.opentripplanner.updater.PropertiesPreferences;

import junit.framework.TestCase;

/*
 */
public class TestPropertiesPreferences extends TestCase {

    public void testPropertiesPreference() throws Exception {

        Preferences prefs1 = new PropertiesPreferences(makeInput("pe=é\npa=1\npb=xyz\n"));
        assertEquals(1, prefs1.getLong("pa", 0));
        assertEquals("xyz", prefs1.get("pb", ""));
        assertEquals(-1, prefs1.getLong("pd", -1));
        assertEquals("é", prefs1.get("pe", null));

        Preferences prefs2 = new PropertiesPreferences(
                makeInput("pa=1\na.pb=2\na.b.pc=3\na.b2.pd=uvw\nb.pe=42"));
        String[] children = prefs2.childrenNames();
        Arrays.sort(children);
        assertEquals("a", children[0]);
        assertEquals("b", children[1]);
        assertEquals(1, prefs2.getLong("pa", 0));
        assertEquals(2, prefs2.node("a").getLong("pb", 0));
        assertEquals(2, prefs2.node("/a").getLong("pb", 0));
        assertEquals(3, prefs2.node("/a/b").getLong("pc", 0));
        Preferences prefs2a = prefs2.node("/a");
        assertEquals(2, prefs2a.getLong("pb", 0));
        assertEquals(-1, prefs2.node("/a/c").getLong("px", -1));
        assertEquals(null, prefs2.node("/a/c").get("py", null));
        Preferences prefs2ab2 = prefs2.node("/a/b2");
        assertEquals("uvw", prefs2ab2.get("pd", null));
    }

    private InputStream makeInput(String data) {
        return new ByteArrayInputStream(data.getBytes(Charset.forName("ISO-8859-1")));
    }
}
