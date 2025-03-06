package org.opentripplanner.framework.token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A token schema contains a set of token definitions, one for each version. This is
 * used to decode a token - the same version used to encode a token is used to
 * decode it. When encoding a token the latest version is always used.
 * <p>
 * OTP only need to be backward compatible with the last version of otp. So, for each release of
 * OTP the schema that is older than the previous version can be merged. By doing so, you do not
 * need to keep code around to support very old versions.
 */
public class TokenSchema {

  private final List<TokenDefinition> definitions;

  TokenSchema(List<TokenDefinition> definitions) {
    // Reverse the list so the newest version is first and the oldest version is last
    var list = new ArrayList<>(definitions);
    Collections.reverse(list);
    this.definitions = List.copyOf(list);
  }

  /**
   * Define a set of versioned tokens. The version number for each will be auto-incremented.
   * The provided {@code baseVersion} specify the version for the first token defined.
   * <p>
   * Old unused tokens definitions can merged into the first used definition. When this is done
   * the "new" base should be given the exact same version number as it had before. The best way to
   * ensure this is to add unit tests for all active version. If done right, the tests should not
   * change when merging the versions.
   * <p>
   * Take a look at the unit tests to see an example on merging a schema.
   * <p>
   * @param baseVersion The initial version for the first definition. The version number is
   *                    automatically incremented when new definitions are added. If there is many
   *                    definitions, the oldest definitions can be merged into one. The new
   *                    definition base version should match the highest version number of the
   *                    definitions merged into one. For example, if version 3, 4 and 5 is merged,
   *                    the new merged base version is 5. Legal range is [1, 1_000_000]
   */
  public static TokenDefinitionBuilder ofVersion(int baseVersion) {
    return new TokenDefinitionBuilder(baseVersion);
  }

  public Token decode(String token) {
    var deserializer = new Deserializer(token);
    for (TokenDefinition definition : definitions) {
      try {
        return new Token(definition, deserializer.deserialize(definition));
      } catch (Exception ignore) {}
    }
    throw new IllegalArgumentException(
      "Token is not valid. Unable to parse token: '" + token + "'."
    );
  }

  public TokenBuilder encode() {
    return new TokenBuilder(currentDefinition());
  }

  /**
   * We iterate over definitions in REVERSE order, because we want to use the
   * latest version.
   */
  public TokenDefinition currentDefinition() {
    return definitions.get(0);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TokenSchema.class)
      .addObj("definition", currentDefinition())
      .toString();
  }
}
