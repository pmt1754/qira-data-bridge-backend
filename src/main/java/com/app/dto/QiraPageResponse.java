package com.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QiraPageResponse {
    
    @JsonProperty("items")
    private List<QiraTicket> items;
    
    @JsonProperty("total")
    private Integer total;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("pageSize")
    private Integer pageSize;
    
    @JsonProperty("hasNext")
    private Boolean hasNext;
    
    @JsonProperty("next")
    private String next;
    
    public QiraPageResponse() {}
    
    public List<QiraTicket> getItems() {
        return items;
    }
    
    public void setItems(List<QiraTicket> items) {
        this.items = items;
    }
    
    public Integer getTotal() {
        return total;
    }
    
    public void setTotal(Integer total) {
        this.total = total;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    public Boolean getHasNext() {
        return hasNext;
    }
    
    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    public String getNext() {
        return next;
    }
    
    public void setNext(String next) {
        this.next = next;
    }
    
    public boolean hasMorePages() {
        return Boolean.TRUE.equals(hasNext) || next != null;
    }
}
