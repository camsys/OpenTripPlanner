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
    return sb.toString();
  }

}
