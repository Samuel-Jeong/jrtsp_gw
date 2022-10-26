package org.kkukie.jrtsp_gw.media.webrtc.websocket.service;

import com.google.gson.*;
import com.neovisionaries.ws.client.*;
import lombok.extern.slf4j.Slf4j;
import media.core.rtsp.sdp.*;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.config.DefaultConfig;
import org.kkukie.jrtsp_gw.config.DtlsConfig;
import org.kkukie.jrtsp_gw.media.stream.manager.ChannelMaster;
import org.kkukie.jrtsp_gw.media.stream.util.WebSocketPortManager;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.command.*;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.model.IceInfo;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.module.RTCIceCandidate;
import org.kkukie.jrtsp_gw.media.webrtc.websocket.service.module.RTCPeerConnection;
import org.kkukie.jrtsp_gw.session.SessionManager;
import org.kkukie.jrtsp_gw.session.call.CallInfo;
import org.kkukie.jrtsp_gw.session.media.MediaInfo;
import org.kkukie.jrtsp_gw.util.RandomManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class WebSocketService {

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final String URI;
    private final String APPLICATION_NAME;

    private String callId = null;

    private final RTCPeerConnection localPeerConnection;

    private long id;
    private int peerId;
    private String localSdp;
    private String remoteSdp;

    private boolean isRemoteIceTrickle = false;
    private final List<RTCIceCandidate> localCandidates = new ArrayList<>();
    private final List<RTCIceCandidate> remoteCandidates = new ArrayList<>();

    private Queue<InetSocketAddress> targetAddressQueue = new ConcurrentLinkedQueue<>();

    private final IceInfo iceInfo;

    private WebSocket webSocket = null;

    private MediaInfo mediaInfo = null;

    private String localIp = null;
    private int localPort = 0;
    private int stunServerPort = 0;

    private String remoteRealm = null;

    private ChannelMaster channelMaster = null;

    public WebSocketService() {
        DefaultConfig defaultConfig = ConfigManager.getDefaultConfig();
        URI = defaultConfig.getServerUri();
        APPLICATION_NAME = defaultConfig.getApplicationName();

        iceInfo = new IceInfo();
        localPeerConnection = new RTCPeerConnection();

        DtlsConfig dtlsConfig = ConfigManager.getDtlsConfig();
        localPeerConnection.setCertPath(dtlsConfig.getCertPath());
    }

    public void start(String callId) throws WebSocketException, IOException {
        if (callId == null || callId.isEmpty()) { return; }

        this.callId = callId;
        webSocket = connect(URI + "/" + APPLICATION_NAME + "/" + callId);

        localIp = webSocket.getSocket().getLocalAddress().getHostAddress();
        localPort = webSocket.getSocket().getLocalPort();

        if (channelMaster != null) {
            channelMaster.stop();
            channelMaster = null;
        }
        channelMaster = new ChannelMaster(callId);
        channelMaster.start();

        log.debug("|WebSocketService({})| LOCAL NETWORK = {}:{}", callId, localIp, localPort);
    }

    public void handshake() {
        // SEND REQUEST_OFFER
        webSocket.sendText(createCommand(CommandType.REQUEST_OFFER));
    }

    public void stop() {
        if (callId != null) {
            SessionManager.getInstance().deleteCall(callId);
        }

        if (mediaInfo != null) {
            SessionManager.getInstance().deleteCall(mediaInfo.getCallId());
            mediaInfo.freeMediaChannel();
            mediaInfo = null;
        }

        channelMaster.stop();

        if (webSocket != null) {
            webSocket.disconnect();
            webSocket = null;
        }
    }

    /**
     * Connect to the server.
     */
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
                        log.warn("|WebSocketService({})| DISCONNECTED or CLOSED by closedByServer({})", callId, closedByServer);
                        SessionManager.getInstance().deleteCall(callId);
                    }
                }).setPingInterval(3000)
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                .connect();
    }

    private void handleData(String message) {
        JsonElement jsonElement = JsonParser.parseString(message);
        log.info("|WebSocketService({})| Message >> {}", callId, gson.toJson(jsonElement));
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

    private String createCommand(CommandType type) {
        switch (type) {
            case REQUEST_OFFER:
                return (new OmeRequestOffer()).makeJson();
            case OFFER:
                return (new OmeOffer()).makeJson();
            case ANSWER:
                return isRemoteIceTrickle ?
                        (new OmeAnswer(id, peerId, localSdp, null)).makeJson() :
                        (new OmeAnswer(id, peerId, localSdp, localCandidates)).makeJson();
            case CANDIDATE:
                return (new OmeCandidate(id, peerId, localCandidates)).makeJson();
            default:
                return "";
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
        if (isRemoteIceTrickle) {
            webSocket.sendText(createCommand(CommandType.CANDIDATE));
        }

        // STUN
        allocateMediaChannel();
    }

    private void allocateMediaChannel() {
        if (mediaInfo.allocateMediaChannel(new InetSocketAddress(localIp, stunServerPort), iceInfo, targetAddressQueue)) {
            log.debug("|WebSocketService({})| Success to allocate the media channel.", callId);
        } else {
            log.warn("|WebSocketService({})| Fail to allocate the media channel.", callId);
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

                    remoteCandidates.add(rtcIceCandidate);
                }
            }
            log.info("|WebSocketService({})| candidates: {}", callId, remoteCandidates.toArray());
        }
    }

    private void handleSdp(JsonObject jsonObject) {
        JsonElement rootSdp = jsonObject.get("sdp");
        if (rootSdp != null) {
            JsonObject rootSdpObject = rootSdp.getAsJsonObject();
            JsonElement sdp = rootSdpObject.get("sdp");
            this.remoteSdp = sdp.getAsString();

            SdpParser sdpParser = SdpParser.INSTANCE;
            try {
                // REMOTE SDP
                SdpSession remoteSdpSession = createRemoteSdpSession(sdpParser);

                // LOCAL SDP
                SdpSession localSdpSession = createLocalRtcSessionDescription();

                // NEW SESSION
                if (createNewSession(remoteSdpSession)) { return; }

                // ICE
                setupIceInfo(localSdpSession, remoteSdpSession);

                // STUN
                List<InetSocketAddress> serverAddressList = createTargetAddressQueue();
                log.debug("|WebSocketService({})| serverAddressList: {} ({})", callId, serverAddressList.toArray(), serverAddressList.size());
            } catch (Exception e) {
                log.warn("|WebSocketService({})| WebSocketService.handleSdp.Exception", callId, e);
            }
        }
    }

    private SdpSession createRemoteSdpSession(SdpParser sdpParser) {
        SdpSession remoteSdpSession = sdpParser.parse(this.remoteSdp);
        log.info("|WebSocketService({})| Parsed Remote Sdp: \n{}", callId, remoteSdpSession.write());

        if (remoteSdpSession.getOrigin().getUsername() != null) {
            remoteRealm = remoteSdpSession.getOrigin().getUsername();
        }

        // a=ice-options:trickle
        if (remoteSdpSession.getIceOptions() != null && remoteSdpSession.getIceOptions().getValue().equals("trickle")) {
            isRemoteIceTrickle = true;
        }

        localPeerConnection.setRemoteDesc(remoteSdpSession);
        return remoteSdpSession;
    }

    private SdpSession createLocalRtcSessionDescription() {
        return localPeerConnection.createAnswerSdpSession();
    }

    private boolean createNewSession(SdpSession remoteSdpSession) {
        CallInfo callInfo = SessionManager.getInstance().findCall(callId);
        if (callInfo == null) {
            log.warn("|WebSocketService({})| Fail to get the call info. Fail to process the sdp.", callInfo);
            stop();
            return true;
        }

        mediaInfo = new MediaInfo(channelMaster, callId, remoteSdpSession);
        callInfo.setMediaInfo(mediaInfo);
        return false;
    }

    private List<InetSocketAddress> createTargetAddressQueue() {
        List<InetSocketAddress> serverAddressList = new ArrayList<>();

        for (RTCIceCandidate remoteCandidate : remoteCandidates) {
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
        targetAddressQueue.clear();
        targetAddressQueue = new ConcurrentLinkedQueue<>(serverAddressList);
        return serverAddressList;
    }

    private RTCIceCandidate createRtcIceCandidate(SdpSession localSdpSession) {
        return mediaInfo.getRemoteAudioDesc().getMid() != null ?
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
                                + " " + stunServerPort
                                + " typ host generation 0"
                                + " ufrag " + iceInfo.getLocalIceUfrag()
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
        stunServerPort = WebSocketPortManager.getInstance().takePort();
        mediaInfo.setLocalMediaAddress(new InetSocketAddress(localIp, stunServerPort));

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

        log.debug("|WebSocketService({})| Stun server port : {}", callId, stunServerPort);
        RTCIceCandidate rtcIceCandidate = createRtcIceCandidate(localSdpSession);
        localCandidates.add(rtcIceCandidate);

        this.localSdp = localSdpSession.write();
        log.info("|WebSocketService({})| Parsed Local Sdp: \n{}", callId, this.localSdp);
    }

    private void handlePeerId(JsonObject jsonObject) {
        JsonElement rootPeerId = jsonObject.get("peer_id");
        if (rootPeerId != null) {
            this.peerId = rootPeerId.getAsInt();
        }
    }

    private void handleId(JsonObject jsonObject) {
        JsonElement rootId = jsonObject.get("id");
        if (rootId != null) {
            this.id = rootId.getAsLong();
        }
    }


}