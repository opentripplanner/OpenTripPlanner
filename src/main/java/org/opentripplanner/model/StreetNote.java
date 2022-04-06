package org.opentripplanner.model;

import java.io.Serializable;
import java.util.Date;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

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
}
