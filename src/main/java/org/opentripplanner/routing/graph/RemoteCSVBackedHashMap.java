package org.opentripplanner.routing.graph;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// dimensions: KEY COLUMN -> KEY COLUMN VALUE -> [ ROWS IN HEADER->VALUE PAIRS ]
public class RemoteCSVBackedHashMap extends HashMap<String, HashMap<AgencyAndId,ArrayList<HashMap<String, String>>>> {
	
    private static final Logger LOG = LoggerFactory.getLogger(RemoteCSVBackedHashMap.class);

	private static final long serialVersionUID = 1016762843831210467L;


	public RemoteCSVBackedHashMap process(String source, String idAgencyId){
		while(true) {
			try {
				LOG.info("Updating CSV backed hashmap from {}...", source);

				processCSV(source, idAgencyId);

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
			}
		}
		return this;
	}

	
	void processCSV(String source, String idAgencyId) throws Exception {
		InputStream inputStream = getCSVFile(source);
		InputStreamReader reader = new InputStreamReader(inputStream);
		BufferedReader buffer = null;
	        
	        synchronized(this) {
		            buffer = new BufferedReader(reader);
	
		            String line = null;
			        String[] headers = null;
			        
			        List<String> keys = new ArrayList<String>();
			        for(String key : keys) {
	            		super.put(key, new HashMap<AgencyAndId, ArrayList<HashMap<String, String>>>());
	            	}
			        
			        while ((line = buffer.readLine()) != null) {
			        	// ignore commas that are quotedn	ad
			        	String[] splitLine = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

		            	// save top row as header; find all "keys" (end with ...ID)
		            	if(headers == null) {
							headers = processHeaders(splitLine);
							keys.addAll(processKeys(headers));
		            		continue;
		            	}

		            	// create a hashmap of each row: column name => value
		            	HashMap<String,String> record = new HashMap<String, String>();
		            	int c = 0;
		            	for(String column : splitLine) {
							column = stripQuotes(column);
							record.put(headers[c], column.trim().isBlank() ? null : column.replace("\"", "").trim());
		            		c++;
		            	}
		       		            	
		            	
		            	// add record by each key 
		            	for(String key : keys) {
			            	HashMap<AgencyAndId, ArrayList<HashMap<String, String>>> recordsByKeyValue = super.get(key);
			            	if(recordsByKeyValue == null) {
			            		recordsByKeyValue = new HashMap<AgencyAndId, ArrayList<HashMap<String, String>>>();			            	
			            	}

			            	ArrayList<HashMap<String,String>> records = 
			            			recordsByKeyValue.get(new AgencyAndId(idAgencyId, record.get(key)));
			            	if(records == null) 
			            		records = new ArrayList<HashMap<String, String>>();
			            	
			            	records.add(record);
		            		recordsByKeyValue.put(new AgencyAndId(idAgencyId, record.get(key)), records);

		            		super.put(key, recordsByKeyValue);
		            	}
		            }	
			        
		            if (buffer != null)
		            	buffer.close();
	        }
	}

	private List<String> processKeys(String[] headers) {
		List<String> keys = new ArrayList<>();
		for(String column : headers) {
			column = stripQuotes(column);
			if(column.toUpperCase().endsWith("ID")) {
				keys.add(column);
			}
		}
		return keys;
	}

	private String[] processHeaders(String[] line) {
		String[] headers = new String[line.length];
		for(int i=0; i<line.length; i++){
			headers[i] = stripQuotes(line[i]).trim();
		}
		return headers;
	}

	private String stripQuotes(String column){
		String value = column.trim();
		if (value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length() - 1).trim();
		}
		return value;
	}

	InputStream getCSVFile(String source) throws Exception {
		if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("file://")) {
			URL url = new URL(source);
			URLConnection connection = url.openConnection();
			return connection.getInputStream();
		} else {
			return new FileInputStream(new File(source));
		}
	}
}
