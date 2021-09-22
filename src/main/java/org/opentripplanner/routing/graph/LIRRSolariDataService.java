package org.opentripplanner.routing.graph;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.bind.DatatypeConverter;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.MTAStop;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

public class LIRRSolariDataService {

    private static final Logger _log = LoggerFactory.getLogger(LIRRSolariDataService.class);

    // queue configuration
	public  String ENDPOINT_URL = null;
	
	public  String USERNAME = null;
	
	public  String PASSWORD = null;
	
	public  String TOPIC = null;
	
	// only these stations should have records pulled from the wire
	private HashSet<String> STATION_ID_WHITELIST = null;
	
	private final String[] directionIdMap = new String[] { "E", "W" }; //  0=E, 1=W
	
    public HashMap<T2<String, String>, JsonNode> solariDataByTripAndStop = new HashMap<>();
    
	public HashMap<String, Stop> stopsByStationCode = null;	

	private ActiveMQConnectionFactory connectionFactory = null; 		

    private ObjectMapper objectMapper = new ObjectMapper();
    
    private Graph _graph;

    public LIRRSolariDataService(Graph graph) throws Exception {
    	_graph = graph;

    	if(_graph.routerConfig == null) {
    		_log.error("No configuration found. Exiting.");
    		return;
    	}
    		
    	JsonNode root = objectMapper.readValue(_graph.routerConfig, JsonNode.class);
    	JsonNode config = root.get("LIRRSolariConfig");
    	
    	if(config != null) {
    		ENDPOINT_URL = config.get("url").asText();
    		USERNAME = config.get("username").asText();
    		PASSWORD = config.get("password").asText();
    		TOPIC = config.get("topic").asText();
    		
    		STATION_ID_WHITELIST = new HashSet<String>();
    		Iterator<JsonNode> i = config.get("stationList").elements();
    		while(i.hasNext()) {
    			JsonNode e = i.next();
    			STATION_ID_WHITELIST.add(e.asText());
    		}
    	} else {
    		_log.error("No configuration found for this module. Exiting.");
    		return;
    	}
    	
		_log.info("Started initializing.");

		connectionFactory = new ActiveMQConnectionFactory(USERNAME, PASSWORD, ENDPOINT_URL);   
		
    	Connection connection = connectionFactory.createConnection();
	    connection.start();
	
	    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    Destination destination = session.createTopic(TOPIC);
	
	    ProcessorThread thread = new ProcessorThread(destination, session);
	    thread.setName("LIRR Solari Processor Thread");
	    thread.start();
	    
		_log.info("Finished initializing.");
    }
    
    public class MessageProcessorThread extends Thread {
    	
    	private final JsonNode message;
    	
        public MessageProcessorThread(JsonNode message) {
        	this.message = message; 
        }

