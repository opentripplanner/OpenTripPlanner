package org.opentripplanner.ext.ojp.mapping;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.ojp.schema.ojp.InternationalTextStructure;
import org.opentripplanner.ojp.schema.siri.NaturalLanguageStringStructure;

class TextMapper {

  public static InternationalTextStructure internationalText(I18NString name) {
    return internationalText(name, null);
  }

  @Nullable
  public static InternationalTextStructure internationalText(@Nullable String name) {
    if (name == null) {
      return null;
    } else {
      return internationalText(name, null);
    }
  }

  static InternationalTextStructure internationalText(I18NString string, @Nullable String lang) {
    if (string == null) {
      return null;
    } else {
      return internationalText(string.toString(), lang);
    }
  }

  static InternationalTextStructure internationalText(String string, @Nullable String lang) {
    if (string == null) {
      return null;
    } else {
      var t = new NaturalLanguageStringStructure().withValue(string);
      if (lang != null) {
        t.withLang(lang);
      }
      return new InternationalTextStructure().withText(t);
    }
  }
}
