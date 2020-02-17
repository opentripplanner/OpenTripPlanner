package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opentripplanner.util.OtpAppException;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NodeAdapterTest {
    private enum AnEnum { A, B }

    static NodeAdapter newNodeAdapterForTest(String configText) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        // Replace ' with "
        configText = configText.replace("'", "\"");

        try {
            JsonNode config = mapper.readTree(configText);
            return new NodeAdapter(config, "Test");
        }
        catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Test
    public void asRawNode() {
        NodeAdapter subject  = newNodeAdapterForTest("{ foo : 'bar' }");
        assertTrue(subject.asRawNode().has("foo"));
    }

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
        assertEquals(-1d, subject.asDouble("missingField", -1d), 00.1);
    }

    @Test
    public void asInt() {
        NodeAdapter subject  = newNodeAdapterForTest("{ aInt : 5 }");
        assertEquals(5, subject.asInt("aInt", -1));
        assertEquals(-1, subject.asInt("missingField", -1));
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
        NodeAdapter subject  = newNodeAdapterForTest("{ 'key' : 'A' }");

        // Then
        assertEquals("Get existing property", AnEnum.A, subject.asEnum("key", AnEnum.B));
        assertEquals("Get default value", AnEnum.B, subject.asEnum("missing-key", AnEnum.B));
    }

    @Test(expected = OtpAppException.class)
    public void asEnumWithIllegalPropertySet() {
        // Given
        NodeAdapter subject  = newNodeAdapterForTest("{ 'key' : 'NONE_EXISTING_ENUM_VALUE' }");

        // Then expect an error when value 'X' is not in the set of legal values: ['A', 'B']
        subject.asEnum("key", AnEnum.B);
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
        NodeAdapter subject  = newNodeAdapterForTest("{ uris : 'no array' }");
        try {
            subject.asUris("uris");
            fail("Expected an exception");
        }
        catch (OtpAppException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Actual: \"uris\" : \"no array\""));
            assertTrue(e.getMessage(), e.getMessage().contains("Expected ARRAY of URIs"));
        }
    }

}