package org.opentripplanner.graph;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.graph.RemoteCSVBackedHashMap;
import org.opentripplanner.routing.graph.RemoteCSVBackedHashMapUtil;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RemoteCSVBackedHashMapUtilTest {

    private static RemoteCSVBackedHashMap mtaSubwayStations;
    private static RemoteCSVBackedHashMap mtaSubwayComplexes;


    @BeforeClass
    public static void setup() throws URISyntaxException{
        String stationsFilePath = getFilePath("control_files/old_stations.csv");
        String complexesFilePath = getFilePath("control_files/old_complex.csv");

        mtaSubwayStations = new RemoteCSVBackedHashMap().process(stationsFilePath, "MTASBWY");
        mtaSubwayComplexes = new RemoteCSVBackedHashMap().process(complexesFilePath, "MTASBWY");
    }

    public static RemoteCSVBackedHashMap getMtaSubwayStations() {
        return mtaSubwayStations;
    }


    public static RemoteCSVBackedHashMap getMtaSubwayComplexes() {
        return mtaSubwayComplexes;
    }


    /**
     * Stations
     */
    @Test
    public void testGetStationRecordsByComplexId_subwayStations()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "623");

        ArrayList<HashMap<String, String>> stationsByComplexId =
                RemoteCSVBackedHashMapUtil.getStationRecordsByComplexId(getMtaSubwayStations(), aid);

        Assert.assertEquals(4, stationsByComplexId.size());
    }

    @Test
    public void testGetStationRecordsByStationId_subwayStations()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "7");

        ArrayList<HashMap<String, String>> stationsByStationId =
                RemoteCSVBackedHashMapUtil.getStationRecordsByStationId(getMtaSubwayStations(), aid);

        Assert.assertEquals(1, stationsByStationId.size());
    }

    @Test
    public void testGetStationRecordsByGtfsStopId_subwayStations()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "R01");

        ArrayList<HashMap<String, String>> stationsByStopId =
                RemoteCSVBackedHashMapUtil.getStationRecordsByGtfsStopId(getMtaSubwayStations(), aid);

        Assert.assertEquals(1, stationsByStopId.size());
    }

    @Test
    public void testGetStationId_subwayStations()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "7");
        ArrayList<HashMap<String, String>> stationsByStationId =
                RemoteCSVBackedHashMapUtil.getStationRecordsByStationId(getMtaSubwayStations(), aid);

        String actualStationId = RemoteCSVBackedHashMapUtil.getStationId(stationsByStationId.get(0));

        Assert.assertEquals("7", actualStationId);
    }


    @Test
    public void testGetStationGtfsStopId_subwayStations()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "7");
        ArrayList<HashMap<String, String>> stationsByStationId =
                RemoteCSVBackedHashMapUtil.getStationRecordsByStationId(getMtaSubwayStations(), aid);

        String actualStopId = RemoteCSVBackedHashMapUtil.getStationGtfsStopId(stationsByStationId.get(0));
        Assert.assertEquals("R11", actualStopId);
    }

    @Test
    public void testGetStationAda_subwayStations()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "10");
        ArrayList<HashMap<String, String>> stationsByStationId =
                RemoteCSVBackedHashMapUtil.getStationRecordsByStationId(getMtaSubwayStations(), aid);

        String actualAdaValue = RemoteCSVBackedHashMapUtil.getStationAda(stationsByStationId.get(0));
        Assert.assertEquals("2", actualAdaValue);
    }

    @Test
    public void testGetStationAdaNotes_subwayStations()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "10");
        ArrayList<HashMap<String, String>> stationsByStationId =
                RemoteCSVBackedHashMapUtil.getStationRecordsByStationId(getMtaSubwayStations(), aid);

        String actualAdaNotes = RemoteCSVBackedHashMapUtil.getStationAdaNotes(stationsByStationId.get(0));
        Assert.assertEquals("Uptown & Queens", actualAdaNotes);
    }


    /**
     * Complexes
     */
    @Test
    public void testGetComplexRecordsByComplexId_subwayComplexes() {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "623");

        ArrayList<HashMap<String, String>> complexesByComplexId =
                RemoteCSVBackedHashMapUtil.getComplexRecordsByComplexId(getMtaSubwayComplexes(), aid);

        Assert.assertEquals(1, complexesByComplexId.size());
    }


    @Test
    public void testGetComplexAda_subwayComplexes()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "623");
        ArrayList<HashMap<String, String>> complexesByComplexId =
                RemoteCSVBackedHashMapUtil.getComplexRecordsByComplexId(getMtaSubwayComplexes(), aid);

        String actualAdaValue = RemoteCSVBackedHashMapUtil.getComplexAda(complexesByComplexId.get(0));
        Assert.assertEquals("2", actualAdaValue);
    }

    @Test
    public void testGetComplexAdaNote_subwayComplexes()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "623");
        ArrayList<HashMap<String, String>> complexesByComplexId =
                RemoteCSVBackedHashMapUtil.getComplexRecordsByComplexId(getMtaSubwayComplexes(), aid);

        String actualAdaNotes = RemoteCSVBackedHashMapUtil.getComplexAdaNotes(complexesByComplexId.get(0));
        Assert.assertEquals("R W not accessible; N Q not accessible; J Z not accessible; 6 accessible", actualAdaNotes);
    }

    @Test
    public void testGetComplexId_subwayStations()  {
        AgencyAndId aid = new AgencyAndId("MTASBWY", "623");
        ArrayList<HashMap<String, String>> stationsByStationId =
                RemoteCSVBackedHashMapUtil.getComplexRecordsByComplexId(getMtaSubwayComplexes(), aid);

        String actualComplexId = RemoteCSVBackedHashMapUtil.getComplexId(stationsByStationId.get(0));
        Assert.assertEquals("623", actualComplexId);
    }


    private static String getFilePath(String fileName) throws URISyntaxException {
        URL resource = RemoteCSVBackedHashMapUtilTest.class.getClassLoader().getResource(fileName);
        File file = new File(resource.toURI());
        return file.getAbsolutePath();
    }
}
