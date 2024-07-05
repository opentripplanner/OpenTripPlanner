package org.opentripplanner.ext.geocoder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.CapitalizationFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * A custom analyzer for stop names. It removes english stop words (at,the...) and splits
 * the input into NGrams (https://en.wikipedia.org/wiki/N-gram) so that the middle
 * of a stop name can be matched efficiently.
 * <p>
 * For example the query of "exanderpl" will match the stop name "Alexanderplatz".
 */
class EnglishNGramAnalyzer extends Analyzer {

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    StandardTokenizer src = new StandardTokenizer();
    TokenStream result = new EnglishPossessiveFilter(src);
    result = new LowerCaseFilter(result);
    result = new StopFilter(result, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
    result = new PorterStemFilter(result);
    result = new CapitalizationFilter(result);
    result = new NGramTokenFilter(result, 4, 10, true);
    return new TokenStreamComponents(src, result);
  }
}
