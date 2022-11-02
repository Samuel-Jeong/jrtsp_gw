package org.kkukie.jrtsp_gw.session.call;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.service.WebRtcService;
import org.kkukie.jrtsp_gw.session.call.model.ConferenceInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class ConferenceMaster {


    private final int maxConferenceSessionCount;

    private static final ConferenceMaster conferenceMaster = new ConferenceMaster();

    private final HashMap<String, ConferenceInfo> conferenceInfos;

    private ConferenceMaster() {
        this.maxConferenceSessionCount = ConfigManager.getSessionConfig().getMaxSessionCount();

        conferenceInfos = new HashMap<>();
    }

    public static ConferenceMaster getInstance() {
        return conferenceMaster;
    }

    public ConferenceInfo createConference(String conferenceId, boolean isHost) {
        if (conferenceInfos.containsKey(conferenceId)) {
            log.warn("|ConferenceMaster{}| Conference is already exist.", conferenceId);
            return null;
        }
        if (conferenceInfos.size() >= maxConferenceSessionCount) {
            log.warn("|ConferenceMaster{}| Conference count is maximum size. (size={})", conferenceId, conferenceInfos.size());
            return null;
        }

        ConferenceInfo conferenceInfo = new ConferenceInfo(conferenceId, isHost);
        synchronized (conferenceInfos) {
            conferenceInfos.put(conferenceId, conferenceInfo);
        }

        conferenceInfo.startWebRtcService();

        log.info("|ConferenceMaster{}| Conference is added.", conferenceInfo.getConferenceId());
        return conferenceInfo;
    }

    public void deleteConference(String conferenceId) {
        ConferenceInfo conferenceInfo = findConference(conferenceId);
        if (conferenceInfo == null) { return; }

        try {
            conferenceInfo.stopWebRtcService();

            synchronized (conferenceInfos) {
                conferenceInfos.remove(conferenceId);
            }
        } finally {
            if (!conferenceInfos.containsKey(conferenceId)) {
                log.info("|ConferenceMaster({})| Conference is deleted.", conferenceInfo.getConferenceId());
            }
        }
    }

    public ConferenceInfo findConference(String conferenceId) {
        return conferenceInfos.get(conferenceId);
    }

    public int getConferenceInfoSize() {
        return conferenceInfos.size();
    }

    public List<ConferenceInfo> getConferenceInfos() {
        synchronized (conferenceInfos) {
            return new ArrayList<>(conferenceInfos.values());
        }
    }

}
