package org.opentripplanner.graph;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.routing.graph.RemoteCSVBackedHashMap;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;


public class RemoteCSVBackedHashMapTest {

    private static RemoteCSVBackedHashMap mtaSubwayStationsOld;
    private static RemoteCSVBackedHashMap mtaSubwayComplexesOld;
    private static RemoteCSVBackedHashMap mtaSubwayStationsNew;
    private static RemoteCSVBackedHashMap mtaSubwayComplexesNew;


    @BeforeClass
    public static void setup() throws URISyntaxException {
        String oldStationsFilePath = getFilePath("control_files/old_stations.csv");
        String oldComplexesFilePath = getFilePath("control_files/old_complex.csv");
        String newStationsFilePath = getFilePath("control_files/stations.csv");
        String newComplexesFilePath = getFilePath("control_files/complex.csv");

        mtaSubwayStationsOld = new RemoteCSVBackedHashMap().process(oldStationsFilePath, "MTASBWY");
        mtaSubwayComplexesOld = new RemoteCSVBackedHashMap().process(oldComplexesFilePath, "MTASBWY");
        mtaSubwayStationsNew = new RemoteCSVBackedHashMap().process(newStationsFilePath, "MTASBWY");
        mtaSubwayComplexesNew = new RemoteCSVBackedHashMap().process(newComplexesFilePath, "MTASBWY");
    }

    @Test
    public void testOldStationsColumns()  {
        Assert.assertEquals(3, mtaSubwayStationsOld.keySet().size());
    }

    @Test
    public void testNewStationsColumns(){
        Assert.assertEquals(3, mtaSubwayStationsNew.keySet().size());
    }

    @Test
    public void testOldStationsValues(){
        Assert.assertEquals(493, mtaSubwayStationsOld.get("Station ID").keySet().size());
    }

    @Test
    public void testNewStationsValues(){
        Assert.assertEquals(493, mtaSubwayStationsNew.get("station_id").keySet().size());
    }

    @Test
    public void testOldComplexColumns(){
        Assert.assertEquals(1, mtaSubwayComplexesOld.keySet().size());
    }

    @Test
    public void testNewComplexColumns(){
        Assert.assertEquals(1, mtaSubwayComplexesNew.keySet().size());
    }

    @Test
    public void testOldComplexValues(){
        Assert.assertEquals(32, mtaSubwayComplexesOld.get("Complex ID").keySet().size());
    }

    @Test
    public void testNewComplexValues(){
        Assert.assertEquals(32, mtaSubwayComplexesNew.get("complex_id").keySet().size());
    }

    public static String getFilePath(String fileName) throws URISyntaxException {
        URL resource = RemoteCSVBackedHashMapTest.class.getClassLoader().getResource(fileName);
        File file = new File(resource.toURI());
        return file.getAbsolutePath();
    }

}
