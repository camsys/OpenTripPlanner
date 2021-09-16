package org.opentripplanner.index.model;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.model.VehicleInfo;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.JsonNode;

import static org.opentripplanner.util.DateUtils.formatDateIso;

public class TripTimeShort {

    public static final int UNDEFINED = -1;

    /** Stop of this arrival/departure */
    public AgencyAndId stopId;

    /** Name of stop */
    public String stopName;

    /** stop latitude */
    public double stopLat;

    /** stop longitude */
    public double stopLon;

    /** Index of this stop in the trip */
    public int stopIndex;

    /** Total number of stops in the trip */
    public int stopCount;

    /** scheduled arrival time, in seconds after midnight of the service day */
    public int scheduledArrival = UNDEFINED ;

    /** scheduled departure time, in seconds after midnight of the service day */
    public int scheduledDeparture = UNDEFINED ;

    /** realtime arrival time, in seconds after midnight of the service day, if realtime=true */
    public int realtimeArrival = UNDEFINED ;

    /** realtime departure time, in seconds after midnight of the service day, if realtime=true */
    public int realtimeDeparture = UNDEFINED ;

    /** realtime arrival delay on the trip, in seconds, if realtime=true */
    public int arrivalDelay = UNDEFINED ;

    /** realtime departure delay on the trip, in seconds, if realtime=true */
    public int departureDelay = UNDEFINED ;

    /** whether this stop is marked as a timepoint in GTFS */
    public boolean timepoint = false;

    /** true if there is realtime data for this arrival/departure; otherwise false. */
    public boolean realtime = false;

    /** If this arrival/departure comes from realtime, the relationship of the TripUpdate to static GTFS schedules */
    public RealTimeState realtimeState = RealTimeState.SCHEDULED ;

    /** service day, in UNIX epoch time */
    public long serviceDay;

    /** trip of arrival/departure */
    public AgencyAndId tripId;

    /** block of arrival/departure */
    public String blockId;

    /** Headsign associated with this trip. */
    public String tripHeadsign;

    /** arrival time in ISO-8601 format, in timezone of router. Realtime if available.*/
    public String arrivalFmt;

    /** departure time in ISO-8601 format, in timezone of router. Realtime if available.*/
    public String departureFmt;

    /** Headsign associated with this stop-time, if given in GTFS. */
    public String stopHeadsign;

    /** track number, if available */
    public String track;

    /** track number, if available */
    public Integer peakOffpeak;

    /** Pattern information for this stop-time */
    public PatternShort pattern;

    /** time the realtime information was updated (given by the realtime source), if realtimeState != SCHEDULED */
    public Long timestamp = null;

    /** direction ID for the trip */
    public String directionId;

    /** VehicleInfo for trip (if in realtime) */
    public VehicleInfo vehicleInfo;

    /** Optional: all stops for this trip, if indicated in API request */
    public List<StopShort> stopsForTrip;

    /** realtime sign text, if given in data */
    public String realtimeSignText;

    /** indicate whether or not a fare card, e.g. OMNY, can be used on trip */
    public boolean regionalFareCardAccepted = false;

    /**
     * This is stop-specific, so the index i is a stop index, not a hop index.
     */
    public TripTimeShort(TripPattern tripPattern, TripTimes tt, int i, Stop stop) {
        stopId = stop.getId();
        stopName           = stop.getName();
        stopLat            = stop.getLat();
        stopLon            = stop.getLon();
        regionalFareCardAccepted = stop.getRegionalFareCardAccepted() != 0;
        stopIndex          = i;
        stopCount          = tt.getNumStops();
        scheduledArrival   = tt.getScheduledArrivalTime(i);
        realtimeArrival    = tt.getArrivalTime(i);
        arrivalDelay       = tt.getArrivalDelay(i);
        scheduledDeparture = tt.getScheduledDepartureTime(i);
        realtimeDeparture  = tt.getDepartureTime(i);
        departureDelay     = tt.getDepartureDelay(i);
        timepoint          = tt.isTimepoint(i);
        realtime           = !tt.isScheduled();
        realtimeState      = tt.getRealTimeState();
        blockId            = tt.trip.getBlockId();
        tripHeadsign       = tt.trip.getTripHeadsign();
        stopHeadsign       = tt.hasStopHeadsigns() ? tt.getHeadsign(i) : null;
        track              = tt.getTrack(i);
        peakOffpeak        = tt.trip.getPeakOffpeak();
        pattern            = new PatternShort(tripPattern);
        timestamp          = tt.getRealTimeState().equals(RealTimeState.SCHEDULED) ? null : tt.getTimestamp();
        directionId        = tt.trip.getDirectionId();
        realtimeSignText   = tt.getRealtimeSignText(i);
        // use final stop if no trip_headsign
        if (tripHeadsign == null) {
            tripHeadsign = tripPattern.getStop(tripPattern.getStops().size() - 1).getName();
        }
    }

