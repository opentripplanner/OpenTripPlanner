package org.opentripplanner.ext.ojp.mapping;

import de.vdv.ojp20.InternationalTextStructure;
import de.vdv.ojp20.siri.DefaultedTextStructure;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;

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
      var t = new DefaultedTextStructure().withValue(string);
      if (lang != null) {
        t.withLang(lang);
      }
      return new InternationalTextStructure().withText(t);
    }
  }
}
