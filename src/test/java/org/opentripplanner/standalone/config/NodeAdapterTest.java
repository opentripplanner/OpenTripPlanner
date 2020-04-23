package org.opentripplanner.standalone.config;

import org.junit.Test;
import org.mockito.Mockito;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.standalone.config.JsonSupport.newNodeAdapterForTest;

public class NodeAdapterTest {
    private enum AnEnum { A, B, C }

    @Test
    public void testAsRawNode() {
        NodeAdapter subject  = newNodeAdapterForTest("{ foo : 'bar' }");
        assertFalse(subject.asRawNode("anObject").has("withText"));
    }
    @Test
    public void isEmpty() {
        NodeAdapter subject  = newNodeAdapterForTest("");
        assertTrue(subject.path("alf").isEmpty());

        subject  = newNodeAdapterForTest("{}");
        assertTrue(subject.path("alf").isEmpty());
        assertTrue(subject.path("alfa").path("bet").isEmpty());
    }

    @Test
    public void path() {
        NodeAdapter subject  = newNodeAdapterForTest("{ foo : 'bar' }");
        assertFalse(subject.path("foo").isEmpty());
        assertTrue(subject.path("missingObject").isEmpty());
    }

    @Test
    public void asBoolean() {
        NodeAdapter subject  = newNodeAdapterForTest("{ aBoolean : true }");
        assertTrue(subject.asBoolean("aBoolean", false));
        assertFalse(subject.asBoolean("missingField", false));
    }

    @Test
    public void asDouble() {
        NodeAdapter subject  = newNodeAdapterForTest("{ aDouble : 7.0 }");
        assertEquals(7.0, subject.asDouble("aDouble", -1d), 0.01);
        assertEquals(7.0, subject.asDouble("aDouble"), 0.01);
        assertEquals(-1d, subject.asDouble("missingField", -1d), 00.1);
    }

