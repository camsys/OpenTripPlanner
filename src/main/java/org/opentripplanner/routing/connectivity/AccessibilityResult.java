package org.opentripplanner.routing.connectivity;

import org.opentripplanner.routing.alertpatch.TransitAlert;

import java.util.Collections;
import java.util.List;

/**
 * Models whether a stop is accessible or not, and if lack of accessibility is due to realtime.
 */
public class AccessibilityResult {
    private boolean isAccessible;

    private boolean isRealtime;

    private List<TransitAlert> alerts;

    public AccessibilityResult(boolean isAccessible, boolean isRealtime, List<TransitAlert> alerts) {
        this.isAccessible = isAccessible;
        this.isRealtime = isRealtime;
        this.alerts = alerts;
    }

    public boolean isAccessible() {
        return isAccessible;
    }

    public boolean isRealtime() {
        return isRealtime;
    }

    public List<TransitAlert> getAlerts() {
        return alerts;
    }

    public static final AccessibilityResult NEVER_ACCESSIBLE = new AccessibilityResult(
            false, false, Collections.emptyList());

    public static final AccessibilityResult ALWAYS_ACCESSIBLE = new AccessibilityResult(
            true, false, Collections.emptyList());

    public static AccessibilityResult notAccessibleForReason(List<TransitAlert> alerts) {
        return new AccessibilityResult(false, true, alerts);
    }
}
