package org.kkukie.jrtsp_gw.controller;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.service.WebRtcService;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.service.model.WebRtcServiceInfo;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.WebSocketService;
import org.kkukie.jrtsp_gw.service.system.SystemManager;
import org.kkukie.jrtsp_gw.session.call.ConferenceMaster;
import org.kkukie.jrtsp_gw.session.call.model.ConferenceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class MainServiceController {

    public MainServiceController() {}

    @GetMapping("/conference_count")
    public String getCallCountV1() {
        return String.valueOf(ConferenceMaster.getInstance().getConferenceInfoSize());
    }

    @GetMapping("/cpu_usage")
    public String getCpuUsageV1() {
        return String.valueOf(SystemManager.getInstance().getCpuUsage());
    }

    @GetMapping("/heap_memory_usage")
    public String getHeapMemoryUsageV1() {
        return String.valueOf(SystemManager.getInstance().getHeapMemoryUsage());
    }

    @GetMapping("/total_thread_count")
    public String getTotalThreadCountV1() {
        return String.valueOf(Thread.activeCount());
    }

    @GetMapping("/all_conference_ids")
    public String getAllConferenceId() {
        List<ConferenceInfo> conferenceInfos = ConferenceMaster.getInstance().getConferenceInfos();
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("ConferenceIds");
        for (ConferenceInfo conferenceInfo : conferenceInfos) {
            if (conferenceInfo == null) { continue; }

            String conferenceId = conferenceInfo.getConferenceId();
            if (conferenceId == null || conferenceId.isEmpty()) { continue; }

            stringBuilder.append(",");
            stringBuilder.append(conferenceId);
        }

        return stringBuilder.toString();
    }

    @GetMapping("/{conferenceId}/uri")
    public String getUriByConferenceId(@PathVariable String conferenceId) {
        WebRtcServiceInfo webRtcServiceInfo = getWebRtcServiceInfo(conferenceId);
        if (webRtcServiceInfo == null) { return "none"; }

        return webRtcServiceInfo.getUri();
    }

    @GetMapping("/{conferenceId}/local_sdp")
    public String getLocalSdpByConferenceId(@PathVariable String conferenceId) {
        WebRtcServiceInfo webRtcServiceInfo = getWebRtcServiceInfo(conferenceId);
        if (webRtcServiceInfo == null)  { return "none"; }

        return webRtcServiceInfo.getLocalSdp();
    }

    @GetMapping("/{conferenceId}/remote_sdp")
    public String getRemoteSdpByConferenceId(@PathVariable String conferenceId) {
        WebRtcServiceInfo webRtcServiceInfo = getWebRtcServiceInfo(conferenceId);
        if (webRtcServiceInfo == null) { return "none"; }

        return webRtcServiceInfo.getRemoteSdp();
    }

    private WebRtcServiceInfo getWebRtcServiceInfo(String conferenceId) {
        ConferenceInfo conference = ConferenceMaster.getInstance().findConference(conferenceId);
        if (conference == null) {
            return null;
        }

        WebRtcService webRtcService = conference.getWebRtcService();
        if (webRtcService == null) {
            return null;
        }

        WebSocketService webSocketService = webRtcService.getWebSocketService();
        if (webSocketService == null) {
            return null;
        }

        return webSocketService.getWebRtcServiceInfo();
    }

}
