package org.kkukie.jrtsp_gw.media.core.stream.webrtc.service.model;

import com.google.gson.*;
import com.neovisionaries.ws.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.*;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.config.DtlsConfig;
import org.kkukie.jrtsp_gw.media.core.manager.PacketSelector;
import org.kkukie.jrtsp_gw.media.core.model.DataChannel;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.OmeAnswer;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.OmeCandidate;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.base.CommandType;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.model.ice.IceInfo;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.module.RTCPeerConnectionMaster;
import org.kkukie.jrtsp_gw.media.core.util.WebSocketPortManager;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.OmeOffer;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.command.OmeRequestOffer;
import org.kkukie.jrtsp_gw.media.core.stream.webrtc.websocket.service.model.ice.RTCIceCandidate;
import org.kkukie.jrtsp_gw.session.call.ConferenceMaster;
import org.kkukie.jrtsp_gw.session.call.model.ConferenceInfo;
import org.kkukie.jrtsp_gw.session.media.MediaSession;
import org.kkukie.jrtsp_gw.util.RandomManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
@Setter
public class WebSocketInfo {

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final RTCPeerConnectionMaster localPeerConnection = new RTCPeerConnectionMaster();
    private WebSocket webSocket = null;

    private String localIp = null;
    private int localPort = 0;
    private String remoteRealm = null;

    private PacketSelector packetSelector = null;
    private DataChannel dataChannel;

    private WebRtcServiceInfo webRtcServiceInfo = null;
    private WebRtcIceInfo webRtcIceInfo = null;

    private MediaSession mediaSession = null;

    public WebSocketInfo() {
        DtlsConfig dtlsConfig = ConfigManager.getDtlsConfig();
        localPeerConnection.setCertPath(dtlsConfig.getCertPath());
    }

    public void start(WebRtcServiceInfo webRtcServiceInfo) throws WebSocketException, IOException {
        if (webRtcServiceInfo == null) { return; }

        this.webRtcServiceInfo = webRtcServiceInfo;
        webSocket = connect(webRtcServiceInfo.getUri() + "/" + webRtcServiceInfo.getApplicationName() + "/" + webRtcServiceInfo.getConferenceId());

        localIp = webSocket.getSocket().getLocalAddress().getHostAddress();
        localPort = webSocket.getSocket().getLocalPort();

        if (packetSelector != null) {
            packetSelector.stop();
            packetSelector = null;
        }
        packetSelector = new PacketSelector(webRtcServiceInfo.getConferenceId());
        packetSelector.start();

        log.debug("|WebSocketInfo({})| LOCAL NETWORK = {}:{}", webRtcServiceInfo.getConferenceId(), localIp, localPort);
    }

