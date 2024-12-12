package org.opentripplanner.routing.graph;

import org.onebusaway.gtfs.model.AgencyAndId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RemoteCSVBackedHashMapUtil {

    public static ArrayList<HashMap<String, String>> getStationRecordsByComplexId(RemoteCSVBackedHashMap map,
                                                                                 AgencyAndId aid) {
        Map<AgencyAndId, ArrayList<HashMap<String,String>>> recordsById = getStationComplexId(map);
        return getSubwayRecordsForId(recordsById, aid);

    }

    public static ArrayList<HashMap<String, String>> getStationRecordsByStationId(RemoteCSVBackedHashMap map,
                                                                                  AgencyAndId aid) {
        Map<AgencyAndId, ArrayList<HashMap<String,String>>> recordsById = getStationId(map);
        return getSubwayRecordsForId(recordsById, aid);
    }

    public static ArrayList<HashMap<String, String>> getStationRecordsByGtfsStopId(RemoteCSVBackedHashMap map,
                                                                                  AgencyAndId aid) {
        Map<AgencyAndId, ArrayList<HashMap<String,String>>> recordsById = getStationGtfsStopId(map);
        return getSubwayRecordsForId(recordsById, aid);
    }

    public static ArrayList<HashMap<String, String>> getComplexRecordsByComplexId(RemoteCSVBackedHashMap map,
                                                                                  AgencyAndId aid) {
        Map<AgencyAndId, ArrayList<HashMap<String,String>>> recordsById = getComplexId(map);
        return getSubwayRecordsForId(recordsById, aid);

    }

    /**
     * Subway Methods
     **/

    public static <T> T getStationGtfsStopId(Map<String,T> record){
        return getSubwayRecordValue(record, "gtfs_stop_id", "GTFS Stop ID");
    }

    public static <T> T getStationComplexId(Map<String,T> record){
        return getSubwayRecordValue(record, "complex_id", "Complex ID");
    }

    public static <T> T getStationAda(Map<String,T> record){
        return getSubwayRecordValue(record, "ada", "ADA");
    }

    public static <T> T getStationAdaNotes(Map<String,T> record){
        return getSubwayRecordValue(record, "ada_notes", "ADA Direction Notes");
    }

    public static <T> T getStationId(Map<String,T> record){
        return getSubwayRecordValue(record, "station_id", "Station ID");
    }

    /**
     * Subway Complex
    **/

     public static <T> T getComplexId(Map<String,T> record){
        return getSubwayRecordValue(record, "complex_id", "Complex ID");
    }

    public static <T> T getComplexAda(Map<String,T> record){
        return getSubwayRecordValue(record, "ada", "ADA");
    }

    public static <T> T getComplexAdaNotes(Map<String,T> record){
        return getSubwayRecordValue(record, "ada_notes", "ADA Notes");
    }

    private static <T> T getSubwayRecordValue(Map<String, T> record, String... keys) {
        for (String key : keys) {
            T value = record.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static ArrayList<HashMap<String, String>> getSubwayRecordsForId(Map<AgencyAndId, ArrayList<HashMap<String,String>>> map,
                                                                            AgencyAndId aid){
        if(map != null){
            return map.get(aid);
        }
        return null;
    }
}
