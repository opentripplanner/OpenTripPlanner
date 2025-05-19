package org.opentripplanner.framework.error;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Use the word list to format nice list of words:
 * <ul>
 *   <li>{@code [empty]} -> {@code ""}</li>
 *   <li>{@code one} -> {@code "one"}</li>
 *   <li>{@code one, two} -> {@code "one and two"}</li>
 *   <li>{@code one, two, three} -> {@code "one, two and three"}</li>
 *   <li>and so on...</li>
 * </ul>
 */
public class WordList {

  private final List<String> words = new ArrayList<String>();

  private WordList() {}

  public static WordList of() {
    return new WordList();
  }

  public static WordList of(String word) {
    return new WordList().add(word);
  }

  public static WordList of(String... words) {
    return new WordList().add(words);
  }

  public WordList add(String word) {
    this.words.add(word);
    return this;
  }

  public WordList add(String... words) {
    this.words.addAll(Arrays.asList(words));
    return this;
  }

  @Override
  public String toString() {
    switch (words.size()) {
      case 0:
        return "";
      case 1:
        return words.getFirst();
      default:
        var buf = new StringBuilder(words.get(0));
        for (int i = 1; i < words.size() - 1; i++) {
          buf.append(", ").append(words.get(i));
        }
        buf.append(" and ").append(words.getLast());
        return buf.toString();
    }
  }

  public boolean isEmpty() {
    return words.isEmpty();
  }
}
