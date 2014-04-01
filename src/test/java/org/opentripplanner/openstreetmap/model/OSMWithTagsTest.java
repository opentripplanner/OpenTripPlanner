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

package org.opentripplanner.openstreetmap.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class OSMWithTagsTest {

    @Test
    public void testHasTag() {
        OSMWithTags o = new OSMWithTags();
        assertFalse(o.hasTag("foo"));
        assertFalse(o.hasTag("FOO"));
        o.addTag("foo", "bar");
        
        assertTrue(o.hasTag("foo"));
        assertTrue(o.hasTag("FOO"));
    }

    @Test
    public void testGetTag() {
        OSMWithTags o = new OSMWithTags();
        assertNull(o.getTag("foo"));
        assertNull(o.getTag("FOO"));
        
        o.addTag("foo", "bar");
        assertEquals("bar", o.getTag("foo"));
        assertEquals("bar", o.getTag("FOO"));
    }
    
    @Test
    public void testIsFalse() {
        assertTrue(OSMWithTags.isFalse("no"));
        assertTrue(OSMWithTags.isFalse("0"));
        assertTrue(OSMWithTags.isFalse("false"));
        
        assertFalse(OSMWithTags.isFalse("yes"));
        assertFalse(OSMWithTags.isFalse("1"));
        assertFalse(OSMWithTags.isFalse("true"));
        assertFalse(OSMWithTags.isFalse("foo"));
        assertFalse(OSMWithTags.isFalse("bar"));
        assertFalse(OSMWithTags.isFalse("baz"));
    }
 
    @Test
    public void testIsTrue() {
        assertTrue(OSMWithTags.isTrue("yes"));
        assertTrue(OSMWithTags.isTrue("1"));
        assertTrue(OSMWithTags.isTrue("true"));
        
        assertFalse(OSMWithTags.isTrue("no"));
        assertFalse(OSMWithTags.isTrue("0"));
        assertFalse(OSMWithTags.isTrue("false"));
        assertFalse(OSMWithTags.isTrue("foo"));
        assertFalse(OSMWithTags.isTrue("bar"));
        assertFalse(OSMWithTags.isTrue("baz"));
    }
    
    @Test
    public void testIsTagFalseOrTrue() {
        OSMWithTags o = new OSMWithTags();
        assertFalse(o.isTagFalse("foo"));
        assertFalse(o.isTagFalse("FOO"));
        assertFalse(o.isTagTrue("foo"));
        assertFalse(o.isTagTrue("FOO"));
        
        o.addTag("foo", "true");
        assertFalse(o.isTagFalse("foo"));
        assertFalse(o.isTagFalse("FOO"));
        assertTrue(o.isTagTrue("foo"));
        assertTrue(o.isTagTrue("FOO"));
        
        o.addTag("foo", "no");
        assertTrue(o.isTagFalse("foo"));
        assertTrue(o.isTagFalse("FOO"));
        assertFalse(o.isTagTrue("foo"));
        assertFalse(o.isTagTrue("FOO"));
    }
    
    @Test
    public void testDoesAllowTagAccess() {
        OSMWithTags o = new OSMWithTags();
        assertFalse(o.doesTagAllowAccess("foo"));
        
        o.addTag("foo", "bar");
        assertFalse(o.doesTagAllowAccess("foo"));
        
        o.addTag("foo", "designated");
        assertTrue(o.doesTagAllowAccess("foo"));
        
        o.addTag("foo", "official");
        assertTrue(o.doesTagAllowAccess("foo"));
    }
    
    @Test
    public void testIsGeneralAccessDenied() {
        OSMWithTags o = new OSMWithTags();
        assertFalse(o.isGeneralAccessDenied());
        
        o.addTag("access", "something");
        assertFalse(o.isGeneralAccessDenied());
        
        o.addTag("access", "license");
        assertTrue(o.isGeneralAccessDenied());
        
        o.addTag("access", "no");
        assertTrue(o.isGeneralAccessDenied());
    }
    
    @Test
    public void testIsThroughTrafficExplicitlyDisallowed() {
        OSMWithTags o = new OSMWithTags();
        assertFalse(o.isThroughTrafficExplicitlyDisallowed());
        
        o.addTag("access", "something");
        assertFalse(o.isThroughTrafficExplicitlyDisallowed());
        
        o.addTag("access", "destination");
        assertTrue(o.isThroughTrafficExplicitlyDisallowed());

        o.addTag("access", "forestry");
        assertTrue(o.isThroughTrafficExplicitlyDisallowed());
        
        o.addTag("access", "private");
        assertTrue(o.isThroughTrafficExplicitlyDisallowed());
    }   
}
