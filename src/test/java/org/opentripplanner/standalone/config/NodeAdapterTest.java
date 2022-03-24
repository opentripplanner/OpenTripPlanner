package org.opentripplanner.standalone.config;

import java.time.Duration;
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
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.standalone.config.JsonSupport.newNodeAdapterForTest;

public class NodeAdapterTest {

    public static final Duration D3h = Duration.ofHours(3);

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
        assertEquals("Get existing property", AnEnum.A, subject.asEnum("key", AnEnum.class));
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
                Mockito.eq("key.unknown:7"),
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
    public void testParsePeriodDateThrowsException() {
        // Given
        NodeAdapter subject  = newNodeAdapterForTest("{ 'foo' : 'bar' }");

        // Then
        subject.asDateOrRelativePeriod("foo", null);
    }

    @Test
    public void asDuration() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key1 : 'PT1s', key2 : '4d3h2m1s' }");
        assertEquals("PT1S", subject.asDuration("key1", null).toString());
        assertEquals("PT99H2M1S", subject.asDuration("key2", null).toString());
        assertEquals("PT3H", subject.asDuration("missing-key", D3h).toString());
    }

    @Test
    public void asDurations() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key1 : ['PT1s', '2h'] }");
        assertEquals("[PT1S, PT2H]", subject.asDurations("key1", List.of()).toString());
        assertEquals("[PT3H]", subject.asDurations("missing-key", List.of(D3h)).toString());
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
        NodeAdapter subject  = newNodeAdapterForTest("{ key : 'Ab*a' }");
        assertEquals("Ab*a",  subject.asPattern("key", "ABC").toString());
        assertEquals("ABC",  subject.asPattern("missingField", "ABC").toString());
    }

    @Test
    public void uri() {
        var URL = "gs://bucket/a.obj";
        NodeAdapter subject  = newNodeAdapterForTest("{ aUri : '" + URL + "' }");

        assertEquals(URL,  subject.asUri("aUri").toString());
        assertEquals(URL,  subject.asUri("aUri", null).toString());
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
    public void uriRequiredValueMissing() {
        NodeAdapter subject  = newNodeAdapterForTest("{ }");
        try {
            subject.asUri("aUri");
            fail("Expected an exception");
        }
        catch (OtpAppException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("Required parameter 'aUri' not found in 'Test'")
            );
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

    @Test
    public void objectAsList() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key : [{ a: 'I' }, { a: '2' } ] }");

        List<NodeAdapter> result = subject.path("key").asList();

        String content = result.stream().map(n ->
            n.asText("a")).collect(Collectors.joining(", ")
        );

        assertEquals("I, 2", content);
    }

    @Test
    public void linearFunction() {
        NodeAdapter subject  = newNodeAdapterForTest("{ key : '4+8x' }");
        assertEquals(
            "f(x) = 4.0 + 8.0 x",
            subject.asLinearFunction("key", null).toString()
        );
        assertNull(subject.asLinearFunction("no-key", null));
    }

    @Test
    public void asMap() {
        NodeAdapter subject = newNodeAdapterForTest("{ key : { A: true, B: false } }");
        assertEquals(
            Map.of("A", true, "B", false),
            subject.asMap("key", NodeAdapter::asBoolean)
        );
        assertEquals(
            Collections.<String, Boolean>emptyMap(),
            subject.asMap("missing-key", NodeAdapter::asBoolean)
        );
    }

    @Test
    public void asTextSet() {
        NodeAdapter subject = newNodeAdapterForTest("{ ids : ['A', 'C', 'F'] }");
        assertEquals(
                Set.of("A", "C", "F"),
                subject.asTextSet("ids", Collections.emptySet())
        );
        assertEquals(
                Set.of("X"),
                subject.asTextSet("nonExisting", Set.of("X"))
        );
    }

    @Test
    public void isNonEmptyArray() {
        NodeAdapter subject = newNodeAdapterForTest("{ foo : ['A'], bar: [], foobar: true }");
        assertTrue(subject.path("foo").isNonEmptyArray());
        assertFalse(subject.path("bar").isNonEmptyArray());
        assertFalse(subject.path("foobar").isNonEmptyArray());
        assertFalse(subject.path("missing").isNonEmptyArray());
    }
}