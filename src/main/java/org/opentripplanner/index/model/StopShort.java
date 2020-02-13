package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.opentripplanner.api.model.alertpatch.LocalizedAlert;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class StopShort {

    public AgencyAndId id;
    public String code;
    public String name;
    public double lat;
    public double lon;
    public String url;
    public String cluster;
    public int locationType;
    public List<LocalizedAlert> alerts;
    public String desc;
    public int wheelchairBoarding;
  
    /** Distance to the stop when it is returned from a location-based query. */
    @JsonInclude(Include.NON_NULL) public Integer dist;

    /** Geometry of the walking path to the stop, if returned from a search-based query */
    @JsonInclude(Include.NON_NULL) public EncodedPolylineBean geometry;

    /** Walking time, if returned from a search-based query */
    @JsonInclude(Include.NON_NULL) public Long walkTime;

    /** OSM Way ID this stop is linked to, if returning debug information. */
    @JsonInclude(Include.NON_NULL) public Long wayId;

    /** distance to OSM way */
    @JsonInclude(Include.NON_NULL) public Double distance;

    public StopShort (Stop stop) {
        id = new AgencyAndId(stop.getId().getAgencyId(), stop.getId().getId()); // copy
        code = stop.getCode();
        name = stop.getName();
        lat = stop.getLat();
        lon = stop.getLon();
        url = stop.getUrl();
        cluster = stop.getParentStation(); // TODO harmonize these names, maybe use "station" everywhere
        locationType = stop.getLocationType();
        desc = stop.getDesc();
        wheelchairBoarding = stop.getWheelchairBoarding();
    }

    /** @param distance in integral meters, to avoid serializing a bunch of decimal places. */
    public StopShort(Stop stop, int distance) {
        this(stop);
        this.dist = distance;
    }

    public static List<StopShort> list (Collection<Stop> in) {
        List<StopShort> out = Lists.newArrayList();
        for (Stop stop : in) out.add(new StopShort(stop));
        return out;
    }    

}
