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
package org.opentripplanner.graph_builder.module;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.Landmark;
import org.opentripplanner.routing.edgetype.LandmarkEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.LandmarkVertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class LandmarksModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(LandmarksModule.class);

    private File landmarksFile;

    public LandmarksModule(File landmarksFile) {
        this.landmarksFile = landmarksFile;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TransitStationStop.class, new JsonDeserializer<TransitStationStop>() {
            @Override
            public TransitStationStop deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                String label = jsonParser.getValueAsString();
                TransitStationStop v =  (TransitStationStop) graph.getVertex(label);
                if (v == null)
                    LOG.warn("no vertex found for label {}", label);
                return v;
            }
        });
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        Landmark[] landmarks = new Landmark[0];
        try {
            landmarks = mapper.readValue(landmarksFile, Landmark[].class);
        } catch (IOException ex) {
            LOG.info("Error reading landmarks file: " + ex);
        }
        for (Landmark landmark : landmarks) {
            removeBadStops(landmark);
            if (landmark.getStops().isEmpty())
                continue;
            graph.addLandmark(landmark);
            Vertex lv = makeLandmarkVertex(graph, landmark);
            for (Vertex stop : landmark.getStops()) {
               	if(stop instanceof TransitStationStop) {
               		TransitStationStop ts = (TransitStationStop)stop;

               		boolean hasEntrances = false;
               		Collection<Stop> stops = graph.index.stopsForParentStation.get(
        					new AgencyAndId(ts.getStopId().getAgencyId(),ts.getStop().getParentStation()));
               		for(Stop entrance : stops) {
        				if(entrance.getLocationType() == Stop.LOCATION_TYPE_ENTRANCE_EXIT) {
        					hasEntrances = true;
        					break;
        				}
        			}
               		
               		if(hasEntrances) {
            			for(Stop entrance : stops) {
            				if(entrance.getLocationType() != Stop.LOCATION_TYPE_ENTRANCE_EXIT)
            					continue;
            				
            				Vertex entranceVertex = graph.getVertex(GtfsLibrary.convertIdToString(entrance.getId()));
                            new LandmarkEdge(lv, entranceVertex);
                            new LandmarkEdge(entranceVertex, lv);
            			}            			
            		} else {
                        new LandmarkEdge(lv, stop);
                        new LandmarkEdge(stop, lv);
            		}
            	}
            }
        }
    }

    private Vertex makeLandmarkVertex(Graph graph, Landmark landmark) {
        List<TransitStationStop> stops = landmark.getStops();
        Comparator<TransitStationStop> latcmp = Comparator.comparingDouble(Vertex::getLat);
        Comparator<TransitStationStop> loncmp =  Comparator.comparingDouble(Vertex::getLon);
        double minLat = Collections.min(stops, latcmp).getLat();
        double maxLat = Collections.max(stops, latcmp).getLat();
        double minLon = Collections.min(stops, loncmp).getLon();
        double maxLon = Collections.max(stops, loncmp).getLon();
        double lat = (minLat + maxLat) / 2;
        double lon = (minLon + maxLon) / 2;
        return new LandmarkVertex(graph, landmark.getName(), lon, lat);
    }

    private void removeBadStops(Landmark landmark) {
        landmark.getStops().removeIf(Objects::isNull);
    }

    @Override
    public void checkInputs() {
        // unused
    }
}