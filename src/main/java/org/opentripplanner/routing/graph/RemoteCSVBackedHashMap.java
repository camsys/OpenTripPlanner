package org.opentripplanner.routing.graph;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCSVBackedHashMap extends HashMap<String,HashMap<String, String>> {
	
    private static final Logger LOG = LoggerFactory.getLogger(RemoteCSVBackedHashMap.class);

	private static final long serialVersionUID = 1016762843831210467L;

	private String _source;
	private String _indexColumn;
	
	public RemoteCSVBackedHashMap(String source, String indexColumn) {
		_source = source;
		_indexColumn = indexColumn;
		update();
	}
	
	private void update() {
		try { 
	        URL url = new URL(_source);
	        URLConnection connection = url.openConnection();
	
	        InputStreamReader input = new InputStreamReader(connection.getInputStream());
	        BufferedReader buffer = null;
	        
	        synchronized(this) {
		        try {
		            buffer = new BufferedReader(input);
	
		            String line = null;
			        String[] header = null;
		            while ((line = buffer.readLine()) != null) {
		            	String[] splitLine = line.split(",");
		            	if(header == null) {
		            		header = splitLine;
		            		continue;
		            	}

		            	HashMap<String,String> record = new HashMap<String, String>();
		            	int c = 0;
		            	for(String column : splitLine) {
		            		record.put(header[c].trim(), column.trim());
		            		c++;
		            	}
		            	
		            	super.put(record.get(_indexColumn).trim(), record);
		            }	
		        } finally {
		            if (buffer != null)
		            	buffer.close();
		        }
	        }
        } catch (Exception e) {
        	LOG.error("Fetch of CSV failed:");
            e.printStackTrace();
        }
	}
}
