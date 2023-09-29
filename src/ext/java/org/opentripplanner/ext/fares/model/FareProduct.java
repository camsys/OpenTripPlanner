package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import java.util.Objects;

import org.onebusaway.gtfs.model.RiderCategory;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public final class FareProduct {
  private final FeedScopedId id;
  private final String name;
  private final Money amount;
  private final Duration duration;
  private final RiderCategory category;
  private final FareContainer container;

  public FareProduct(
          FeedScopedId id,
          String name,
          Money amount,
          Duration duration,
          RiderCategory category,
          FareContainer container
  ) {
    this.id = id;
    this.name = name;
    this.amount = amount;
    this.duration = duration;
    this.category = category;
    this.container = container;
  }

  public FeedScopedId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Money amount() {
    return amount;
  }

  public Duration duration() {
    return duration;
  }

  public RiderCategory category() {
    return category;
  }

  public FareContainer container() {
    return container;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (FareProduct) obj;
    return Objects.equals(this.id, that.id) &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.amount, that.amount) &&
            Objects.equals(this.duration, that.duration) &&
            Objects.equals(this.category, that.category) &&
            Objects.equals(this.container, that.container);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, amount, duration, category, container);
  }

  @Override
  public String toString() {
    return "FareProduct[" +
            "id=" + id + ", " +
            "name=" + name + ", " +
            "amount=" + amount + ", " +
            "duration=" + duration + ", " +
            "category=" + category + ", " +
            "container=" + container + ']';
  }

  public boolean coversItinerary(Itinerary i) {
    var transitLegs = i.getScheduledTransitLegs();
    var allLegsInProductFeed = transitLegs
            .stream()
            .allMatch(leg -> leg.getAgency().getId().getFeedId().equals(id.getFeedId()));

    return (
            allLegsInProductFeed && (transitLegs.size() == 1 || coversDuration(i.getTransitDuration()))
    );
  }

  public boolean coversDuration(Duration journeyDuration) {
    return Objects.nonNull(duration) && duration.toSeconds() > journeyDuration.toSeconds();
  }
}