    @Test
    public void asDoubles() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key : [ 2.0, 3.0, 5.0 ] }");
        assertEquals(List.of(2d, 3d, 5d), subject.asDoubles("key", null));
    }

    @Test
    public void asInt() {
        NodeAdapter subject  = newNodeAdapterForTest("{ aInt : 5 }");
        assertEquals(5, subject.asInt("aInt", -1));
        assertEquals(-1, subject.asInt("missingField", -1));
    }

    @Test
    public void asLong() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key : 5 }");
        assertEquals(5, subject.asLong("key", -1));
        assertEquals(-1, subject.asLong("missingField", -1));
    }

    @Test
    public void asText() {
        NodeAdapter subject  = newNodeAdapterForTest("{ aText : 'TEXT' }");
        assertEquals("TEXT", subject.asText("aText", "DEFAULT"));
        assertEquals("DEFAULT", subject.asText("missingField", "DEFAULT"));
        assertNull(subject.asText("missingField", null));

        assertEquals("TEXT", subject.asText("aText"));
    }

    @Test(expected = OtpAppException.class)
    public void requiredAsText() {
        NodeAdapter subject  = newNodeAdapterForTest("{ }");
        subject.asText("missingField");
    }

    @Test
    public void asEnum() {
        // Given
        NodeAdapter subject  = newNodeAdapterForTest("{ key : 'A' }");

        // Then
        assertEquals("Get existing property", AnEnum.A, subject.asEnum("key", AnEnum.B));
        assertEquals("Get default value", AnEnum.B, subject.asEnum("missing-key", AnEnum.B));
    }

    @Test(expected = OtpAppException.class)
    public void asEnumWithIllegalPropertySet() {
        // Given
        NodeAdapter subject  = newNodeAdapterForTest("{ key : 'NONE_EXISTING_ENUM_VALUE' }");

        // Then expect an error when value 'NONE_EXISTING_ENUM_VALUE' is not in the set of legal
        // values: ['A', 'B', 'C']
        subject.asEnum("key", AnEnum.B);
    }

    @Test
    public void asEnumMap() {
        // With optional enum values in map
        NodeAdapter subject  = newNodeAdapterForTest("{ key : { A: true, B: false } }");
        assertEquals(Map.of(AnEnum.A, true, AnEnum.B, false), subject.asEnumMap("key", AnEnum.class, NodeAdapter::asBoolean));
        assertEquals(Collections.<AnEnum, Boolean>emptyMap(), subject.asEnumMap("missing-key", AnEnum.class, NodeAdapter::asBoolean));
    }

    @Test
    public void asEnumMapWithUnknownValue() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key : { unknown : 7 } }");
        assertEquals(Map.<AnEnum, Double>of(), subject.asEnumMap("key", AnEnum.class, NodeAdapter::asDouble));

        // Assert unknown parameter is logged at warning level and with full pathname
        Logger log = Mockito.mock(Logger.class);
        subject.logAllUnusedParameters(log);
        Mockito.verify(log)
            .warn(
                Mockito.anyString(),
                Mockito.eq("key.unknown"),
                Mockito.eq("Test")
            );
    }

    @Test
    public void asEnumMapAllKeysRequired() {
        // Require all enum values to exist (if param exist)
        NodeAdapter subject  = newNodeAdapterForTest("{ key : { A: true, B: false, C: true } }");
        assertEquals(
            Map.of(AnEnum.A, true, AnEnum.B, false, AnEnum.C, true),
            subject.asEnumMapAllKeysRequired("key", AnEnum.class, NodeAdapter::asBoolean)
        );
        assertNull(subject.asEnumMapAllKeysRequired("missing-key", AnEnum.class, NodeAdapter::asText));
    }

    @Test(expected = OtpAppException.class)
    public void asEnumMapWithRequiredMissingValue() {
        // A value for C is missing in map
        NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");
        subject.asEnumMapAllKeysRequired("key", AnEnum.class, NodeAdapter::asBoolean);
    }

    @Test
    public void asEnumSetUsingJsonArray() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key : [ 'A', 'B' ] }");
        assertEquals(Set.of(AnEnum.A, AnEnum.B), subject.asEnumSet("key", AnEnum.class));
        assertEquals(Set.of(), subject.asEnumSet("missing-key", AnEnum.class));
    }

    @Test
    public void asEnumSetUsingConcatenatedString() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key : 'A,B' }");
        assertEquals(Set.of(AnEnum.A, AnEnum.B), subject.asEnumSet("key", AnEnum.class));
    }

    @Test
    public void asFeedScopedId() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key1: 'A:23', key2: 'B:12' }");
        assertEquals("A:23", subject.asFeedScopedId("key1", null).toString());
        assertEquals("B:12", subject.asFeedScopedId("key2", null).toString());
        assertEquals("C:12", subject.asFeedScopedId("missing-key", new FeedScopedId("C", "12")).toString());
    }

    @Test
    public void asDateOrRelativePeriod() {
        // Given
        NodeAdapter subject  = newNodeAdapterForTest("{ 'a' : '2020-02-28', 'b' : '-P3Y' }");

        // Then
        assertEquals(
                LocalDate.of(2020, 2, 28),
                subject.asDateOrRelativePeriod("a", null)
        );

        assertEquals(
                LocalDate.now().minusYears(3),
                subject.asDateOrRelativePeriod("b", null)
        );
        assertEquals(
                LocalDate.of(2020, 3, 1),
                subject.asDateOrRelativePeriod( "do-no-exist", "2020-03-01")
        );
        assertNull(subject.asDateOrRelativePeriod("do-no-exist", null));
    }

    @Test(expected = OtpAppException.class)
    public void testParsePeriodDateThrowsException() throws Exception {
        // Given
        NodeAdapter subject  = newNodeAdapterForTest("{ 'foo' : 'bar' }");

        // Then
        subject.asDateOrRelativePeriod("foo", null);
    }

    @Test
    public void asLocale() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key1 : 'no', key2 : 'no_NO', key3 : 'no_NO_NY' }");
        assertEquals("no", subject.asLocale("key1", null).toString());
        assertEquals("no_NO", subject.asLocale("key2", null).toString());
        assertEquals("no_NO_NY", subject.asLocale("key3", null).toString());
        assertEquals(Locale.FRANCE, subject.asLocale("missing-key", Locale.FRANCE));
    }


    @Test
    public void asPattern() {
        NodeAdapter subject  = newNodeAdapterForTest("{ aPtn : 'Ab*a' }");
        assertEquals("Ab*a",  subject.asPattern("aPtn", "ABC").toString());
        assertEquals("ABC",  subject.asPattern("missingField", "ABC").toString());
    }

    @Test
    public void uri() {
        NodeAdapter subject  = newNodeAdapterForTest("{ aUri : 'gs://bucket/path/a.obj' }");
        assertEquals("gs://bucket/path/a.obj",  subject.asUri("aUri", null).toString());
        assertEquals("http://foo.bar/", subject.asUri("missingField", "http://foo.bar/").toString());
        assertNull(subject.asUri("missingField", null));
    }

    @Test
    public void uriSyntaxException() {
        NodeAdapter subject  = newNodeAdapterForTest("{ aUri : 'error$%uri' }");
        try {
            subject.asUri("aUri", null);
            fail("Expected an exception");
        }
        catch (OtpAppException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("error$%uri"));
        }
    }


    @Test
    public void uris() {
        NodeAdapter subject  = newNodeAdapterForTest("{ foo : ['gs://a/b', 'gs://c/d'] }");
        assertEquals("[gs://a/b, gs://c/d]", subject.asUris("foo").toString());

        subject  = newNodeAdapterForTest("{ }");
        assertEquals("[]", subject.asUris("foo").toString());
    }

    @Test
    public void urisNotAnArrayException() {
        NodeAdapter subject  = newNodeAdapterForTest("{ 'uris': 'no array' }");
        try {
            subject.asUris("uris");
            fail("Expected an exception");
        }
        catch (OtpAppException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'uris': 'no array'"));
            assertTrue(e.getMessage(), e.getMessage().contains("Source: Test"));
        }
    }
}