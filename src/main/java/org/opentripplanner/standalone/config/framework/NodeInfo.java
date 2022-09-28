package org.opentripplanner.standalone.config.framework;

import static org.opentripplanner.standalone.config.framework.ConfigType.ARRAY;
import static org.opentripplanner.standalone.config.framework.ConfigType.ENUM;
import static org.opentripplanner.standalone.config.framework.ConfigType.ENUM_MAP;
import static org.opentripplanner.standalone.config.framework.ConfigType.ENUM_SET;
import static org.opentripplanner.standalone.config.framework.ConfigType.MAP;

import java.util.EnumSet;
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
 * @param deprecated This parameter is no longer in use in OTP, we keep it here to be able to
 *                   generate documentation.
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
  @Nullable DeprecatedInfo deprecated
) {
  public NodeInfo {
    Objects.requireNonNull(name);
    Objects.requireNonNull(type);
    Objects.requireNonNull(since);
    Objects.requireNonNull(summary);
  }

  public boolean printDetails() {
    return description != null;
  }

  public boolean isDeprecated() {
    return deprecated != null;
  }

  static Builder of() {
    return new Builder();
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
        builder.addText(" = ").addText(type.wrap(defaultValue));
      } else {
        builder.addText(" Optional");
      }
    }
    builder.addText(" Since ").addText(since.toString());

    return builder.toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  static class Builder {

    private String name;
    private ConfigType type;
    private Class<Enum<?>> enumType;
    private ConfigType elementType;

    @Nullable
    Class<Enum<?>> elementEnumType;

    private OtpVersion since = OtpVersion.NA;
    private String summary = "TODO: Add short summary.";
    private String description = null;
    private String defaultValue = null;
    private boolean required = true;
    private DeprecatedInfo deprecated = null;

    public String name() {
      return name;
    }

    Builder withName(String name) {
      this.name = name;
      return this;
    }

    Builder withType(ConfigType type) {
      if (EnumSet.of(ARRAY, MAP, ENUM_MAP, ENUM_SET, ENUM).contains(type)) {
        throw new IllegalArgumentException(
          "Use type specific build methods for this type like 'withArray'. Type : " + type
        );
      }
      this.type = type;
      return this;
    }

    Builder withSince(OtpVersion since) {
      this.since = since;
      return this;
    }

    public Builder withSummary(String summary) {
      this.summary = summary;
      return this;
    }

    Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    Builder withDeprecated(OtpVersion deprecatedSince, String description) {
      this.deprecated = new DeprecatedInfo(deprecatedSince, description);
      return this;
    }

    Builder withOptional(String defaultValue) {
      this.defaultValue = defaultValue;
      return withOptional();
    }

    Builder withOptional() {
      this.required = false;
      return this;
    }

    Builder withRequired() {
      this.required = true;
      return this;
    }

    Builder withEnum(Class<Enum<?>> enumType) {
      this.type = ENUM;
      this.enumType = enumType;
      return this;
    }

    Builder withArray(ConfigType elementType) {
      this.type = ARRAY;
      this.elementType = elementType;
      return this;
    }

    Builder withMap(ConfigType elementType) {
      this.type = MAP;
      this.elementType = elementType;
      return this;
    }

    Builder withEnumMap(Class<Enum<?>> enumType, ConfigType elementType) {
      this.type = ENUM_MAP;
      this.enumType = enumType;
      this.elementType = elementType;
      return this;
    }

    Builder withEnumSet(Class<Enum<?>> enumType) {
      this.type = ENUM_SET;
      this.elementType = ENUM;
      this.enumType = enumType;
      return this;
    }

    NodeInfo build() {
      return new NodeInfo(
        name,
        summary,
        description,
        type,
        enumType,
        elementType,
        since,
        defaultValue,
        required,
        deprecated
      );
    }
  }
}
