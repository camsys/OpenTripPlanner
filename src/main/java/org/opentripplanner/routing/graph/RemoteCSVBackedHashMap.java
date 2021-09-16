package org.opentripplanner.routing.graph;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// dimensions: KEY COLUMN -> KEY COLUMN VALUE -> [ ROWS IN HEADER->VALUE PAIRS ]
public class RemoteCSVBackedHashMap extends HashMap<String, HashMap<AgencyAndId,ArrayList<HashMap<String, String>>>> {
	
    private static final Logger LOG = LoggerFactory.getLogger(RemoteCSVBackedHashMap.class);

	private static final long serialVersionUID = 1016762843831210467L;

	private String _source;
	private String _idAgencyId;
	
	public RemoteCSVBackedHashMap(String source, String idAgencyId) {
		_source = source;
		_idAgencyId = idAgencyId;

		while(true) {
			try {
				LOG.info("Updating CSV backed hashmap from {}...", source);

				update();		
				
				LOG.info("done.");
				break;
			} catch(Exception e) {
				LOG.error("Failed: " + e.getMessage());

				try {
					LOG.info("Retrying...");
					Thread.sleep(30 * 1000);
				} catch (InterruptedException e1) {
					break;
				}

				continue;
			}
		}
	}
	
	private void update() throws Exception {
	        URL url = new URL(_source);
	        URLConnection connection = url.openConnection();
	
	        InputStreamReader input = new InputStreamReader(connection.getInputStream());
	        BufferedReader buffer = null;
	        
	        synchronized(this) {
		            buffer = new BufferedReader(input);
	
		            String line = null;
			        String[] header = null;
			        
			        List<String> keys = new ArrayList<String>();
			        for(String key : keys) {
	            		super.put(key, new HashMap<AgencyAndId, ArrayList<HashMap<String, String>>>());
	            	}
			        
			        while ((line = buffer.readLine()) != null) {
			        	// ignore commas that are quotedn	ad
			        	String[] splitLine = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

		            	// save top row as header; find all "keys" (end with ...ID)
		            	if(header == null) {
		            		header = splitLine;
		            		
		            		for(String column : header) {
		            			if(column.endsWith("ID")) {
		            				keys.add(column);
		            			}
		            		}		            		
		            		
		            		continue;
		            	}

		            	// create a hashmap of each row: column name => value
		            	HashMap<String,String> record = new HashMap<String, String>();
		            	int c = 0;
		            	for(String column : splitLine) {
		            		record.put(header[c].trim(), column.trim().isBlank() ? null : column.replace("\"", "").trim());
		            		c++;
		            	}
		       		            	
		            	
		            	// add record by each key 
		            	for(String key : keys) {
			            	HashMap<AgencyAndId, ArrayList<HashMap<String, String>>> recordsByKeyValue = super.get(key);
			            	if(recordsByKeyValue == null) {
			            		recordsByKeyValue = new HashMap<AgencyAndId, ArrayList<HashMap<String, String>>>();			            	
			            	}

			            	ArrayList<HashMap<String,String>> records = 
			            			recordsByKeyValue.get(new AgencyAndId(_idAgencyId, record.get(key)));
			            	if(records == null) 
			            		records = new ArrayList<HashMap<String, String>>();
			            	
			            	records.add(record);
		            		recordsByKeyValue.put(new AgencyAndId(_idAgencyId, record.get(key)), records);

		            		super.put(key, recordsByKeyValue);
		            	}
		            }	
			        
		            if (buffer != null)
		            	buffer.close();
	        }
       
	}
}
