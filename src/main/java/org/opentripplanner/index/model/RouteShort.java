package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.gtfs.GtfsLibrary;

import com.beust.jcommander.internal.Lists;
import org.opentripplanner.routing.core.TraverseMode;

public class RouteShort {

    /** ID of this route */
    public AgencyAndId id;

    /** Name of route, if given in GTFS. Typically, this name is what user interfaces will display.*/
    public String shortName;

    /** Longer name of route, if given in GTFS */
    public String longName;

    /** Mode of route */
    public String mode;

    /** Color for display, if given in GTFS */
    public String color;

    /** Agency this route is associated with in GTFS. */
    public String agencyName;

    /** use this parameter for bannedRoutes, preferredRoutes, etc in the /plan call */
    public String paramId;

    /** sort order parameter */
    public int sortOrder;

    /** route type */
    public int routeType;

    public boolean regionalFareCardAccepted;

    public RouteShort (Route route) {
        id = route.getId();
        shortName = route.getShortName();
        longName = route.getLongName();
        mode = GtfsLibrary.getTraverseMode(route).toString();
        color = route.getColor();
        agencyName = route.getAgency().getName();
        paramId = id.getAgencyId() + "__" + id.getId();
        if (route.getSortOrder() >= 0)
            sortOrder = route.getSortOrder();
        routeType = route.getType();
        if (GtfsLibrary.getTraverseMode(route) == TraverseMode.BUS) {
            regionalFareCardAccepted = route.getRegionalFareCardAccepted() != 0;
        }
    }

    public static List<RouteShort> list (Collection<Route> in) {
        List<RouteShort> out = Lists.newArrayList();
        for (Route route : in) out.add(new RouteShort(route));
        return out;
    }

}
