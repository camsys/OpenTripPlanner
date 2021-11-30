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

package org.opentripplanner.updater.stoptime;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtimeExtensions;
import com.google.transit.realtime.GtfsRealtimeNYCT;
import com.google.transit.realtime.GtfsRealtimeNYCT.NyctTripDescriptor;

import org.opentripplanner.updater.JsonConfigurable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class GtfsRealtimeHttpTripUpdateSource implements TripUpdateSource, JsonConfigurable {
    private static final Logger LOG =
            LoggerFactory.getLogger(GtfsRealtimeHttpTripUpdateSource.class);

    private static final ExtensionRegistry _extensionRegistry;

    /**
     * True iff the last list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private boolean fullDataset = true;

    /**
     * Feed id that is used to match trip ids in the TripUpdates
     */
    private String feedId;

    private String url;

    private long timestamp;
    
    private Graph graph;

    private boolean matchStopSequence;

    static {
        _extensionRegistry = ExtensionRegistry.newInstance();
        GtfsRealtimeExtensions.registerExtensions(_extensionRegistry);
    }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }
        this.url = url;
        this.feedId = config.path("feedId").asText();
        this.matchStopSequence = config.path("matchStopSequence").asBoolean(true);
        this.graph = graph;
    }

    @Override
    public List<TripUpdate> getUpdates() {
        FeedMessage feedMessage = null;
        List<FeedEntity> feedEntityList = null;
        List<TripUpdate> updates = null;
        fullDataset = true;
        long start = System.currentTimeMillis();
        try {
            InputStream is = HttpUtils.getData(url);
            if (is != null) {
                // Decode message
                feedMessage = FeedMessage.PARSER.parseFrom(is, _extensionRegistry);
                feedEntityList = feedMessage.getEntityList();

                // Change fullDataset value if this is an incremental update
                if (feedMessage.hasHeader()
                        && feedMessage.getHeader().hasIncrementality()
                        && feedMessage.getHeader().getIncrementality()
                        .equals(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL)) {
                    fullDataset = false;
                }

                timestamp = feedMessage.getHeader().getTimestamp();

                // Create List of TripUpdates
                updates = new ArrayList<TripUpdate>(feedEntityList.size());

                int droppedTripUpdates = 0;
                int totalTripUpdates = 0;
                nextFE:
                for (FeedEntity feedEntity : feedEntityList) {                	
                	if (feedEntity.hasTripUpdate()) {
                	
                		if(feedEntity.getTripUpdate().getTrip().hasExtension(GtfsRealtimeNYCT.nyctTripDescriptor)) {
                			NyctTripDescriptor td = 
                    			feedEntity.getTripUpdate().getTrip().getExtension(GtfsRealtimeNYCT.nyctTripDescriptor);
                    	
                			if(td.getIsAssigned() == false) {
                				LOG.debug("Here for unassigned trip described by " + feedEntity.getTripUpdate().getTrip().getTripId().replace("\r|\n", ""));
                				
                				// create a hash map of the terminal plus 4 stops after which we want to show for any
                				// unassigned trips
                				HashMap<String, StopTimeUpdate> stopsToShow = new HashMap<>();
                				List<StopTimeUpdate> stops = feedEntity.getTripUpdate().getStopTimeUpdateList();                				
                				for(int i = 0; i < Math.min(stops.size(), 5); i++) { // show 4 stops after terminal
                					StopTimeUpdate s = stops.get(i);
                					stopsToShow.put(s.getStopId(), s);
                				}

                				LOG.debug("Terminal plus stops = " + stopsToShow.keySet());
                				
                				// filter the stop time updates to just include STUs for the terminal plus those 4 stops
                				TripUpdate tripUpdate = feedEntity.getTripUpdate();
                				TripUpdate.Builder newTripUpdate = TripUpdate.newBuilder();
                				newTripUpdate.setDelay(tripUpdate.getDelay());
                				newTripUpdate.setTimestamp(tripUpdate.getTimestamp());
                				newTripUpdate.setTrip(tripUpdate.getTrip());
                				                				
                				for(StopTimeUpdate stu : tripUpdate.getStopTimeUpdateList()) {
                					// is this update for the origin terminal?
                					if(stops.get(0).getStopId().equals(stu.getStopId())) {
                						long departureTime = stu.getDeparture().getTime();
                						long now = timestamp;

                        				LOG.debug("Departure at terminal " + stops.get(0).getStopId() + " happens at " + departureTime + ". Now=" + now + " delta=" + (now - departureTime));

                						// if the train was supposed to leave the terminal > 5 minutes ago, skip this TU
                						if(now - departureTime > 5 * 60) {
                							LOG.debug("skipping because departure is 5+ min old");
                							droppedTripUpdates++;
                							continue nextFE;
                						}
                					}                					
                					
                					if(stopsToShow.keySet().contains(stu.getStopId()))
                						newTripUpdate.addStopTimeUpdate(stu);
                				}
                				
                				TripUpdate builtNewTripUpdate = newTripUpdate.build();
                				
                				// no stops made it through
                				if(builtNewTripUpdate.getStopTimeUpdateList().isEmpty()) {
        							LOG.debug("Skipping update for trip " + td + " because no stop times were filtered through.");
                					continue;
                				} else {		
        							LOG.debug("Pushing update for trip " + td + " to update list with terminal plus up to 4 stops; total=" + builtNewTripUpdate.getStopTimeUpdateCount() + " stops");
                					updates.add(builtNewTripUpdate);
                					totalTripUpdates++;
                				}
                			}
                		}
                		
                		updates.add(feedEntity.getTripUpdate());
                	} // if has tripupdate
                }// for feedentity
                
            	LOG.info("Dropped StopUpdate total because of unassigned status = " + droppedTripUpdates + ", passed count = " + totalTripUpdates);
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse gtfs-rt feed from " + url + ":", e);
        } finally {

        	long end = System.currentTimeMillis();
            LOG.info("Feed " + this.feedId + " downloaded in " + (end - start) + "ms via url=" + url);
        }
        return updates;
    }

    @Override
    public boolean getFullDatasetValueOfLastUpdates() {
        return fullDataset;
    }

    public String toString() {
        return "GtfsRealtimeHttpUpdateStreamer(" + url + ")";
    }

    @Override
    public String getFeedId() {
        return this.feedId;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean getMatchStopSequence() {
        return matchStopSequence;
    }
}
