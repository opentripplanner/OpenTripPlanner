package org.opentripplanner.street.model.note;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;

public class StreetNote implements Serializable {

  public final I18NString note;
  public I18NString descriptionText;
  public Date effectiveStartDate;
  public Date effectiveEndDate;
  public String url;

  public StreetNote(I18NString note) {
    this.note = note;
  }

  public StreetNote(String note) {
    this.note = new NonLocalizedString(note);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StreetNote that = (StreetNote) o;
    return (
      Objects.equals(note, that.note) &&
      Objects.equals(descriptionText, that.descriptionText) &&
      Objects.equals(effectiveStartDate, that.effectiveStartDate) &&
      Objects.equals(effectiveEndDate, that.effectiveEndDate) &&
      Objects.equals(url, that.url)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(note, descriptionText, effectiveStartDate, effectiveEndDate, url);
  }
}
