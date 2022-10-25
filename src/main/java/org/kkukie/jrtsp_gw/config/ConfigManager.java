package org.kkukie.jrtsp_gw.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ConfigManager {

    private final ConfigEnv configEnv;

    public ConfigManager(ConfigEnv configEnv) {
        this.configEnv = configEnv;

        setDefaultConfig();
        setSdpConfig();
        setDtlsConfig();
        setStunConfig();
        setSessionConfig();
    }

    private static DefaultConfig defaultConfig = null;
    private static SdpConfig sdpConfig = null;
    private static DtlsConfig dtlsConfig = null;
    private static StunConfig stunConfig = null;
    private static SessionConfig sessionConfig = null;

    public void setDefaultConfig() {
        if (defaultConfig == null) {
            defaultConfig = new DefaultConfig();
            defaultConfig.setId(configEnv.getStringProperty("default.id"));
            defaultConfig.setServerUri(configEnv.getStringProperty("default.serverUri"));
            defaultConfig.setApplicationName(configEnv.getStringProperty("default.applicationName"));
            defaultConfig.setSendBufSize(configEnv.getIntProperty("default.sendBufSize"));
            defaultConfig.setRecvBufSize(configEnv.getIntProperty("default.recvBufSize"));
            defaultConfig.setStreamThreadPoolSize(configEnv.getIntProperty("default.streamThreadPoolSize"));
            defaultConfig.setLocalListenIp(configEnv.getStringProperty("default.localListenIp"));
            defaultConfig.setLocalRtspListenPort(configEnv.getIntProperty("default.localRtspListenPort"));
            defaultConfig.setLocalPortMin(configEnv.getIntProperty("default.localPortMin"));
            defaultConfig.setLocalPortMax(configEnv.getIntProperty("default.localPortMax"));
            defaultConfig.setRtpBurstBufferCount(configEnv.getIntProperty("default.rtpBurstBufferCount"));
            log.debug("DefaultConfig: {}", defaultConfig.toString());
        }
    }

    public static DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    public void setSdpConfig() {
        if (sdpConfig == null) {
            sdpConfig = new SdpConfig();
            sdpConfig.setVersion(configEnv.getStringProperty("sdp.version"));
            sdpConfig.setOrigin(configEnv.getStringProperty("sdp.origin"));
            sdpConfig.setSession(configEnv.getStringProperty("sdp.session"));
            sdpConfig.setConnection(configEnv.getStringProperty("sdp.connection"));
            sdpConfig.setTime(configEnv.getStringProperty("sdp.time"));
            sdpConfig.setAudio(configEnv.getStringProperty("sdp.audio"));
            sdpConfig.setAudioRtpMap(configEnv.getStringProperty("sdp.audioRtpMap"));
            sdpConfig.setVideo(configEnv.getStringProperty(("sdp.video")));
            sdpConfig.setVideoRtpMap(configEnv.getStringProperty("sdp.videoRtpMap"));
            sdpConfig.setSdpLocalIp(configEnv.getStringProperty("sdp.sdpLocalIp"));
            sdpConfig.setAudioAttributeList(configEnv.getStringArrayProperty("sdp.audioAttributeList"));
            sdpConfig.setVideoAttributeList(configEnv.getStringArrayProperty("sdp.videoAttributeList"));
            log.debug("SdpConfig: {}", sdpConfig.toString());
        }
    }

    public static SdpConfig getSdpConfig() {
        return sdpConfig;
    }

    public void setDtlsConfig() {
        if (dtlsConfig == null) {
            dtlsConfig = new DtlsConfig();
            dtlsConfig.setKeyPath(configEnv.getStringProperty("dtls.keyPath"));
            dtlsConfig.setCertPath(configEnv.getStringProperty("dtls.certPath"));
            log.debug("DtlsConfig: {}", dtlsConfig.toString());
        }
    }

    public static DtlsConfig getDtlsConfig() {
        return dtlsConfig;
    }

    public void setStunConfig() {
        if (stunConfig == null) {
            stunConfig = new StunConfig();
            stunConfig.setHarvestIntervalMs(configEnv.getIntProperty("stun.harvestIntervalMs"));
            log.debug("StunConfig: {}", stunConfig.toString());
        }
    }

    public static StunConfig getStunConfig() {
        return stunConfig;
    }

    public void setSessionConfig() {
        if (sessionConfig == null) {
            sessionConfig = new SessionConfig();
            sessionConfig.setMaxSessionCount(configEnv.getIntProperty("session.maxSessionCount"));
            log.debug("SessionConfig: {}", sessionConfig.toString());
        }
    }

    public static SessionConfig getSessionConfig() {
        return sessionConfig;
    }

}
