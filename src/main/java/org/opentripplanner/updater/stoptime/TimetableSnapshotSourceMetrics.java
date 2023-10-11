package org.opentripplanner.updater.stoptime;

/**
 * Metrics representing realtime consumption per feed.  Prefer this
 * to extensive log messages.
 */
public class TimetableSnapshotSourceMetrics {

  int tripUpdates = 0;
  int unmatchedTripUpdates = 0;
  int rejectedTripUpdates = 0;
  int scheduledTripUpdates = 0;
  int addedTripUpdates = 0;
  int unscheduledTripUpdates = 0;
  int canceledTripUpdates = 0;
  int modifedTripUpdates = 0;
  int missingPattern = 0;
  int noStoptimeUpdates = 0;
  int missingTripDescriptor = 0;
  int unknownTripId = 0;
  int tripIdNotInPattern = 0;
  int badArrivalTime = 0;
  int badDepartureTime = 0;
  int decreasingTimes = 0;

  public void addMissingPattern() {
    missingPattern++;
  }
  public void addNoStoptimeUpdates() {
    noStoptimeUpdates++;
  }
  public void addMissingTripDescriptor() {
    missingTripDescriptor++;
  }
  public void addUnknownTripId() {
    unknownTripId++;
  }
  public void addTripIdNotInPattern() {
    tripIdNotInPattern++;
  }
  public void addBadArrivalTime() {
    badArrivalTime++;
  }
  public void addBadDepartureTime() {
    badDepartureTime++;
  }
  public void addDecreasingTimes() {
    decreasingTimes++;
  }

  public void addTripUpdate() {
    tripUpdates++;
  }

  public void addUnmatchedTripUpdate() {
    unmatchedTripUpdates++;
  }
  public void addRejectedTripUpdate() {
    rejectedTripUpdates++;
  }
  public void removeRejectedTripUpdate() {
    rejectedTripUpdates--;
  }

  public void addScheduledTripUpdate() {
    scheduledTripUpdates++;
  }

  public void addAddedTripUpdate() {
    addedTripUpdates++;
  }

  public void addUnscheduledTripUpdate() {
    unscheduledTripUpdates++;
  }

  public void addCanceledTripUpdate() {
    canceledTripUpdates++;
  }

  public void addModifedTripUpdate() {
    modifedTripUpdates++;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Total: ").append(tripUpdates).append(" ");
    sb.append("ADDED: ").append(addedTripUpdates).append(" ");
    sb.append("CANCELED: ").append(canceledTripUpdates).append(" ");
    sb.append("Updates: ").append(scheduledTripUpdates).append(" ");
    sb.append("Unmatched: ").append(unmatchedTripUpdates).append(" ");
    sb.append("Rejected: ").append(rejectedTripUpdates).append(" ");
    sb.append("Missing Pattern: ").append(missingPattern).append(" ");
    sb.append("NoStoptimeUpdates: ").append(noStoptimeUpdates).append(" ");
    sb.append("MissingTripDescriptor: ").append(missingTripDescriptor).append(" ");
    sb.append("UnknownTripId: ").append(unknownTripId).append(" ");
    sb.append("TripIdNotInPattern: ").append(tripIdNotInPattern).append(" ");
    sb.append("BadArrivalTime: ").append(badArrivalTime).append(" ");
    sb.append("BadDepartureTime: ").append(badDepartureTime).append(" ");
    sb.append("DecreasingTimes: ").append(decreasingTimes).append(" ");
    return sb.toString();
  }

}
