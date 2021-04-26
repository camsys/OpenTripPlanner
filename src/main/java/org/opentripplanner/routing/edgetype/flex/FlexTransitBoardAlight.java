package org.opentripplanner.routing.edgetype.flex;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.routing.vertextype.flex.TemporaryTransitStop;

public class FlexTransitBoardAlight extends TransitBoardAlight implements TemporaryEdge {

    // normalized to [0, 1]
    private double startIndex;
    private double endIndex;
    private PartialPatternHop hop;

    public FlexTransitBoardAlight(TransitStopDepart fromStopVertex, PatternStopVertex toPatternVertex,
                                       int stopIndex, PartialPatternHop hop) {
        super(fromStopVertex, toPatternVertex, stopIndex, hop.getMode());
        setIndices(hop);
    }

    public FlexTransitBoardAlight(PatternStopVertex fromPatternStop, TransitStopArrive toStationVertex,
                                       int stopIndex, PartialPatternHop hop) {
        super(fromPatternStop, toStationVertex, stopIndex, hop.getMode());
        setIndices(hop);
    }

    private void setIndices(PartialPatternHop hop) {
        if (hop.getOriginalHopLength() > 0) {
            this.startIndex = hop.getStartIndex() / hop.getOriginalHopLength();
            this.endIndex = hop.getEndIndex() / hop.getOriginalHopLength();
        } else {
            // entirely-deviated area hop. Never add entire path time.
            this.startIndex = 0.0d;
            this.endIndex = 0.0d;
        }
        this.hop = hop;
    }

    @Override
    public State traverse(State s0) {
        // do not board call-n-ride if it is not a temporary stop and we aren't doing a fixed route-C&R transfer
        if (!s0.getOptions().arriveBy && boarding && hop.isDeviatedRouteBoard()
            && !(checkCallAndRideBoardAlightOkDepart(s0, (TransitStopDepart) getFromVertex()))) {
            return null;
        }

        if (s0.getOptions().arriveBy && !boarding && hop.isDeviatedRouteAlight()
            && !(checkCallAndRideBoardAlightOkArrive(s0, (TransitStopArrive) getToVertex()))) {
            return null;
        }

        return super.traverse(s0);
    }

    public TripTimes getNextTrip(State s0, ServiceDay sd, Timetable timetable) {
        if (hop.isUnscheduled()) {
            RoutingRequest options = s0.getOptions();
            int time = (int) Math.round(hop.timeLowerBound(options));
            return timetable.getNextCallNRideTrip(s0, sd, getStopIndex(), boarding, time);
        }
        double adjustment = boarding ? startIndex : -1 * (1 - endIndex);
        return timetable.getNextTrip(s0, sd, getStopIndex(), boarding, adjustment, hop.getStartVehicleTime(), hop.getEndVehicleTime());
    }



    public int calculateWait(State s0, ServiceDay sd, TripTimes tripTimes) {
        if (hop.isUnscheduled()) {
            int currTime = sd.secondsSinceMidnight(s0.getTimeSeconds());
            boolean useClockTime = !s0.getOptions().flexIgnoreDrtAdvanceBookMin;
            long clockTime = s0.getOptions().clockTimeSec;
            if (boarding) {
                int scheduledTime = getCallAndRideBoardTime(tripTimes, getStopIndex(), currTime, (int) hop.timeLowerBound(s0.getOptions()), sd, useClockTime, clockTime);
                if (scheduledTime < 0)
                    throw new IllegalArgumentException("Unexpected bad wait time");
                return (int) (sd.time(scheduledTime) - s0.getTimeSeconds());
            } else {
                int scheduledTime = getCallAndRideAlightTime(tripTimes, getStopIndex(), currTime, (int) hop.timeLowerBound(s0.getOptions()), sd, useClockTime, clockTime);
                if (scheduledTime < 0)
                    throw new IllegalArgumentException("Unexpected bad wait time");
                return (int) (s0.getTimeSeconds() - (sd.time(scheduledTime)));
            }
        }
        int stopIndex = getStopIndex();
        if (boarding) {
            int startVehicleTime = hop.getStartVehicleTime();
            if (startVehicleTime != 0) {
                //TODO getDemandResponseMaxTime for RTD Flex currently using directTime
                //startVehicleTime = tripTimes.getDemandResponseMaxTime(startVehicleTime);
                startVehicleTime = startVehicleTime;
            }
            int offset = (int) Math.round(startIndex * (tripTimes.getRunningTime(stopIndex)));
            return  (int)(sd.time(tripTimes.getDepartureTime(stopIndex) + offset - startVehicleTime) - s0.getTimeSeconds());
        }
        else {
            int endVehicleTime = hop.getEndVehicleTime();
            if (endVehicleTime != 0) {
                //TODO getDemandResponseMaxTime for RTD Flex currently using directTime
                //endVehicleTime = tripTimes.getDemandResponseMaxTime(endVehicleTime);
                endVehicleTime = endVehicleTime;
            }
            int offset = (int) Math.round((1-endIndex) * (tripTimes.getRunningTime(stopIndex - 1)));
            return (int)(s0.getTimeSeconds() - sd.time(tripTimes.getArrivalTime(stopIndex) - offset + endVehicleTime));
        }
    }


