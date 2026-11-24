package org.opentripplanner.framework.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;

class CsvRecordReaderTest {

  @Test
  void readHeadersAndRecords() {
    try (var subject = CsvRecordReader.parse("id,name\n1,Oslo\n2,Bergen\n")) {
      assertTrue(subject.readHeaders());
      assertEquals(List.of("id", "name"), subject.headers());

      assertTrue(subject.readRecord());
      assertEquals("1", subject.get("id"));
      assertEquals("Oslo", subject.get("name"));

      assertTrue(subject.readRecord());
      assertEquals("2", subject.get("id"));
      assertEquals("Bergen", subject.get("name"));

      assertFalse(subject.readRecord());
    }
  }

  @Test
  void emptyInput() {
    try (var subject = CsvRecordReader.parse("")) {
      assertFalse(subject.readHeaders());
      assertEquals(List.of(), subject.headers());
      assertFalse(subject.readRecord());
    }
  }

  @Test
  void missingColumnAndShortRecordReturnEmptyString() {
    try (var subject = CsvRecordReader.parse("id,name\n1\n")) {
      subject.readHeaders();
      subject.readRecord();
      assertEquals("1", subject.get("id"));
      assertEquals("", subject.get("name"));
      assertEquals("", subject.get("unknown"));
    }
  }

  @Test
  void trimSpacesAndSkipEmptyLines() {
    try (var subject = CsvRecordReader.parse("id , name\n\n 1 ,  Oslo \n\n")) {
      subject.readHeaders();
      assertEquals(List.of("id", "name"), subject.headers());
      assertTrue(subject.readRecord());
      assertEquals("1", subject.get("id"));
      assertEquals("Oslo", subject.get("name"));
      assertFalse(subject.readRecord());
    }
  }

  @Test
  void quotedValues() {
    try (var subject = CsvRecordReader.parse("a,b\n\"x, y\",\"say \"\"hi\"\"\"\n")) {
      subject.readHeaders();
      subject.readRecord();
      assertEquals("x, y", subject.get("a"));
      assertEquals("say \"hi\"", subject.get("b"));
    }
  }

  @Test
  void rawRecordJoinsValuesWithCommas() {
    try (var subject = CsvRecordReader.parse("a,b,c\n1, 2 ,\"3\"\n")) {
      subject.readHeaders();
      subject.readRecord();
      assertEquals("1,2,3", subject.rawRecord());
    }
  }

  @Test
  void malformedQuotingThrowsJacksonException() {
    try (var subject = CsvRecordReader.parse("a,b\n\"x\"y,z\n")) {
      subject.readHeaders();
      assertThrows(JacksonException.class, subject::readRecord);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE" })
  void detectEncoding(String charsetName) {
    byte[] bytes = "id,name\n1,Tromsø\n".getBytes(Charset.forName(charsetName));
    try (var subject = CsvRecordReader.of(new ByteArrayInputStream(bytes))) {
      subject.readHeaders();
      subject.readRecord();
      assertEquals("Tromsø", subject.get("name"));
    }
  }
}
