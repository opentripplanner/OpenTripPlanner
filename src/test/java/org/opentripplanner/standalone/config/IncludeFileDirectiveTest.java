package org.opentripplanner.standalone.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class IncludeFileDirectiveTest {
    private static final String FILE_PREFIX = "o_o_standalone_config_IncludeFileDirectiveTest_";
    private static final String PART_FILE_NAME = FILE_PREFIX + "part.json";
    private static final File CONFIG_DIR = new File(".");
    private static final File PART_FILE = new File(CONFIG_DIR, PART_FILE_NAME);

    @Test
    void includeFileWithoutQuotes() throws IOException {
        savePartialFile(quote("value"));
        String result = IncludeFileDirective.includeFileDirective(
                CONFIG_DIR,
                quote("{${includeFile:" + PART_FILE_NAME + "}}"),
                PART_FILE_NAME
        );
        assertEquals(quote("{value}"), result);
    }

    @Test
    void includeFileWithQuotesAndProperJsonInput() throws IOException {
        savePartialFile(quote("\t {\n  'foo' : 'bar' \n  }\n"));
        String result = IncludeFileDirective.includeFileDirective(
                CONFIG_DIR,
                quote("{ 'key' : '${includeFile:" + PART_FILE_NAME + "}'}"),
                PART_FILE_NAME
        );
        assertEquals(quote("{ 'key' : \t {\n  'foo' : 'bar' \n  }\n}"), result);
    }

    @Test
    void includeFileWithQuotesWithNoJsonInput() throws IOException {
        savePartialFile("value");
        String result = IncludeFileDirective.includeFileDirective(
                CONFIG_DIR,
                quote("{ 'key' : '${includeFile:" + PART_FILE_NAME + "}' }"),
                PART_FILE_NAME
        );
        assertEquals(quote("{ 'key' : 'value' }"), result);
    }

    private static String quote(String text) {
        return text.replace('\'', '"');
    }

    private static void savePartialFile(String text) throws IOException {
        FileUtils.write(PART_FILE, text, UTF_8);
    }

    @AfterAll
    static void teardown() {
        //noinspection ResultOfMethodCallIgnored
        PART_FILE.delete();
    }
}