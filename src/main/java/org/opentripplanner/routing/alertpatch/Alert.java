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

package org.opentripplanner.routing.alertpatch;

import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class Alert implements Serializable {
    private static final long serialVersionUID = 8305126586053909836L;

    public String id;

    public I18NString alertHeaderText;

    public I18NString alertDescriptionText;

    public I18NString alertUrl;

    //null means unknown
    public Date effectiveStartDate;

    //null means unknown
    public Date effectiveEndDate;

    //null means unknown
    public Date createdDate;

    //null means unknown
    public Date updatedAt;

    //null means unknown
    public Date displayBeforeActive;

    public I18NString alertType;

    // friendly description of complex periods
    public I18NString humanReadableActivePeriod;

    public ArrayList<String> generalOrderNumberList;

    public ArrayList<String> servicePlanNumberList;

    public ArrayList<AlertAlternateStation> stationAlternatives;


    public static HashSet<Alert> newSimpleAlertSet(String text) {
        Alert note = createSimpleAlerts(text);
        HashSet<Alert> notes = new HashSet<Alert>(1);
        notes.add(note);
        return notes;
    }

    public static Alert createSimpleAlerts(String text) {
        Alert note = new Alert();
        note.alertHeaderText = new NonLocalizedString(text);
        return note;
    }

    public static Alert createSimpleAlerts(String header, String description) {
        Alert note = new Alert();
        note.alertHeaderText = new NonLocalizedString(header);
        note.alertDescriptionText = new NonLocalizedString(description);
        return note;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Alert)) {
            return false;
        }
        Alert ao = (Alert) o;
        if (id == null) {
            if (ao.id != null) {
                return false;
            }
        } else {
            if (!id.equals(ao.id)) {
                return false;
            }
        }
        if (updatedAt == null) {
            if (ao.updatedAt != null) {
                return false;
            }
        } else {
            if (!updatedAt.equals(ao.updatedAt)) {
                return false;
            }
        }
        if (alertDescriptionText == null) {
            if (ao.alertDescriptionText != null) {
                return false;
            }
        } else {
            if (!alertDescriptionText.equals(ao.alertDescriptionText)) {
                return false;
            }
        }
        if (alertHeaderText == null) {
            if (ao.alertHeaderText != null) {
                return false;
            }
        } else {
            if (!alertHeaderText.equals(ao.alertHeaderText)) {
                return false;
            }
        }
        if (alertUrl == null) {
            return ao.alertUrl == null;
        } else {
            return alertUrl.equals(ao.alertUrl);
        }
    }

    public int hashCode() {
        return (id == null ? 0 : id.hashCode())
                + (updatedAt == null ? 0 : updatedAt.hashCode())
                + (alertDescriptionText == null ? 0 : alertDescriptionText.hashCode())
                + (alertHeaderText == null ? 0 : alertHeaderText.hashCode())
                + (alertUrl == null ? 0 : alertUrl.hashCode());
    }

    @Override
    public String toString() {
        return "Alert('"
                + (alertHeaderText != null ? alertHeaderText.toString()
                        : alertDescriptionText != null ? alertDescriptionText.toString()
                                : "?") + "')";
    }
}
