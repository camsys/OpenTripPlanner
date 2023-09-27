package org.opentripplanner.ext.fares.model;

import org.onebusaway.gtfs.model.FareProduct;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class FareLegRule {
  private final String feedId;
  private final String networkId;
  private final String fromAreaId;
  private final String toAreadId;
  private final FareProduct fareProduct;

  FareLegRule(@Nonnull String feedId,
              @Nullable String networkId,
              @Nullable String fromAreaId,
              @Nullable String toAreadId,
              @Nonnull FareProduct fareProduct
  ) {
    this.feedId = feedId;
    this.networkId = networkId;
    this.fromAreaId = fromAreaId;
    this.toAreadId = toAreadId;
    this.fareProduct = fareProduct;
  }

  public String feedId() {
    return feedId;
  }

  public String networkId() {
    return networkId;
  }

  public String fromAreaId() {
    return fromAreaId;
  }

  public String toAreadId() {
    return toAreadId;
  }

  public FareProduct fareProduct() {
    return fareProduct;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (FareLegRule) obj;
    return Objects.equals(this.feedId, that.feedId) &&
            Objects.equals(this.networkId, that.networkId) &&
            Objects.equals(this.fromAreaId, that.fromAreaId) &&
            Objects.equals(this.toAreadId, that.toAreadId) &&
            Objects.equals(this.fareProduct, that.fareProduct);
  }

  @Override
  public int hashCode() {
    return Objects.hash(feedId, networkId, fromAreaId, toAreadId, fareProduct);
  }

  @Override
  public String toString() {
    return "FareLegRule[" +
            "feedId=" + feedId + ", " +
            "networkId=" + networkId + ", " +
            "fromAreaId=" + fromAreaId + ", " +
            "toAreadId=" + toAreadId + ", " +
            "fareProduct=" + fareProduct + ']';
  }
}
