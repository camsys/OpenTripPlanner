package org.opentripplanner.graph;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.routing.graph.RemoteCSVBackedHashMap;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class RemoteCSVBackedHashMapTest {
    @Test
    public void testOldStationsColumns() throws URISyntaxException {
        RemoteCSVBackedHashMap csvBackedHashMap = new RemoteCSVBackedHashMap();
        String filePath = getFilePath("control_files/old_stations.csv");
        csvBackedHashMap.process(filePath, "MTASBWY");
        Assert.assertEquals(3, csvBackedHashMap.keySet().size());
    }

    @Test
    public void testNewStationsColumns() throws URISyntaxException {
        RemoteCSVBackedHashMap csvBackedHashMap = new RemoteCSVBackedHashMap();
        String filePath = getFilePath("control_files/stations.csv");
        csvBackedHashMap.process(filePath, "MTASBWY");
        Assert.assertEquals(3, csvBackedHashMap.keySet().size());
    }

    @Test
    public void testOldStationsValues() throws URISyntaxException {
        RemoteCSVBackedHashMap csvBackedHashMap = new RemoteCSVBackedHashMap();
        String filePath = getFilePath("control_files/old_stations.csv");
        csvBackedHashMap.process(filePath, "MTASBWY");
        Assert.assertEquals(493, csvBackedHashMap.get("Station ID").keySet().size());
    }

    @Test
    public void testNewStationsValues() throws URISyntaxException {
        RemoteCSVBackedHashMap csvBackedHashMap = new RemoteCSVBackedHashMap();
        String filePath = getFilePath("control_files/stations.csv");
        csvBackedHashMap.process(filePath, "MTASBWY");
        Assert.assertEquals(493, csvBackedHashMap.get("station_id").keySet().size());
    }

    @Test
    public void testOldComplexColumns() throws URISyntaxException {
        RemoteCSVBackedHashMap csvBackedHashMap = new RemoteCSVBackedHashMap();
        String filePath = getFilePath("control_files/old_complex.csv");
        csvBackedHashMap.process(filePath, "MTASBWY");
        Assert.assertEquals(1, csvBackedHashMap.keySet().size());
    }

    @Test
    public void testNewComplexColumns() throws URISyntaxException {
        RemoteCSVBackedHashMap csvBackedHashMap = new RemoteCSVBackedHashMap();
        String filePath = getFilePath("control_files/complex.csv");
        csvBackedHashMap.process(filePath, "MTASBWY");
        Assert.assertEquals(1, csvBackedHashMap.keySet().size());
    }

    @Test
    public void testOldComplexValues() throws URISyntaxException {
        RemoteCSVBackedHashMap csvBackedHashMap = new RemoteCSVBackedHashMap();
        String filePath = getFilePath("control_files/old_complex.csv");
        csvBackedHashMap.process(filePath, "MTASBWY");
        Assert.assertEquals(32, csvBackedHashMap.get("Complex ID").keySet().size());
    }

    @Test
    public void testNewComplexValues() throws URISyntaxException {
        RemoteCSVBackedHashMap csvBackedHashMap = new RemoteCSVBackedHashMap();
        String filePath = getFilePath("control_files/complex.csv");
        csvBackedHashMap.process(filePath, "MTASBWY");
        Assert.assertEquals(32, csvBackedHashMap.get("complex_id").keySet().size());
    }

    private String getFilePath(String fileName) throws URISyntaxException {
        URL resource = this.getClass().getClassLoader().getResource(fileName);
        File file = new File(resource.toURI());
        return file.getAbsolutePath();
    }
}
