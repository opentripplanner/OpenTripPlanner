package org.opentripplanner.standalone.config.framework.json;

import static org.opentripplanner.standalone.config.framework.json.ConfigType.OBJECT;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

/**
 * Information about a configuration parameter.
 *
 * @param name The parameter name in the JSON config file.
 * @param summary Short one sentence description of the parameter used for documentation.
 * @param description Long optional multi-line documentation of a parameter - use markdown.
 * @param type The NodeAdaptor supported type.
 * @param enumType If type is ENUM, ENUM_MAP or ENUM_SET, then this contains the enum type.
 * @param elementType If the type is an ARRAY or a MAP then this is the type of the value.
 * @param since This is the first OTP version this feature existed in.
 * @param defaultValue The default value used if this parameter is not present in the documentation.
 * @param required The config parameter is required. OTP will not start unless this parameter is
 *                 set.
 * @param skipChild Skip generating doc for this node - the child(this) is documented in the parent
 *                  node.
 *
 *
 * TODO DOC - Add Unit tests on this class using the builder
 */
public record NodeInfo(
  String name,
  String summary,
  @Nullable String description,
  ConfigType type,
  @Nullable Class<? extends Enum<?>> enumType,
  @Nullable ConfigType elementType,
  OtpVersion since,
  @Nullable String defaultValue,
  boolean required,
  boolean skipChild
)
  implements Comparable<NodeInfo> {
  static final String EXPERIMENTAL_FEATURE =
    "**THIS IS STILL AN EXPERIMENTAL FEATURE - IT MAY CHANGE WITHOUT ANY NOTICE!**";

  static final String TYPE_QUALIFIER = "type";
  static final String SOURCETYPE_QUALIFIER = "sourceType";

  public NodeInfo {
    Objects.requireNonNull(name);
    Objects.requireNonNull(type);
    Objects.requireNonNull(since);
    Objects.requireNonNull(summary);

    if (type.isMapOrArray()) {
      Objects.requireNonNull(elementType);
    }
  }

  /**
   * For some complex types like Map, we describe the child elements as part of the node-info of
   * the parent. Hence, we need to skip the child when generating documentation. So, this factory
   * method is used to generate a placeholder in these cases.
   * <p>
   * TODO DOC: A better way to do this is to remove this and add proper NodeInfo elements for
   *           the child, but that requires a bit of refactoring.
   */
  static NodeInfo ofSkipChild(String name) {
    return of()
      .withName(name)
      .withSummary("No doc, parent contains doc.")
      .withType(OBJECT)
      .withSince(OtpVersion.NA)
      .withOptional()
      .withSkipChild()
      .build();
  }

  /**
   * For array of objects the child elements does not have as part of the node-info of
   * the parent. Hence, we need to skip the child when generating documentation. So, this factory
   * method is used to generate a placeholder in these cases.
   * <p>
   * TODO DOC: A better way to do this is to remove this and add proper NodeInfo elements for
   *           the child, but that requires a bit of refactoring.
   */
  public NodeInfo arraysChild() {
    return of()
      .withName("{ object }")
      .withSummary("Nested object in array. The object type is determined by the parameters.")
      .withType(elementType)
      .withSince(since)
      .withOptional()
      .build();
  }

  /**
   * This method will return {@code true} if there is more information than just the basic
   * required info. It is used to be able to list a node in a "details" section of a document in
   * the case where there is more info than the info listed in the summary section.
   */
  public boolean printDetails() {
    return (description != null || enumType != null || elementType != null) && !isTypeQualifier();
  }

  static NodeInfoBuilder of() {
    return new NodeInfoBuilder();
  }

  @SuppressWarnings("ConstantConditions")
  public String typeDescription() {
    return switch (type) {
      case ARRAY -> elementType.docName() + "[]";
      case MAP -> "map of " + elementType.docName();
      case ENUM_MAP -> "enum map of " + elementType.docName();
      case ENUM_SET -> "enum set";
      default -> type.docName();
    };
  }

  /**
   * A type qualifier is a field in an JSON object which determines which type it is.
   * Usually the mapping is split in two different paths - creating different types.
   * For example, we have both NETEX and GTFS config types in the same list/JSON array.
   */
  public boolean isTypeQualifier() {
    return enumType != null && TYPE_QUALIFIER.equalsIgnoreCase(name);
  }

  public List<? extends Enum<?>> enumTypeValues() {
    return enumType == null ? List.of() : Arrays.stream(enumType.getEnumConstants()).toList();
  }

  /**
   * Format the given value (read from JSON file) to a Markdown formatted string.
   */
  public String toMarkdownString(Object value) {
    if (enumType != null) {
      value = StringUtils.kebabCase(value.toString());
    }
    return type.quote(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NodeInfo leafNode = (NodeInfo) o;
    return Objects.equals(name, leafNode.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    var builder = ValueObjectToStringBuilder.of()
      .addText(name)
      .addText(" : ")
      .addText(typeDescription());

    if (required) {
      builder.addText(" Required");
    } else {
      if (defaultValue != null) {
        builder.addText(" = ").addText(type.quote(defaultValue));
      } else {
        builder.addText(" Optional");
      }
    }
    builder.addText(" Since ").addText(since.toString());

    return builder.toString();
  }

  /**
   * NodeInfo's are sorted by:
   * <ol>
   *   <li>Type qualifier</li>
   *   <li>simple before complex types</li>
   *   <li>alphabetical on name</li>
   * </ol>
   */
  @Override
  public int compareTo(NodeInfo other) {
    // Put type qualifier first
    if (isTypeQualifier()) {
      return -1;
    }
    if (other.isTypeQualifier()) {
      return 1;
    }
    // Put simple types before complex
    if (type.isSimple() != other.type.isSimple()) {
      return type.isSimple() ? -1 : 1;
    }
    // Sort by name
    return name.compareTo(other.name);
  }
}