    public TripTimeShort(JsonNode solariPacket, GraphIndex index) { 
    	String destinationLocationCode = solariPacket.get("destinationLocation").get("code").asText();
    	Stop stop = index.lirrSolari.stopsByStationCode.get(destinationLocationCode);

    	this.stopId 			= stop.getId();
        this.stopName           = stop.getName();
        this.stopLat            = stop.getLat();
        this.stopLon            = stop.getLon();
        
        Calendar scheduledDeparture = javax.xml.bind.DatatypeConverter.parseDateTime(solariPacket.get("scheduleDateTime").asText());
        this.scheduledDeparture = (int)(scheduledDeparture.getTimeInMillis()/1000);
        
        Calendar predictedDeparture = javax.xml.bind.DatatypeConverter.parseDateTime(solariPacket.get("predictedDateTime").asText());
        this.realtimeDeparture = (int)(predictedDeparture.getTimeInMillis()/1000);

        this.tripId = new AgencyAndId("LI", "TRAIN_NO_" + solariPacket.get("trainNumber").asText());
        this.tripHeadsign = solariPacket.get("destinationLocation").get("name").asText();
        this.realtime = true;
        this.track = solariPacket.get("track").asText();
        this.peakOffpeak = solariPacket.get("peakCode").asText().equals("P") ? 1 : 0;
        this.realtimeSignText = solariPacket.get("status").asText();
    }
    
    public TripTimeShort(TripPattern tripPattern, TripTimes tt, int i, Stop stop, ServiceDay sd, TimeZone tz) {
        this(tripPattern, tt, i, stop, sd, tz, false);
    }

    public TripTimeShort(TripPattern tripPattern, TripTimes tt, int i, Stop stop, ServiceDay sd, TimeZone tz, boolean includeStopsForTrip) {
        this(tripPattern, tt, i, stop, sd == null ? -1 : sd.time(0), tz, includeStopsForTrip);
    }

    public TripTimeShort(TripPattern tripPattern, TripTimes tt, int i, Stop stop, long serviceDay, TimeZone tz, boolean includeStopsForTrip) {
        this(tripPattern, tt, i, stop);
        if (serviceDay < 0) {
            return;
        }
        tripId = tt.trip.getId();
        this.serviceDay = serviceDay;
        if (realtimeArrival != TripTimes.UNAVAILABLE || realtimeState.equals(RealTimeState.CANCELED)) {
            long arrival = realtimeArrival != TripTimes.UNAVAILABLE ? realtimeArrival : scheduledArrival;
            arrivalFmt = formatDateIso(serviceDay + arrival, tz);
        }
        if (realtimeDeparture != TripTimes.UNAVAILABLE || realtimeState.equals(RealTimeState.CANCELED)) {
            long departure = realtimeDeparture != TripTimes.UNAVAILABLE ? realtimeDeparture : scheduledDeparture;
            departureFmt = formatDateIso(serviceDay + departure, tz);
        }
        if (includeStopsForTrip) {
            stopsForTrip = StopShort.list(tripPattern.getStops());
        }
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip) {
        return fromTripTimes(table, trip, -1, null);
    }

    public static List<TripTimeShort> fromTripTimes(Timetable table, Trip trip, long serviceDay, TimeZone tz) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));
        List<TripTimeShort> out = Lists.newArrayList();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(table.pattern, times, i, table.pattern.getStop(i), serviceDay, tz, false));
        }
        return out;
    }

    public int getRealtimeArrival() {
        return realtimeArrival;
    }

    public int getRealtimeDeparture() {
        return realtimeDeparture;
    }

    public void setRegionalFareCardAccepted(boolean regionalFareCardAccepted) {
        this.regionalFareCardAccepted = regionalFareCardAccepted;
    }
    
    public boolean equals(Object o) {
        return (o instanceof TripTimeShort) && ((TripTimeShort)o).tripId.equals(this.tripId);
    }

    public int hashCode() {
        return tripId.hashCode();
    }
}
