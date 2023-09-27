package org.opentripplanner.ext.fares.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;

public final class FareRulesData {
  private final List<FareAttribute> fareAttributes;
  private final List<FareRule> fareRules;
  private final List<FareLegRule> fareLegRules;

  FareRulesData(
          List<FareAttribute> fareAttributes,
          List<FareRule> fareRules,
          List<FareLegRule> fareLegRules
  ) {
    this.fareAttributes = fareAttributes;
    this.fareRules = fareRules;
    this.fareLegRules = fareLegRules;
  }

  public List<FareAttribute> fareAttributes() {
    return fareAttributes;
  }

  public List<FareRule> fareRules() {
    return fareRules;
  }

  public List<FareLegRule> fareLegRules() {
    return fareLegRules;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (FareRulesData) obj;
    return Objects.equals(this.fareAttributes, that.fareAttributes) &&
            Objects.equals(this.fareRules, that.fareRules) &&
            Objects.equals(this.fareLegRules, that.fareLegRules);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fareAttributes, fareRules, fareLegRules);
  }

  @Override
  public String toString() {
    return "FareRulesData[" +
            "fareAttributes=" + fareAttributes + ", " +
            "fareRules=" + fareRules + ", " +
            "fareLegRules=" + fareLegRules + ']';
  }
}
