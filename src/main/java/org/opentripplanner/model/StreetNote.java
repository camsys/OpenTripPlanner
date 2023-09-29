package org.opentripplanner.model;

import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;

import java.io.Serializable;
import java.util.Date;

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
