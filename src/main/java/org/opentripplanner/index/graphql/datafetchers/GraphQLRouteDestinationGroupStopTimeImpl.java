package org.opentripplanner.index.graphql.datafetchers;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.gtfs.model.*;
import org.opentripplanner.index.graphql.GraphQLRequestContext;
import org.opentripplanner.index.graphql.generated.GraphQLDataFetchers;
import org.opentripplanner.index.model.EquipmentShort;
import org.opentripplanner.index.model.StopTimesByRouteAndHeadsign;
import org.opentripplanner.index.model.StopTimesByStop;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.standalone.Router;

public class GraphQLRouteDestinationGroupStopTimeImpl implements GraphQLDataFetchers.GraphQLRouteDestinationGroupStopTime {

	@Override
	public DataFetcher<Integer> stopIndex() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.stopIndex;
		    };
	}

	@Override
	public DataFetcher<Integer> stopCount() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.stopCount;
		    };
	}

	@Override
	public DataFetcher<Object> serviceDay() {
		return environment -> {
			TripTimeShort e = environment.getSource();
			return e.serviceDay;
		};
	}

	@Override
	public DataFetcher<String> serviceDayFormatted() {
		DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
		return environment -> {
			TripTimeShort e = environment.getSource();
			return new DateTime(e.serviceDay * 1000).toString(fmt);
		};
	}

	@Override
	public DataFetcher<Object> scheduledArrival() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.serviceDay + e.scheduledArrival;
		    };
	}

	@Override
	public DataFetcher<Object> scheduledDeparture() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.serviceDay + e.scheduledDeparture;
		    };
	}

	@Override
	public DataFetcher<Object> realtimeArrival() {
		return environment -> {
			TripTimeShort e = environment.getSource();
			return e.serviceDay + e.realtimeArrival;
		};
	}

	@Override
	public DataFetcher<Object> realtimeDeparture() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.serviceDay + e.realtimeDeparture;
		    };
	}

	@Override
	public DataFetcher<Integer> arrivalDelay() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.arrivalDelay;
		    };
	}

	@Override
	public DataFetcher<Integer> departureDelay() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.departureDelay;
		    };
	}

	@Override
	public DataFetcher<Boolean> timepoint() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.timepoint;
		    };
	}

	@Override
	public DataFetcher<Boolean> realtime() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.realtime;
		    };
	}

	@Override
	public DataFetcher<String> tripId() {
		return environment -> {
			TripTimeShort e = environment.getSource();
			return AgencyAndId.convertToString(e.tripId);
		};
	}

	@Override
	public DataFetcher<Object> trip() {
		return environment -> {
			TripTimeShort e = environment.getSource();
			AgencyAndId aid = e.tripId;
			return getGraphIndex(environment).tripForId.get(aid);
		};
	}

	@Override
	public DataFetcher<String> track() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.track;
		    };
	}

	@Override
	public DataFetcher<Integer> peakOffpeak() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.peakOffpeak;
		    };
	}

	@Override
	public DataFetcher<Object> vehicleInfo() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.vehicleInfo;
		    };
	}

	@Override
	public DataFetcher<String> realtimeSignText() {
		 return environment -> {
			TripTimeShort e = environment.getSource();
		    	return e.realtimeSignText;
		    };
	}

	@Override
	public DataFetcher stopsForTrip() {
		 return environment -> {
			Map<String, Object> localContext = new HashMap<String, Object>();

			TripTimeShort e = environment.getSource();
			
			Trip t = getGraphIndex(environment).getTripForId(e.tripId);			
			if(t == null)
				throw new Exception("Trip " + e.tripId + " not found.");

			TripPattern pattern = getGraphIndex(environment).getTripPatternForTripId(t.getId());			
			Stop s = pattern.getStops().get(e.stopIndex);
			if(s == null)
				throw new Exception("Stops on pattern " + t + " not found (index=" + e.stopIndex + ")");
			
			localContext.put("stop", s); // boarding stop
			localContext.put("trip", t); // trip
			
			List<Stop> data = new ArrayList<Stop>();
			for(int i = 0; i < pattern.getStops().size(); i++) {
				Stop stop = pattern.getStops().get(i);
				if (e.stopId.equals(stop.getId()) || pattern.stopPattern.dropoffs[i] != StopPattern.PICKDROP_NONE) {
					data.add(stop);
				}
			}
			
			return DataFetcherResult.newResult()
					.data(data)
					.localContext(localContext)
					.build();
		 }; 
	}
	
	private GraphIndex getGraphIndex(DataFetchingEnvironment environment) {
		return environment.<GraphQLRequestContext>getContext().getIndex();
	}

}
