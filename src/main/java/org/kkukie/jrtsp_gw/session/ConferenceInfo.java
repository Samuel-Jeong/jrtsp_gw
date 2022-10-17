package org.kkukie.jrtsp_gw.session;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConferenceInfo {

    private static final int MAX_CLIENT_COUNT = 100;

    private final Map<String, CallInfo> callInfos;

    private final long createdTime;

    private final String conferenceId;

    private String hostCallId;

    private ConferenceState state = ConferenceState.INIT;

    public ConferenceInfo(String conferenceId) {
        this.conferenceId = conferenceId;
        callInfos = new ConcurrentHashMap<>();
        createdTime = System.currentTimeMillis();
    }

    public String getConferenceId() {
        return conferenceId;
    }

    public boolean addCall(String callId, CallInfo callInfo) {
        if (callInfos.containsKey(callId)) {
            log.warn("Call-Id({}) is already exist. (conferenceId={})", callId, conferenceId);
            return false;
        }
        if (callInfos.size() >= MAX_CLIENT_COUNT) {
            log.warn("Conference's Call-ID list is full. (Call-ID={}, conferenceId={})", callId, conferenceId);
            return false;
        }
        boolean result = callInfos.put(callId, callInfo) == null;
        if (result && state != ConferenceState.ACTIVATE) {
            state = ConferenceState.ACTIVATE;
        }
        return result;
    }

    public void removeCall(String callId) {
        callInfos.remove(callId);
        if (callInfos.isEmpty() && state != ConferenceState.EMPTY) {
            state = ConferenceState.EMPTY;
        }
    }

    public List<String> getCallIds() {
        synchronized (callInfos) {
            return new ArrayList<>(callInfos.keySet());
        }
    }

    public List<CallInfo> getCallInfos() {
        synchronized (callInfos) {
            return new ArrayList<>(callInfos.values());
        }
    }

    public int getCallIdSize() {
        return callInfos.size();
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public ConferenceState getState() {
        return state;
    }

    public void setState(ConferenceState state) {
        this.state = state;
    }

    public String getHostCallId() {
        return hostCallId;
    }

    public ConferenceInfo setHostCallId(String hostCallId) {
        this.hostCallId = hostCallId;
        return this;
    }

}
