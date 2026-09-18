package org.opentripplanner.utils.text;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This utility class substitute variable placeholders in a given text on the format ${variable}.
 *
 * The pattern matching a placeholder must start with '${' and end with '}'. The variable name
 * must consist of only alphanumerical characters (a-z, A-Z, 0-9), dot `.` and underscore '_'.
 */
public class TextVariablesSubstitution {

  private static final Pattern PATTERN = Pattern.compile("\\$\\{([.\\w]+)}");

  /**
   * This method uses the {@link #insertVariables(String, Function, Consumer)} to substitute
   * all variable tokens in all values in the given {@code properties}. It supports nesting, but
   * you must avoid cyclic references.
   * <p>
   * Example:
   * <pre>
   *   a -> My car is a ${b} car, with an ${c} look.
   *   b -> good old ${c}
   *   c -> fancy
   * </pre>
   * This will resolve to:
   * <pre>
   *   a -> My car is a good old fancy car, with an fancy look.
   *   b -> good old fancy
   *   c -> fancy
   * </pre>
   */
  public static Map<String, String> insertVariables(
    Map<String, String> properties,
    Consumer<String> errorHandler
  ) {
    var result = new HashMap<String, String>(properties);

    for (String key : result.keySet()) {
      var value = result.get(key);
      var sub = insertVariables(value, result::get, errorHandler);
      if (!value.equals(sub)) {
        result.put(key, sub);
      }
    }
    return result;
  }

  /**
   * Replace all variables({@code ${variable.name}}) in the given {@code text}. The given
   * {@code variableProvider} is used to look up values to insert into the text replacing the
   * variable token.
   *
   * @param errorHandler The error handler is called if a variable key does not exist in the
   *                     {@code variableProvider}.
   * @return the new value with all variables replaced.
   */
  public static String insertVariables(
    String text,
    Function<String, String> variableProvider,
    Consumer<String> errorHandler
  ) {
    return insert(text, PATTERN.matcher(text), variableProvider, errorHandler);
  }

  private static String insert(
    String text,
    Matcher matcher,
    Function<String, String> variableProvider,
    Consumer<String> errorHandler
  ) {
    boolean matchFound = matcher.find();
    if (!matchFound) {
      return text;
    }

    Map<String, String> substitutions = new HashMap<>();

    while (matchFound) {
      String subKey = matcher.group(0);
      String nameOnly = matcher.group(1);
      if (!substitutions.containsKey(nameOnly)) {
        String value = variableProvider.apply(nameOnly);
        if (value != null) {
          substitutions.put(subKey, value);
        } else {
          errorHandler.accept(nameOnly);
        }
      }
      matchFound = matcher.find();
    }
    for (Map.Entry<String, String> entry : substitutions.entrySet()) {
      text = text.replace(entry.getKey(), entry.getValue());
    }
    return insert(text, PATTERN.matcher(text), variableProvider, errorHandler);
  }
}
