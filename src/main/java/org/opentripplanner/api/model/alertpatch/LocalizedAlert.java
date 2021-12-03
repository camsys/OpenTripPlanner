package org.opentripplanner.api.model.alertpatch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertAlternateStation;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class LocalizedAlert {

    @JsonIgnore
    public Alert alert;

    @JsonIgnore
    private Locale locale;

    public LocalizedAlert(Alert alert, Locale locale) {
        this.alert = alert;
        this.locale = locale;
    }

    public LocalizedAlert(){
    }

    @JsonSerialize
    public String getId() {
        return alert.id;
    }

    @JsonSerialize
    public String getAlertHeaderText() {
        if (alert.alertHeaderText == null) {
            return null;
        }
        return alert.alertHeaderText.toString(locale);
    }

    @JsonSerialize
    public String getAlertDescriptionText() {
        if (alert.alertDescriptionText == null) {
            return null;
        }
        return alert.alertDescriptionText.toString(locale);
    }

    @JsonSerialize
    public String getAlertUrl() {
        if (alert.alertUrl == null) {
            return null;
        }
        return alert.alertUrl.toString(locale);
    }

    //null means unknown
    @JsonSerialize
    public Date getEffectiveStartDate() {
        return alert.effectiveStartDate;
    }

    @JsonSerialize
    public Date getEffectiveEndDate() {
        return alert.effectiveEndDate;
    }

    @JsonSerialize
    /** non-standard mercury extension **/
    public Date getCreatedAtDate() { return alert.createdDate; }

    @JsonSerialize
    /** non-standard mercury extension **/
    public Date getUpdatedAtDate() { return alert.updatedAt; }

    @JsonSerialize
    /** non-standard mercury extension **/
    public Date getDisplayBeforeActiveMillis() { return alert.displayBeforeActive; }

    @JsonSerialize
    /** non-standard mercury extension **/
    public String getAlertType() {
        if (alert.alertType == null)
            return null;
        return alert.alertType.toString(locale);
    }

    @JsonSerialize
    /** non-standard mercury extension **/
    public String getHumanReadableActivePeriod() {
        if (alert.humanReadableActivePeriod == null)
            return null;
        return alert.humanReadableActivePeriod.toString(locale);
    }

    @JsonSerialize
    /** non-standard mercury extension **/
    public ArrayList<LocalizedAlertAlternateStation> getStationAlternates() {
        if (alert.stationAlternatives == null)
            return null;
        ArrayList<LocalizedAlertAlternateStation> alternates = new ArrayList<>();
        for (AlertAlternateStation aas : alert.stationAlternatives) {
            LocalizedAlertAlternateStation laas = new LocalizedAlertAlternateStation(aas, locale);
            alternates.add(laas);
        }
        return alternates;
    }

    @JsonSerialize
    /** non-standard mercury extension **/
    public ArrayList<String> getServicePlanNumbers() {
        return alert.servicePlanNumberList;
    }

    @JsonSerialize
    /** non-standard mercury extension **/
    public ArrayList<String> getGeneralOrderNumbers() {
        return alert.generalOrderNumberList;
    }
}
