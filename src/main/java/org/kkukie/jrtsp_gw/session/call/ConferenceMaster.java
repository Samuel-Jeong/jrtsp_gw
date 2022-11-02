package org.kkukie.jrtsp_gw.session.call;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.session.call.model.ConferenceInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ConferenceMaster {


    private final int maxSessionCount;

    private static final ConferenceMaster conferenceMaster = new ConferenceMaster();

    private final HashMap<String, ConferenceInfo> conferenceInfos;
    private final ReentrantLock conferenceInfoMapLock = new ReentrantLock();

    private ConferenceMaster() {
        this.maxSessionCount = ConfigManager.getSessionConfig().getMaxSessionCount();

        conferenceInfos = new HashMap<>();
    }

    public static ConferenceMaster getInstance() {
        return conferenceMaster;
    }

    public ConferenceInfo createConference(String conferenceId, boolean isHost) {
        if (conferenceInfos.containsKey(conferenceId)) {
            log.warn("|ConferenceMaster| Conference is already exist. (conferenceId={})", conferenceId);
            return null;
        }
        if (conferenceInfos.size() >= maxSessionCount) {
            log.warn("|ConferenceMaster| Conference count is maximum size. (conferenceId={}, size={})", conferenceId, conferenceInfos.size());
            return null;
        }

        ConferenceInfo conferenceInfo;
        conferenceInfoMapLock.lock();
        try {
            conferenceInfo = new ConferenceInfo(conferenceId, isHost);
            conferenceInfos.put(conferenceId, conferenceInfo);
        } catch (Exception e) {
            log.warn("|ConferenceMaster| Fail to add new conferenceInfo. (conferenceId={})", conferenceId);
            return null;
        } finally {
            conferenceInfoMapLock.unlock();
            if (conferenceInfos.get(conferenceId) != null) {
                log.debug("|ConferenceMaster| Conference is added. (conferenceId={})", conferenceId);
            }
        }

        return conferenceInfo;
    }

    public void deleteConference(String conferenceId) {
        ConferenceInfo conferenceInfo = findConference(conferenceId);
        if (conferenceInfo == null) { return; }

        conferenceInfoMapLock.lock();
        try {
            conferenceInfo.stopWebRtcService();
            conferenceInfos.remove(conferenceId);
        } catch (Exception e) {
            log.warn("|ConferenceMaster| Fail to delete the conferenceInfo. (conferenceId={})", conferenceInfo.getConferenceId(), e);
        } finally {
            conferenceInfoMapLock.unlock();
            if (conferenceInfos.get(conferenceId) == null) {
                log.debug("|ConferenceMaster| Conference is deleted. (conferenceId={})", conferenceInfo.getConferenceId());
            }
        }
    }

    public ConferenceInfo findConference(String conferenceId) {
        if (conferenceId == null) { return null; }
        return conferenceInfos.get(conferenceId);
    }

    public int getConferenceInfoSize() {
        return conferenceInfos.size();
    }

    public List<ConferenceInfo> getConferenceInfos() {
        conferenceInfoMapLock.lock();
        try {
            return new ArrayList<>(conferenceInfos.values());
        } catch (Exception e) {
            log.warn("|ConferenceMaster| Fail to get the conferenceInfo list.", e);
            return Collections.emptyList();
        } finally {
            conferenceInfoMapLock.unlock();
        }
    }

}
