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
        "Ale",
        "Alex",
        "Alexa",
        "Alexan",
        "Alexand",
        "Alexande",
        "Alexander",
        "Alexanderp",
        "lex",
        "lexa",
        "lexan",
        "lexand",
        "lexande",
        "lexander",
        "lexanderp",
        "lexanderpl",
        "exa",
        "exan",
        "exand",
        "exande",
        "exander",
        "exanderp",
        "exanderpl",
        "exanderpla",
        "xan",
        "xand",
        "xande",
        "xander",
        "xanderp",
        "xanderpl",
        "xanderpla",
        "xanderplat",
        "and",
        "ande",
        "ander",
        "anderp",
        "anderpl",
        "anderpla",
        "anderplat",
        "anderplatz",
        "nde",
        "nder",
        "nderp",
        "nderpl",
        "nderpla",
        "nderplat",
        "nderplatz",
        "der",
        "derp",
        "derpl",
        "derpla",
        "derplat",
        "derplatz",
        "erp",
        "erpl",
        "erpla",
        "erplat",
        "erplatz",
        "rpl",
        "rpla",
        "rplat",
        "rplatz",
        "pla",
        "plat",
        "platz",
        "lat",
        "latz",
        "atz",
        "Alexanderplatz"
      ),
      result
    );
  }

  @Test
  void ampersand() throws IOException {
    var analyzer = new EnglishNGramAnalyzer();
    List<String> result = analyze("Meridian Ave N & N 148th St", analyzer);

    assertEquals(
      List.of(
        "Mer",
        "Meri",
        "Merid",
        "Meridi",
        "Meridia",
        "Meridian",
        "eri",
        "erid",
        "eridi",
        "eridia",
        "eridian",
        "rid",
        "ridi",
        "ridia",
        "ridian",
        "idi",
        "idia",
        "idian",
        "dia",
        "dian",
        "ian",
        "Av",
        "N",
        "N",
        "148",
        "148t",
        "148th",
        "48t",
        "48th",
        "8th",
        "St"
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
