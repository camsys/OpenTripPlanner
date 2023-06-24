package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ApiAlert {
    public String alertId;
    public String alertHeaderText;
    public String alertDescriptionText;
    public String alertUrl;

    public String alertType;
    public List<String> sortOrders = new ArrayList<>();

    /** null means unknown */
    public Date effectiveStartDate;
    public Date effectiveEndDate;
    public String consequenceMessage;
}
