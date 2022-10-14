package org.opentripplanner.standalone.config.framework.json;

import static org.opentripplanner.standalone.config.framework.json.ConfigType.ARRAY;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM_MAP;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM_SET;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.MAP;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.OBJECT;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.util.lang.ValueObjectToStringBuilder;

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
 * @param deprecated This parameter is no longer in use in OTP, we keep it here to be able to
 *                   generate documentation.
 *
 *
 * TODO DOC - Add Unit tests on this class using the builder
 */
public record NodeInfo(
  String name,
  String summary,
  @Nullable String description,
  ConfigType type,
  @Nullable Class<Enum<?>> enumType,
  @Nullable ConfigType elementType,
  OtpVersion since,
  @Nullable String defaultValue,
  boolean required,
  boolean skipChild,
  @Nullable DeprecatedInfo deprecated
) {
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
   * This method will return {@code true} if there is more information than just the basic
   * required info. It is used to be able to list a node in a "details" section of a document in
   * the case where there is more info than the info listed in the summary section.
   */
  public boolean printDetails() {
    return description != null;
  }

  public boolean isDeprecated() {
    return deprecated != null;
  }

  static NodeInfoBuilder of() {
    return new NodeInfoBuilder();
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

  @SuppressWarnings("ConstantConditions")
  @Override
  public String toString() {
    var builder = ValueObjectToStringBuilder.of().addText(name).addText(" : ");

    if (type == ARRAY) {
      builder.addText(elementType.docName()).addText("[]");
    } else if (type == MAP) {
      builder.addText("map of ").addText(elementType.docName());
    } else if (type == ENUM_MAP) {
      builder.addText("enum map of ").addText(elementType.docName());
    } else if (type == ENUM_SET) {
      builder.addText("set of ").addText(elementType.docName());
    } else {
      builder.addText(type.docName());
    }

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

  @SuppressWarnings("ConstantConditions")
  private String exampleValueJson(ConfigType type) {
    return type.quote(
      switch (type) {
        case BOOLEAN -> "true";
        case DOUBLE -> "3.15";
        case INTEGER -> "123";
        case LONG -> "1000";
        case ENUM -> firstEnumValue();
        case STRING -> "A String";
        case LOCALE -> "en_US";
        case DATE -> "20022-05-31";
        case DATE_OR_PERIOD -> "P1Y5D";
        case DURATION -> "45s";
        case REGEXP -> "-?[\\d+=*/ =]+";
        case URI -> "https://www.opentripplanner.org/";
        case ZONE_ID -> "Europe/Paris";
        case FEED_SCOPED_ID -> "FID:Trip0001";
        case LINEAR_FUNCTION -> "600 + 3.0 x";
        case OBJECT -> "{a:1}";
        case MAP -> "{a:" + exampleValueJson(elementType) + "}";
        case ENUM_MAP -> "{" + firstEnumValue() + " : " + exampleValueJson(elementType) + "}";
        case ENUM_SET -> "[" + firstEnumValue() + "]";
        case ARRAY -> "[{n:1},{n:2}]";
      }
    );
  }
  private String firstEnumValue() {
    //noinspection ConstantConditions
    return enumType.getEnumConstants()[0].name();
  }
}
