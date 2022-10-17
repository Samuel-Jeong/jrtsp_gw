package org.kkukie.jrtsp_gw.session;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class SessionManager {


    private static final int MAX_SESSION_COUNT = 100;

    private static final SessionManager sessionManager = new SessionManager();

    private final HashMap<String, CallInfo> callInfos;
    private final HashMap<String, ConferenceInfo> conferenceInfos;

    private SessionManager() {
        callInfos = new HashMap<>();
        conferenceInfos = new HashMap<>();
    }

    public static SessionManager getInstance() {
        return sessionManager;
    }

    public ConferenceInfo createConference(String conferenceId) {
        boolean created = false;
        ConferenceInfo conferenceInfo = conferenceInfos.get(conferenceId);
        if (conferenceInfo == null) {
            conferenceInfo = new ConferenceInfo(conferenceId);
            created = true;
        }

        if (created) {
            synchronized (conferenceInfos) {
                conferenceInfos.put(conferenceId, conferenceInfo);
            }
        }
        return conferenceInfo;
    }

    public synchronized void deleteConference(String conferenceId) {
        ConferenceInfo conferenceInfo = findConference(conferenceId);
        if (conferenceInfo != null) {
            conferenceInfo.setState(ConferenceState.DELETED);
            for (String callId : conferenceInfo.getCallIds()) {
                deleteCall(callId);
            }
            conferenceInfos.remove(conferenceId);
        }
    }

    public ConferenceInfo findConference(String conferenceId) {
        return conferenceInfos.get(conferenceId);
    }

    public int getConferenceInfoSize() {
        return conferenceInfos.size();
    }

    public CallInfo createCall(String conferenceId, String callId, boolean isHost) {
        if (callInfos.containsKey(callId)) {
            log.warn("|SessionManager({}/{})| Call is already exist.", conferenceId, callId);
            return null;
        }
        if (callInfos.size() >= MAX_SESSION_COUNT) {
            log.warn("|SessionManager({}/{})| Call count is maximum size. (size={})", conferenceId, callId, callInfos.size());
            return null;
        }

        CallInfo callInfo = new CallInfo(conferenceId, callId, isHost);
        synchronized (callInfos) {
            callInfos.put(callId, callInfo);
        }

        return callInfo;
    }

    public void deleteCall(String callId) {
        CallInfo callInfo = findCall(callId);
        if (callInfo == null) return;

        try {
            log.info("|SessionManager({}/{})| Call is deleted.", callInfo.getConferenceId(), callInfo.getCallId());
        } finally {
            synchronized (callInfos) {
                callInfos.remove(callId);
            }
        }
    }

    public CallInfo findCall(String callId) {
        return callInfos.get(callId);
    }

    public int getCallInfoSize() {
        return callInfos.size();
    }

    public List<CallInfo> getCallInfos() {
        synchronized (callInfos) {
            return new ArrayList<>(callInfos.values());
        }
    }

    public List<ConferenceInfo> getConferenceInfos() {
        synchronized (conferenceInfos) {
            return new ArrayList<>(conferenceInfos.values());
        }
    }
}
