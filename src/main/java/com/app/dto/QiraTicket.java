package com.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QiraTicket {
    
    private JsonNode rawData;
    
    public QiraTicket() {}
    
    public JsonNode getRawData() {
        return rawData;
    }
    
    public void setRawData(JsonNode rawData) {
        this.rawData = rawData;
    }
}
