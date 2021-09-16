package org.opentripplanner.model;

import org.onebusaway.csv_entities.schema.annotations.CsvField;

public class MTAStop {

	  @CsvField(optional = true)
	  public String stopCode;

	  public String getStopCode() {
		  return stopCode;		  
	  }
	  
	  public void setStopCode(String c) {
		  stopCode = c;
	  }
	  
}