    private WebSocket connect(String uri) throws IOException, WebSocketException {
        return new WebSocketFactory()
                .setConnectionTimeout(5000)
                .createSocket(uri)
                .addListener(new WebSocketAdapter() {
                    @Override
                    public void onTextMessage(WebSocket websocket, String message) {
                        handleData(message);
                    }

                    @Override
                    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                        log.warn("|WebSocketInfo({})| DISCONNECTED or CLOSED by closedByServer({})", webRtcServiceInfo.getConferenceId(), closedByServer);
                        ConferenceMaster.getInstance().deleteConference(webRtcServiceInfo.getConferenceId());
                    }
                }).setPingInterval(3000)
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                .connect();
    }

    public void stop() {
        freeMediaChannel();

        packetSelector.stop();

        if (webSocket != null) {
            webSocket.disconnect();
            webSocket = null;
        }
    }

    private boolean allocateMediaChannel(SocketAddress localAddress, IceInfo iceInfo) {
        try {
            if (dataChannel == null) {
                dataChannel = new DataChannel(
                        packetSelector, mediaSession,
                        webRtcServiceInfo.getConferenceId(), localAddress
                );
                dataChannel.initChannel();

                List<InetSocketAddress> targetAddressList = createTargetAddressList();
                log.debug("|WebSocketInfo({})| serverAddressList: {} ({})", webRtcServiceInfo.getConferenceId(), targetAddressList.toArray(), targetAddressList.size());

                if (iceInfo != null) {
                    dataChannel.initIce(iceInfo, targetAddressList);
                }
            } else {
                log.warn("|WebSocketInfo({})| Media channel Already Opened", webRtcServiceInfo.getConferenceId());
                return false;
            }
        } catch (Exception e) {
            log.warn("|WebSocketInfo({})| Media channel is not created", webRtcServiceInfo.getConferenceId(), e);
            return false;
        }

        return true;
    }

    private void freeMediaChannel() {
        if (dataChannel != null) {
            dataChannel.close();
            WebSocketPortManager.getInstance().restorePort(
                    ((InetSocketAddress) mediaSession.getLocalMediaAddress()).getPort());
        }
    }

    public void handshake(WebRtcIceInfo webRtcIceInfo) {
        // SEND REQUEST_OFFER
        this.webRtcIceInfo = webRtcIceInfo;
        webSocket.sendText(createCommand(CommandType.REQUEST_OFFER));
    }

    private String createCommand(CommandType type) {
        switch (type) {
            case REQUEST_OFFER:
                return (new OmeRequestOffer()).makeJson();
            case OFFER:
                return (new OmeOffer()).makeJson();
            case ANSWER:
                return webRtcIceInfo.isRemoteIceTrickle() ?
                        (new OmeAnswer(
                                webRtcServiceInfo.getId(),
                                webRtcServiceInfo.getPeerId(),
                                webRtcServiceInfo.getLocalSdp(),
                                null)).makeJson() :
                        (new OmeAnswer(
                                webRtcServiceInfo.getId(),
                                webRtcServiceInfo.getPeerId(),
                                webRtcServiceInfo.getLocalSdp(),
                                webRtcIceInfo.getLocalCandidates())).makeJson();
            case CANDIDATE:
                return (new OmeCandidate(
                        webRtcServiceInfo.getId(),
                        webRtcServiceInfo.getPeerId(),
                        webRtcIceInfo.getLocalCandidates())).makeJson();
            default:
                return "";
        }
    }

    private void handleData(String message) {
        JsonElement jsonElement = JsonParser.parseString(message);
        log.info("|WebSocketInfo({})| Message >> {}", webRtcServiceInfo.getConferenceId(), gson.toJson(jsonElement));
        if (jsonElement == null) {
            return;
        }

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject == null) {
            return;
        }

        JsonElement command = jsonObject.get("command");
        JsonElement code = jsonObject.get("code");
        if (OmeOffer.TYPE.equals(command.getAsString()) && (code.getAsInt() == 200)) {
            handleOmeOffer(jsonObject);
        }
    }

    private void handleOmeOffer(JsonObject jsonObject) {
        // ID
        handleId(jsonObject);

        // PEER ID
        handlePeerId(jsonObject);

        // CANDIDATES
        handleCandidates(jsonObject);

        // SDP
        handleSdp(jsonObject);

        // SEND ANSWER
        webSocket.sendText(createCommand(CommandType.ANSWER));

        // SEND CANDIDATE
        /**
         * ICE trickling is the process of continuing to send candidates
         *      after the initial offer or answer has already been sent to the other peer.
         */
        if (webRtcIceInfo.isRemoteIceTrickle()) {
            webSocket.sendText(createCommand(CommandType.CANDIDATE));
        }

        // STUN
        allocateMediaChannelToSession();
    }

    private void handleId(JsonObject jsonObject) {
        JsonElement rootId = jsonObject.get("id");
        if (rootId != null) {
            webRtcServiceInfo.setId(rootId.getAsLong());
        }
    }

    private void handlePeerId(JsonObject jsonObject) {
        JsonElement rootPeerId = jsonObject.get("peer_id");
        if (rootPeerId != null) {
            webRtcServiceInfo.setPeerId(rootPeerId.getAsInt());
        }
    }

    private void handleCandidates(JsonObject jsonObject) {
        JsonElement rootCandidates = jsonObject.get("candidates");
        if (rootCandidates != null) {
            JsonArray candidatesAsJsonArray = rootCandidates.getAsJsonArray();
            for (int i = 0; i < candidatesAsJsonArray.size(); i++) {
                JsonElement jsonElement = candidatesAsJsonArray.get(i);
                if (jsonElement == null) {
                    continue;
                }

                JsonObject candidateObject = jsonElement.getAsJsonObject();
                if (candidateObject != null) {
                    JsonElement candidate = candidateObject.get("candidate");
                    JsonElement sdpMid = candidateObject.get("sdpMid");
                    JsonElement sdpMLineIndex = candidateObject.get("sdpMLineIndex");

                    RTCIceCandidate rtcIceCandidate = new RTCIceCandidate(
                            sdpMid != null ? sdpMid.getAsString() : null,
                            sdpMLineIndex != null ? sdpMLineIndex.getAsInt() : 0,
                            candidate != null ? candidate.getAsString() : null
                    );

                    webRtcIceInfo.getRemoteCandidates().add(rtcIceCandidate);
                }
            }
            log.info("|WebSocketInfo({})| candidates: {}",
                    webRtcServiceInfo.getConferenceId(), webRtcIceInfo.getRemoteCandidates().toArray()
            );
        }
    }

    private void handleSdp(JsonObject jsonObject) {
        JsonElement rootSdp = jsonObject.get("sdp");
        if (rootSdp != null) {
            JsonObject rootSdpObject = rootSdp.getAsJsonObject();
            JsonElement sdp = rootSdpObject.get("sdp");
            webRtcServiceInfo.setRemoteSdp(sdp.getAsString());

            SdpParser sdpParser = SdpParser.INSTANCE;
            try {
                // REMOTE SDP
                SdpSession remoteSdpSession = createRemoteSdpSession(sdpParser);

                // LOCAL SDP
                SdpSession localSdpSession = createLocalRtcSessionDescription();

                // NEW SESSION
                if (createNewWebSocketInfo(remoteSdpSession)) { return; }

                // ICE
                setupIceInfo(localSdpSession, remoteSdpSession);
            } catch (Exception e) {
                log.warn("|WebSocketInfo({})| WebSocketInfo.handleSdp.Exception", webRtcServiceInfo.getConferenceId(), e);
            }
        }
    }

    private void allocateMediaChannelToSession() {
        if (allocateMediaChannel(
                new InetSocketAddress(localIp, webRtcIceInfo.getStunServerPort()),
                webRtcIceInfo.getIceInfo())) {
            log.debug("|WebSocketInfo({})| Success to allocate the media channel.", webRtcServiceInfo.getConferenceId());
        } else {
            log.warn("|WebSocketInfo({})| Fail to allocate the media channel.", webRtcServiceInfo.getConferenceId());
        }
    }

    private SdpSession createRemoteSdpSession(SdpParser sdpParser) {
        SdpSession remoteSdpSession = sdpParser.parse(webRtcServiceInfo.getRemoteSdp());
        log.info("|WebSocketInfo({})| Parsed Remote Sdp: \n{}", webRtcServiceInfo.getConferenceId(), remoteSdpSession.write());

        if (remoteSdpSession.getOrigin().getUsername() != null) {
            remoteRealm = remoteSdpSession.getOrigin().getUsername();
        }

        // a=ice-options:trickle
        if (remoteSdpSession.getIceOptions() != null && remoteSdpSession.getIceOptions().getValue().equals("trickle")) {
            webRtcIceInfo.setRemoteIceTrickle(true);
        }

        localPeerConnection.setRemoteDesc(remoteSdpSession);
        return remoteSdpSession;
    }

    private SdpSession createLocalRtcSessionDescription() {
        return localPeerConnection.createAnswerSdpSession();
    }

    private boolean createNewWebSocketInfo(SdpSession remoteSdpSession) {
        ConferenceInfo conferenceInfo = ConferenceMaster.getInstance().findConference(webRtcServiceInfo.getConferenceId());
        if (conferenceInfo == null) {
            log.warn("|WebSocketInfo({})| Fail to get the call info. Fail to process the sdp.", webRtcServiceInfo.getConferenceId());
            stop();
            return true;
        }

        mediaSession = new MediaSession(webRtcServiceInfo.getConferenceId(), remoteSdpSession);
        conferenceInfo.setMediaSession(mediaSession);
        return false;
    }

    private List<InetSocketAddress> createTargetAddressList() {
        List<InetSocketAddress> serverAddressList = new ArrayList<>();

        for (RTCIceCandidate remoteCandidate : webRtcIceInfo.getRemoteCandidates()) {
            String candidate = remoteCandidate.sdp;
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }

            String[] splitedCandidate = candidate.split(" ");
            if (splitedCandidate.length > 0) {
                String ip = splitedCandidate[4];
                int port = Integer.parseInt(splitedCandidate[5]);

                if ((ip != null && !ip.isEmpty()) && port > 0) {
                    serverAddressList.add(new InetSocketAddress(ip, port));
                }
            }
        }

        return serverAddressList;
    }

    private RTCIceCandidate createRtcIceCandidate(SdpSession localSdpSession) {
        return mediaSession.getRemoteSdpMediaInfo().getAudioDesc().getMid() != null ?
                /**
                 * {"id":336200207,"peer_id":0,"command":"candidate","candidates":[
                 * {"candidate":"candidate:1666098506 1 udp 2113937151 601f67ac-e8d3-4862-8352-0b1aa55f2d23.local 62860 typ host generation 0 ufrag LScd network-cost 999","sdpMid":"F3nmxZ","sdpMLineIndex":0}
                 * ]}
                 *
                 * {"id":336200207,"peer_id":0,"command":"candidate","candidates":[
                 * {"candidate":"candidate:3348106977 1 udp 2113942271 7489ed6b-21be-4406-904c-aa67ae15893c.local 62039 typ host generation 0 ufrag LScd network-cost 999","sdpMid":"F3nmxZ","sdpMLineIndex":0}
                 * ]}
                 */
                new RTCIceCandidate(
                        localSdpSession.getMedia().get(0).getMid().getValue(),
                        0,
                        "candidate:" + getRandomLongValue() + "" + getRandomLongValue() + " 1 udp"
                                + " " + getRandomLongValue() + "" + getRandomLongValue()
                                + " " + UUID.randomUUID() + ".local"
                                + " " + webRtcIceInfo.getStunServerPort()
                                + " typ host generation 0"
                                + " ufrag " + webRtcIceInfo.getIceInfo().getLocalIceUfrag()
                ) : null;
    }

    private long getRandomLongValue() {
        return RandomManager.getRandomLong(10000, 99999);
    }

    private void setupMediaPorts(SdpSession localSdpSession, long audioPort, long videoPort) {
        for (SdpMedia media : localSdpSession.getMedia()) {
            if (media == null) { continue; }

            SdpMline mline = media.getMline();
            if (mline == null) { continue; }
            String mediaType = mline.getType();
            if (mediaType.isEmpty()) { continue; }

            switch (mediaType) {
                case "video":
                    media.setMline(new SdpMline(mline.getType(), videoPort, mline.getProtocol(), mline.getPayloads()));
                    break;
                case "audio":
                    media.setMline(new SdpMline(mline.getType(), audioPort, mline.getProtocol(), mline.getPayloads()));
                    break;
                default: break;
            }

            if (media.getRtcpMux() != null) {
                SdpRtcp sdpRtcp = media.getRtcp();
                if (sdpRtcp == null) { continue; }

                switch (mediaType) {
                    case "video":
                        media.setRtcp(new SdpRtcp(videoPort, sdpRtcp.getNetType(), sdpRtcp.getIpVer(), sdpRtcp.getAddress()));
                        break;
                    case "audio":
                        media.setRtcp(new SdpRtcp(audioPort, sdpRtcp.getNetType(), sdpRtcp.getIpVer(), sdpRtcp.getAddress()));
                        break;
                    default: break;
                }
            }
        }
    }

    private void setupIceInfo(SdpSession localSdpSession, SdpSession remoteSdpSession) {
        // MEDIA
        webRtcIceInfo.setStunServerPort(WebSocketPortManager.getInstance().takePort());
        mediaSession.setLocalMediaAddress(new InetSocketAddress(localIp, webRtcIceInfo.getStunServerPort()));

        IceInfo iceInfo = webRtcIceInfo.getIceInfo();

        if (localSdpSession.getMedia().get(0).getIceUfrag() != null) {
            iceInfo.setLocalIceUfrag(localSdpSession.getMedia().get(0).getIceUfrag().getValue());
        } else if (localSdpSession.getIceUfrag() != null) {
            iceInfo.setLocalIceUfrag(localSdpSession.getIceUfrag().getValue());
        }

        if (localSdpSession.getMedia().get(0).getIcePwd() != null) {
            iceInfo.setLocalIcePasswd(localSdpSession.getMedia().get(0).getIcePwd().getValue());
        } else if (localSdpSession.getIcePwd() != null) {
            iceInfo.setLocalIcePasswd(localSdpSession.getIcePwd().getValue());
        }

        if (remoteSdpSession.getMedia().get(0).getIceUfrag() != null) {
            iceInfo.setRemoteIceUfrag(remoteSdpSession.getMedia().get(0).getIceUfrag().getValue());
        } else if (remoteSdpSession.getIceUfrag() != null) {
            iceInfo.setRemoteIceUfrag(remoteSdpSession.getIceUfrag().getValue());
        }

        if (remoteSdpSession.getMedia().get(0).getIcePwd() != null) {
            iceInfo.setRemoteIcePasswd(remoteSdpSession.getMedia().get(0).getIcePwd().getValue());
        } else if (remoteSdpSession.getIcePwd() != null) {
            iceInfo.setRemoteIcePasswd(remoteSdpSession.getIcePwd().getValue());
        }

        if (iceInfo.getLocalIceUfrag() != null && iceInfo.getRemoteIceUfrag() != null) {
            iceInfo.setRemoteUsername(iceInfo.getRemoteIceUfrag() + ":" + iceInfo.getLocalIceUfrag());
        }

        log.debug("|WebSocketInfo({})| Stun server port : {}", webRtcServiceInfo.getConferenceId(), webRtcIceInfo.getStunServerPort());
        RTCIceCandidate rtcIceCandidate = createRtcIceCandidate(localSdpSession);
        webRtcIceInfo.getLocalCandidates().add(rtcIceCandidate);

        webRtcServiceInfo.setLocalSdp(localSdpSession.write());
        log.info("|WebSocketInfo({})| Parsed Local Sdp: \n{}", webRtcServiceInfo.getConferenceId(), webRtcServiceInfo.getLocalSdp());
    }

}
