package com.smartedge.model;

import java.util.List;

public class EventChainResult {

    private String deviceId;

    private String diagnosis;

    private Double confidence;

    private String severity;

    private Double patternMatch;

    private Double temporalMatch;

    private Double eventConfidence;

    private Long timeWindowSeconds;

    private List<String> eventSequence;

    private String explanation;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EventChainResult() {
    }


    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }


    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }


    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }


    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }


    public Double getPatternMatch() {
        return patternMatch;
    }

    public void setPatternMatch(Double patternMatch) {
        this.patternMatch = patternMatch;
    }


    public Double getTemporalMatch() {
        return temporalMatch;
    }

    public void setTemporalMatch(Double temporalMatch) {
        this.temporalMatch = temporalMatch;
    }


    public Double getEventConfidence() {
        return eventConfidence;
    }

    public void setEventConfidence(Double eventConfidence) {
        this.eventConfidence = eventConfidence;
    }


    public Long getTimeWindowSeconds() {
        return timeWindowSeconds;
    }

    public void setTimeWindowSeconds(Long timeWindowSeconds) {
        this.timeWindowSeconds = timeWindowSeconds;
    }


    public List<String> getEventSequence() {
        return eventSequence;
    }

    public void setEventSequence(List<String> eventSequence) {
        this.eventSequence = eventSequence;
    }


    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}