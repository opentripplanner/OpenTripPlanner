package org.opentripplanner.ext.geocoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnglishNgramAnalyzerTest {

  @Test
  void ngram() {
    List<String> result = tokenize("Alexanderplatz");

    //System.out.println(result.stream().collect(Collectors.joining("\",\"", "\"", "\"")));
    assertEquals(
      List.of(
        "Alex",
        "Alexa",
        "Alexan",
        "Alexand",
        "Alexande",
        "Alexander",
        "Alexanderp",
        "lexa",
        "lexan",
        "lexand",
        "lexande",
        "lexander",
        "lexanderp",
        "lexanderpl",
        "exan",
        "exand",
        "exande",
        "exander",
        "exanderp",
        "exanderpl",
        "exanderpla",
        "xand",
        "xande",
        "xander",
        "xanderp",
        "xanderpl",
        "xanderpla",
        "xanderplat",
        "ande",
        "ander",
        "anderp",
        "anderpl",
        "anderpla",
        "anderplat",
        "anderplatz",
        "nder",
        "nderp",
        "nderpl",
        "nderpla",
        "nderplat",
        "nderplatz",
        "derp",
        "derpl",
        "derpla",
        "derplat",
        "derplatz",
        "erpl",
        "erpla",
        "erplat",
        "erplatz",
        "rpla",
        "rplat",
        "rplatz",
        "plat",
        "platz",
        "latz",
        "Alexanderplatz"
      ),
      result
    );
  }

  @Test
  void ampersand() {
    List<String> result = tokenize("Meridian Ave N & N 148th St");

    assertEquals(
      List.of(
        "Meri",
        "Merid",
        "Meridi",
        "Meridia",
        "Meridian",
        "erid",
        "eridi",
        "eridia",
        "eridian",
        "ridi",
        "ridia",
        "ridian",
        "idia",
        "idian",
        "dian",
        "Av",
        "N",
        "N",
        "148",
        "St"
      ),
      result
    );
  }

  @ParameterizedTest
  @CsvSource(
    value = {
      "1st:1",
      "2nd:2",
      "3rd:3",
      "4th:4",
      "6th:6",
      "148th:148",
      "102nd:102",
      "1003rd:1003",
      "St:St",
      "S3:S3",
      "Aard:Aard",
    },
    delimiter = ':'
  )
  void numberSuffixes(String input, String expected) {
    var result = tokenize(input);
    assertEquals(List.of(expected), result);
  }

  @Test
  void wordBoundary() {
    var result = tokenize("1stst");
    assertEquals(List.of("1sts", "1stst", "stst"), result);
  }

  private List<String> tokenize(String text) {
    try (var analyzer = new EnglishNGramAnalyzer()) {
      List<String> result;
      TokenStream tokenStream;
      result = new ArrayList<>();
      tokenStream = analyzer.tokenStream("name", text);
      CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
      tokenStream.reset();
      while (tokenStream.incrementToken()) {
        result.add(attr.toString());
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