    public long getExtraWeight(RoutingRequest options) {
        boolean deviatedRoute = (boarding && hop.isDeviatedRouteBoard()) || (!boarding && hop.isDeviatedRouteAlight());
        return (deviatedRoute ? options.flexDeviatedRouteExtraPenalty : options.flexFlagStopExtraPenalty);
    }


    public boolean isDeviated() {
        return boarding ? hop.isDeviatedRouteBoard() : hop.isDeviatedRouteAlight();
    }

    @Override
    public String toString() {
        return "FlexTransitBoardAlight(" +
                (boarding ? "boarding " : "alighting ") +
                getFromVertex() + " to " + getToVertex() + ")";
    }

    @Override
    public void dispose() {
        tov.removeIncoming(this);
        fromv.removeOutgoing(this);
    }

    // We want to avoid a situation where results look like
    // 1) call-and-ride from A to B
    // 2) call-and-ride from A to real transit stop right next to B, walk to B
    public boolean checkCallAndRideBoardAlightOkDepart(State s0, TransitStopDepart transitStop) {
        return checkCallAndRideBoardAlightOk(s0, transitStop.getStopVertex());
    }

    public boolean checkCallAndRideBoardAlightOkArrive(State s0, TransitStopArrive transitStop) {
        return checkCallAndRideBoardAlightOk(s0, transitStop.getStopVertex());
    }

    public boolean checkCallAndRideBoardAlightOk(State s0, TransitStop transitStop) {
        RoutingContext rctx = s0.getOptions().rctx;
        if (transitStop == rctx.fromVertex || transitStop == rctx.toVertex) {
            return true;
        }
        if (!s0.isEverBoarded()) {
            return false;
        }
        // only allow call-n-ride transfers at the same stop
        return s0.getPreviousStop().equals(transitStop.getStop());
    }

    public int getCallAndRideBoardTime(TripTimes tt, int stop, long currTime, int directTime, ServiceDay sd, boolean useClockTime, long startClockTime) {
        //TODO getDemandResponseMaxTime for RTD Flex currently using directTime
        //int travelTime = getDemandResponseMaxTime(directTime);
        int travelTime = directTime;
        int minBoardTime = tt.getArrivalTime(stop + 1) - travelTime;
        int ret = (int) Math.min(Math.max(currTime, tt.getDepartureTime(stop)), minBoardTime);
        if (useClockTime) {
            int clockTime = (int) (sd.secondsSinceMidnight(startClockTime) + Math.round(tt.trip.getDrtAdvanceBookMin() * 60.0));
            if (ret >= clockTime) {
                return ret;
            } else if (clockTime < minBoardTime) {
                return clockTime;
            } else {
                return -1;
            }
        }
        return ret;
    }

    public int getCallAndRideAlightTime(TripTimes tt, int stop, long currTime, int directTime, ServiceDay sd, boolean useClockTime, long startClockTime) {
        //TODO getDemandResponseMaxTime for RTD Flex currently using directTime
        //int travelTime = getDemandResponseMaxTime(directTime);
        int travelTime = directTime;
        int maxAlightTime = tt.getDepartureTime(stop - 1) + travelTime;
        int ret = (int) Math.max(Math.min(currTime, tt.getArrivalTime(stop)), maxAlightTime);
        if (useClockTime) {
            int clockTime = (int) (sd.secondsSinceMidnight(startClockTime) + Math.round(tt.trip.getDrtAdvanceBookMin() * 60.0));
            // boarding time must be > clockTime
            int boardTime = ret - travelTime;
            if (boardTime >= clockTime) {
                return ret;
            }
            ret += (clockTime - boardTime);
            if (ret >= maxAlightTime) {
                return -1;
            }
        }
        return ret;
    }

    public boolean checkCallAndRideStreetLinkOk(State s0, TransitStop transitStop) {
        RoutingContext rctx = s0.getOptions().rctx;
        return transitStop == rctx.fromVertex || transitStop == rctx.toVertex;
    }
}