        public void run() {
        	if(_graph == null || _graph.index == null || _graph.index.patternsForFeedId == null || stopsByStationCode == null) {
        		_log.trace("Processor thread not yet ready...");
        		return;
        	}
        	
        	_log.debug("Start processing thread");
        
        	int matched = 0;
        	double averageScore = 0f;
        	
        	JsonNode stationLocation = message.get("location");            
        	String stationCode = stationLocation.get("code").asText();
        	Stop stationAsStop = stopsByStationCode.get(stationCode);

        	// scheduled patterns
        	List<TripPattern> rawPatterns = new ArrayList<>(_graph.index.patternsForStop.get(stationAsStop));
        	
        	// patterns added via RT
        	if (_graph.timetableSnapshotSource != null) {
            	_graph.timetableSnapshotSource.purgeExpiredData = true;
            	TimetableSnapshot snapshot = _graph.timetableSnapshotSource.getTimetableSnapshot();
            		
            	if (snapshot != null) {
            		rawPatterns.addAll(snapshot.getTripPatternsForStop(stationAsStop));
                }
            }
        	
        	Set<TripPattern> patterns = rawPatterns.stream().distinct().collect(Collectors.toSet());
        	
        	for(JsonNode train : message.get("trains")) {
        		String trainNumberRaw = train.get("trainNumber").asText();
        		String directionRaw = train.get("direction").asText();
        		
        		Calendar scheduleDateTimeCalendar = DatatypeConverter.parseDateTime(train.get("scheduleDateTime").asText());
        		ServiceDate scheduleDateTimeServiceDate = new ServiceDate(scheduleDateTimeCalendar);
        		
                JsonNode destinationStopRaw = train.get("destinationLocation");
            	String destinationStopCode = destinationStopRaw.get("code").asText();

            	SortedSetMultimap<Float, Trip> bestTrips = TreeMultimap.create(
	            	new Comparator<Float>() {
						@Override
						public int compare(Float o1, Float o2) {
							return o2.compareTo(o1);
						}
	            	}, 
	            	new Comparator<Trip>() {
						@Override
						public int compare(Trip o1, Trip o2) {
							return 0;
						}
	            	});
            	
            	// go through all candidate trips and add some "points" (score) to each option
            	// based on how well it matches what we're looking for--top result wins
                for(TripPattern tripPattern : patterns) {                	
                    Timetable updatedTimeTable = _graph.index.currentUpdatedTimetableForTripPattern(tripPattern);

                    for(Trip t : tripPattern.getTrips()) {
                		float score = 0f;
                		
                		// the train number being inside the trip ID 
                		if(ArrayUtils.contains(t.getId().toString().split(" |_"), trainNumberRaw))
                			continue; // for efficiency
                		score += 40.0f;
                			
                		// direction ID being right--trains can still be on their previous trip if they're late, so we 
                		// don't make this a hard check
                        if(directionRaw.equals(directionIdMap[Integer.parseInt(t.getDirectionId())]))
                			score += 10.0f;
                        
                        int index = 0;
                		for(Stop stop : tripPattern.getStops()) {     
                        	if(!stop.getId().equals(stopsByStationCode.get(stationCode).getId())) {
                        		index++;
                        		continue;
                        	}
                        	
                        	// add a scaled 40% based on how close the departureTime is to the schedule
                			long currentDepartureTime = updatedTimeTable.getTripTimes(t).getScheduledDepartureTime(index);

                			// sometimes different systems use different service dates, so normalize 
                			while(currentDepartureTime > 24 * 60 * 60) {
                				currentDepartureTime -= 24 * 60 * 60;
                			}

                			long scheduledDepartureTime = (scheduleDateTimeCalendar.getTimeInMillis() - scheduleDateTimeServiceDate.getAsDate().getTime())/1000;

                			// sometimes different systems use different service dates, so normalize 
                			while(scheduledDepartureTime > 24 * 60 * 60) {
                				scheduledDepartureTime -= 24 * 60 * 60;
                			}

                        	long diffTime = Math.abs(currentDepartureTime - scheduledDepartureTime);                        	
                        	if(diffTime < 60 * 60) { // 1 hr window       
                        		float increment = (float)(40.0f * (((60f * 60f) - (float)diffTime) / (60f * 60f)));
                        		score += increment;
                        	}
                        	
                      	    break;
                		}
                		
                		// trip destination or origin matching the destination of this Solari message--again, trains can be on their previous
                		// trip still so we check both the first and last stop of the schedule
                		Stop lastStop = tripPattern.getStops().get(tripPattern.getStops().size() - 1);
                		Stop firstStop = tripPattern.getStops().get(0);

                		if(lastStop.getId().equals(stopsByStationCode.get(destinationStopCode).getId()) 
                				|| firstStop.getId().equals(stopsByStationCode.get(destinationStopCode).getId())) {
                			score += 10.0f;                        			
                		}                   
                		
                		bestTrips.put(score, t);
                   } // for each trip
                } // for each serviceDate

                if(bestTrips == null || bestTrips.isEmpty()) {
                	_log.error("No match in the schedule could be found for " + train);
                	continue;
                }

                float matchScore = bestTrips.keys().iterator().next();
                averageScore += matchScore;

                Trip bestTrip = bestTrips.values().iterator().next();

                if(matchScore < 80.0f) {
                	_log.error("Poor match in the schedule for record below; need 80% or higher:");

                	_log.error("RECORD FROM MTA: Train " + trainNumberRaw + " " + 
  	                		directionRaw + " at " + scheduleDateTimeCalendar.getTime() + " to " + destinationStopRaw.get("name"));
  	
  	                for(java.util.Map.Entry<Float, Trip> e : bestTrips.entries()) {
  	                	Trip t = e.getValue();
	                	TripPattern p = _graph.index.patternForTrip.get(t);
	                	List<Stop> stopTimes = p.getStops();
                        Timetable updatedTimeTable = _graph.index.currentUpdatedTimetableForTripPattern(p);
            			ServiceDate sd = updatedTimeTable.serviceDate;
            			if(sd == null)
            				sd = new ServiceDate();

  	                	_log.error(" " + e.getKey() + " Trip " + t.getId() + " " + t.getDirectionId() 
  	                			+ "(" + directionIdMap[Integer.parseInt(t.getDirectionId())] + ") to " 
  	                			+ stopTimes.get(stopTimes.size() - 1).getName() + " at "
	                			+ sd.getAsDate().toInstant()
	                				.plus(updatedTimeTable.getTripTimes(t).getArrivalTime(stopTimes.size() - 1),ChronoUnit.SECONDS).atZone(ZoneId.of("America/New_York")));
  	                }

  	                // fall through--we still want this trip to be in the list, but not associated with a 
  	                // trip that doesn't represent it well
  	                bestTrip = null;
  	            } else 
  	                matched++;
                
                T2<String, String> key = new T2<String, String>(
                		bestTrip != null ? AgencyAndId.convertToString(bestTrip.getId()) : "LI_TRAIN_NO_" + trainNumberRaw, 
                		AgencyAndId.convertToString(stationAsStop.getId()));
                                
                solariDataByTripAndStop.put(key, train);

                if(_log.isDebugEnabled()) {
	                _log.debug("RECORD FROM MTA: " + trainNumberRaw + " " + 
	                		directionRaw + " " + scheduleDateTimeCalendar.getTime() + " " + destinationStopRaw.get("name"));
	
	                for(java.util.Map.Entry<Float, Trip> e : bestTrips.entries()) {
	                	Trip t = e.getValue();
	                	TripPattern p = _graph.index.patternForTrip.get(t);
	                	List<Stop> stopTimes = p.getStops();
                        Timetable updatedTimeTable = _graph.index.currentUpdatedTimetableForTripPattern(p);
            			ServiceDate sd = updatedTimeTable.serviceDate;
            			if(sd == null)
            				sd = new ServiceDate();

	                	_log.debug(" " + e.getKey() + " " + t.getId() + " " + t.getDirectionId() 
	                			+ "(" + directionIdMap[Integer.parseInt(t.getDirectionId())] + ") " 
	                			+ stopTimes.get(stopTimes.size() - 1).getName() + " at " 
	                			+ sd.getAsDate().toInstant()
	                				.plus(updatedTimeTable.getTripTimes(t).getArrivalTime(stopTimes.size() - 1),ChronoUnit.SECONDS).atZone(ZoneId.of("America/New_York")));
	                }
                }
        	} // for each train

        	_log.debug("End processing thread");

            _log.info("Found {} Solari trip messages for LIRR station {}, {} matched to schedule. Average match score is {} %", message.get("trains").size(), stationCode, matched, (float)(averageScore/message.get("trains").size()));
        }
    	
    }
    
