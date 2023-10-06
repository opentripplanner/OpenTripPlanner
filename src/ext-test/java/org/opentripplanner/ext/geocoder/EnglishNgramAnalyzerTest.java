package org.opentripplanner.ext.geocoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.Test;

class EnglishNgramAnalyzerTest {

  @Test
  void ngram() throws IOException {
    var analyzer = new EnglishNGramAnalyzer();
    List<String> result = analyze("Alexanderplatz", analyzer);

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

  public List<String> analyze(String text, Analyzer analyzer) throws IOException {
    List<String> result = new ArrayList<>();
    TokenStream tokenStream = analyzer.tokenStream("name", text);
    CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();
    while (tokenStream.incrementToken()) {
      result.add(attr.toString());
    }
    return result;
  }
}
