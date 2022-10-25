package org.kkukie.jrtsp_gw.media.webrtc.websocket.service.module;

import kotlin.random.Random;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.*;
import org.kkukie.jrtsp_gw.util.StringManager;

import java.util.ArrayList;
import java.util.Collections;

@Slf4j
@Getter
@Setter
public class RTCPeerConnection {

    private String certPath;

    private SdpSession remoteDesc = null;
    private SdpSession localDesc = null;

    public SdpSession createAnswerSdpSession() {
        if (certPath == null || remoteDesc == null) { return null; }

        createLocalDesc();
        disableUnusedFields();
        if (enableRequisiteFields()) { return null; }

        return localDesc;
    }

    private void createLocalDesc() {
        localDesc = null; // 명시적 메모리 할당 해제
        localDesc = new SdpSession();
        localDesc.parse(remoteDesc.write());
    }

    private void disableUnusedFields() {
        localDesc.setIcePwd(null);
        localDesc.setIceUfrag(null);
        localDesc.setFingerprint(null);
        localDesc.setIceOptions(null);
        localDesc.setGroups(new ArrayList<>());
        localDesc.setSsrcGroups(new ArrayList<>());
        localDesc.setSsrcs(new ArrayList<>());
        localDesc.setMsidSemantic(null);
    }

    private boolean enableRequisiteFields() {
        // VERSION
        localDesc.setVersion(remoteDesc.getVersion());

        // ORIGIN
        SdpOrigin remoteDescOrigin = remoteDesc.getOrigin();
        localDesc.setOrigin(new SdpOrigin(
                "-",
                Random.Default.nextLong(0, Long.MAX_VALUE),
                2,
                "IN",
                remoteDescOrigin != null ? remoteDescOrigin.getIpVer() : 4,
                "127.0.0.1"
        ));

        // TIMEING
        localDesc.setTiming(new SdpTiming(0, 0));

        // GROUP
        SdpGroups descGroup = null;
        for (SdpGroups remoteDescGroup : remoteDesc.getGroups()) {
            if (remoteDescGroup != null && remoteDescGroup.getType().equals("BUNDLE")) {
                descGroup = remoteDescGroup;
                break;
            }
        }
        if (descGroup != null) {
            localDesc.setGroups(Collections.singletonList(descGroup));
        }

        // MEDIA:VIDEO
        String newIcePwd = StringManager.getRandomString(24);
        String newIceUfrag = StringManager.getRandomString(6);
        SdpIceoptions remoteIceOptions = remoteDesc.getIceOptions();
        String newFingerPrint;

        try {
            newFingerPrint = FingerPrintGen.getFingerPrint(certPath);
        } catch (Exception e) {
            log.warn("RTCPeerConnection.getFingerPrint.Exception", e);
            return true;
        }

        for (SdpMedia localMedia : localDesc.getMedia()) {
            SdpMline mline = localMedia.getMline();
            if (mline == null) { continue; }

            localMedia.setIcePwd(new SdpIcepwd(newIcePwd));
            localMedia.setIceUfrag(new SdpIceufrag(newIceUfrag));
            if ((remoteIceOptions != null) && (remoteIceOptions.getValue().equals("trickle"))) {
                localMedia.setIceOptions(remoteIceOptions);
            }
            localMedia.setFingerprint(new SdpFingerprint(newFingerPrint, "sha-256"));
            localMedia.setControl(new SdpControl("recvonly"));
            localMedia.setSetup(new SdpSetup("active"));
            localMedia.setRtcp(new SdpRtcp(mline.getPort(), localDesc.getOrigin().getNetType(), localDesc.getOrigin().getIpVer(), "0.0.0.0"));
            localMedia.setSsrcGroups(new ArrayList<>());
            localMedia.setSsrcs(new ArrayList<>());
            localMedia.setMsid(null);
        }
        return false;
    }

}
