package com.example.Project01;

 // match your actual package name

import jakarta.persistence.*;

@Entity
@Table(name = "request_log")
public class RequestLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientId;
    private long requestedAtEpochMillis;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public long getRequestedAtEpochMillis() { return requestedAtEpochMillis; }
    public void setRequestedAtEpochMillis(long requestedAtEpochMillis) { this.requestedAtEpochMillis = requestedAtEpochMillis; }
}
