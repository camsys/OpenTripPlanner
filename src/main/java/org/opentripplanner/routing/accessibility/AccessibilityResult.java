/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.routing.accessibility;

import org.opentripplanner.routing.alertpatch.Alert;

import java.util.Collections;
import java.util.List;

/**
 * Models whether a stop is accessible or not, and if lack of accessibility is due to realtime.
 */
public class AccessibilityResult {
    private boolean isAccessible;

    private boolean isRealtime;

    private List<Alert> alerts;

    public AccessibilityResult(boolean isAccessible, boolean isRealtime, List<Alert> alerts) {
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

    public List<Alert> getAlerts() {
        return alerts;
    }

    public static final AccessibilityResult NEVER_ACCESSIBLE = new AccessibilityResult(
            false, false, Collections.emptyList());

    public static final AccessibilityResult ALWAYS_ACCESSIBLE = new AccessibilityResult(
            true, false, Collections.emptyList());

    public static AccessibilityResult notAccessibleForReason(List<Alert> alerts) {
        return new AccessibilityResult(false, true, alerts);
    }
}
