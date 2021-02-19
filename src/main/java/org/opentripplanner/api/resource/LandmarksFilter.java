package org.opentripplanner.api.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import java.io.IOException;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LandmarksFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LandmarksFilter.class);

    public String[] testLoc(Response response, String builderConfig) throws ParseException {
        String fromPlace;
        String toPlace;
        String[] locationUpdate = {null, null};
        fromPlace = response.requestParameters.get("fromPlace").replaceAll(","," ");
        toPlace = response.requestParameters.get("toPlace").replaceAll(","," ");
        JsonNode landmarksTree;
        try {
            ObjectMapper mapper = new ObjectMapper();
            landmarksTree = mapper.readTree(builderConfig).get("landmarksFilter");
            LOG.info("Landmarks filter read in routeConfig");
        } catch (IOException ioe) {
            LOG.info("Landmarks filter couldn't read in routeConfig: ", ioe);
            return null;
        } catch (NullPointerException npe) {
            LOG.info("Landmarks filter received null pointer exception: ", npe);
            return null;
        }
        Iterator<JsonNode> landmarksIterator = landmarksTree.iterator();
        WKTReader reader = new WKTReader();
        while (landmarksIterator.hasNext()) {
            JsonNode currentLandmark = landmarksIterator.next();
            String targetWkt = currentLandmark.get("target").textValue();
            String areaWkt = currentLandmark.get("area").textValue();
            Geometry targetGeometry;
            Geometry areaGeometry;
            Geometry locationGeometry;
            try {
                targetGeometry = reader.read(targetWkt);
                areaGeometry = reader.read(areaWkt);
                locationGeometry = reader.read("POINT (" + fromPlace + ")");
            } catch (ParseException pe){
                LOG.info("Couldn't convert wkt to jts geometry", pe);
                return null;
            }
            if (locationGeometry != null && areaGeometry.contains(locationGeometry)) {
                locationUpdate[0] = targetGeometry.getCoordinate().x+","+targetGeometry.getCoordinate().y;
            }
            locationGeometry = reader.read("POINT (" + toPlace + ")");
            if (locationGeometry != null && areaGeometry.contains(locationGeometry)) {
                locationUpdate[1] = targetGeometry.getCoordinate().x+","+targetGeometry.getCoordinate().y;
            }
        }
            LOG.info("New from: {}, New to: {}", locationUpdate[0], locationUpdate[1]);
        return locationUpdate;
    }
}