    public class ProcessorThread extends Thread {
    	
        private MessageConsumer consumer = null;
        
        public ProcessorThread(Destination destination, Session session) throws JMSException {
        	this.consumer = session.createConsumer(destination);   
        }

        public void run() {
        	  while(true) {
          		// lazy initialize maps
              	if(stopsByStationCode == null && _graph.index != null && _graph.index.stopForId != null) {
              		stopsByStationCode = new HashMap<>();
              		_graph.index.stopForId.values().stream()
              				.map(it -> {
              					MTAStop s = it.getExtension(MTAStop.class);
              					return new T2<String, Stop>(s.stopCode, it);
              				})
              				.forEach(s -> {
              					stopsByStationCode.put(s.first, s.second);
              				});
              	}

            	try {
	  	        	Message message = consumer.receive();
	  	        	if(message != null) {
	  	        		String rawJson = ((TextMessage)message).getText();
	  	        		JsonNode jsonMessage = objectMapper.readValue(rawJson, JsonNode.class);	  	        	   	

	  	        		JsonNode stationLocation = jsonMessage.get("location");
	  	        		String stationCode = stationLocation.get("code").asText();
	  	        		if(!STATION_ID_WHITELIST.contains(stationCode))
	  	        			continue;
	  	          	
	  	        		Thread t = new MessageProcessorThread(jsonMessage);
	  	        		t.setName("Message processor thread for LIRR Solari");
	  	        		t.run();
	  	        	}	        	  	        
            	} catch(Exception e) {
            		e.printStackTrace();
            	} 

            }
        }
         
	}
}
