# jrtsp_gw
- RTSP Gateway with WAS
- We can get the stream from OvenMediaEngine with multiple clients
  
## REFERENCE
~~~
- Thanks to [ RestComm/media-core ]
  # https://github.com/RestComm/media-core

- Thanks to [ bouncycastle.org ]
  # https://www.bouncycastle.org/java.html
~~~
  
## FLOW
- WebRTC to RTSP
  ![195779791-7e6b3c0d-20c2-455f-ae14-a74b9a0fa4c9](https://user-images.githubusercontent.com/37236920/196082925-da256ce6-ce80-4e71-942e-823c08a6f010.png)
  
## HOW-TO
~~~
1. Lauch the OvenMediaEngine service on your media server.
  > Must install the ome service first...
2. Start to stream your media to the engine. (video & audio)

Example) Publish stream with OBS
- stream key : abcd1234
- Resolution : 720p (1280x720)
~~~
  
<img width="850" alt="스크린샷 2022-10-21 오전 8 28 46" src="https://user-images.githubusercontent.com/37236920/197077896-c64de780-7923-4d29-812c-169fe665d229.png">
  
~~~
[OME Log]

[2022-10-21 08:11:23.911] I [OutboundWorker:12712] MediaRouter | mediarouter_application.cpp:821  | [#default#app/abcd1234(2327242597)] Stream has been created
[Stream Info]
id(2327242597), msid(0), output(abcd1234), SourceType(Transcoder), RepresentationType(Source), Created Time (Fri Oct 21 08:11:23 2022) UUID(9fa03964-4f71-4a06-a29f-7ab487bf08c4/default/#default#app/abcd1234/o)
        >> Origin Stream Info
        id(300), output(abcd1234), SourceType(Rtmp), Created Time (Fri Oct 21 08:11:23 2022)

        Video Track #0: Name(bypass_video) Bitrate(2.50Mb) Codec(1,H264,Passthrough) BSF(H264_AVCC) Resolution(1280x720) Framerate(30.00fps) KeyInterval(0) BFrames(0) timebase(1/1000)
        Video Track #1: Name(video_720) Bitrate(2.02Mb) Codec(1,H264,OpenH264) BSF(H264_ANNEXB) Resolution(1280x720) Framerate(30.00fps) KeyInterval(0) BFrames(0) timebase(1/90000)
        Audio Track #2: Name(bypass_audio) Bitrate(160.00Kb) Codec(6,AAC,Passthrough) BSF(AAC_RAW) Samplerate(48.0K) Format(fltp, 32) Channel(stereo, 2) timebase(1/1000)
        Audio Track #3: Name(audio_0) Bitrate(128.00Kb) Codec(8,OPUS,libopus) BSF(OPUS) Samplerate(48.0K) Format(s16, 16) Channel(stereo, 2) timebase(1/48000)
[2022-10-21 08:11:23.914] I [OutboundWorker:12712] WebRTC Publisher | rtc_stream.cpp:198  | WebRTC Stream has been created : abcd1234/2327242597
Rtx(false) Ulpfec(false) JitterBuffer(true) PlayoutDelay(false min:0 max: 0)
[2022-10-21 08:11:23.914] I [OutboundWorker:12712] Publisher | stream.cpp:204  | WebRTC Publisher Application application has started [abcd1234(2327242597)] stream (MSID : 0)
[2022-10-21 08:11:23.915] I [OutboundWorker:12712] Publisher | stream.cpp:204  | OVTPublisher Application application has started [abcd1234(2327242597)] stream (MSID : 0)


3. Request the stream to [jrtsp_gw] by [RTSP Client(vlc)].

Example) Provided stream with VLC
- Stream URI : rtsp://127.0.0.1:8554/abcd1234
~~~
  
<img width="797" alt="스크린샷 2022-10-21 오전 8 42 25" src="https://user-images.githubusercontent.com/37236920/197079397-80974f17-a82f-418e-9352-aee115e972d8.png">
  
~~~
[JRTSP_GW Log]

1) Recv OPTIONS Request from VLC & Send Response to VLC

08:12:15.754 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () < OPTIONS
DefaultHttpRequest(decodeResult: success, version: RTSP/1.0)
OPTIONS rtsp://127.0.0.1:8554/abcd1234 RTSP/1.0
CSeq: 2
User-Agent: LibVLC/3.0.17.3 (LIVE555 Streaming Media v2016.11.28)
08:12:15.790 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) [OPTIONS] > Success to send the response: DefaultFullHttpResponse(decodeResult: success, version: RTSP/1.0, content: UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf(ridx: 0, widx: 0, cap: 0))
RTSP/1.0 200 OK
public: OPTIONS, DESCRIBE, SETUP, PLAY, TEARDOWN
cseq: 2
connection: keep-alive
date: 2022-10-21T08:12:15.790561
cache-control: no-cache


2) Recv DESCRIBE Request from VLC

08:12:15.793 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () < DESCRIBE
DefaultHttpRequest(decodeResult: success, version: RTSP/1.0)
DESCRIBE rtsp://127.0.0.1:8554/abcd1234 RTSP/1.0
CSeq: 3
User-Agent: LibVLC/3.0.17.3 (LIVE555 Streaming Media v2016.11.28)
Accept: application/sdp
08:12:15.794 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () Call-ID: abcd1234


3) Send REQUEST_OFFER to OME & Recv OFFER from OME

08:12:17.610 [nioEventLoopGroup-2-1] INFO umedia.core.scheduler.ServiceScheduler - Started scheduler!
08:12:17.643 [nioEventLoopGroup-2-1] DEBUG media.core.stream.manager.ChannelMaster - |ChannelMaster(abcd1234)| Started.
08:12:17.645 [nioEventLoopGroup-2-1] DEBUG media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| LOCAL NETWORK = 10.254.110.220:49708
08:12:17.668 [nioEventLoopGroup-2-1] DEBUG media.core.webrtc.websocket.command.OmeRequestOffer - OmeRequestOffer: 
{
  "command": "request_offer"
}
08:12:17.746 [MAIN_JobExecutor-0] DEBUG service.monitor.HaHandler - | cpu=[41.48], mem=[15.44/4096.00], thread=[27] | CallCount=[1]
08:12:17.753 [ReadingThread] INFO media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| Message >> {
  "candidates": [
    {
      "candidate": "candidate:0 1 UDP 50 222.235.208.2 30001 typ host",
      "sdpMLineIndex": 0
    },
    {
      "candidate": "candidate:0 1 UDP 50 192.168.5.224 30001 typ host",
      "sdpMLineIndex": 0
    },
    {
      "candidate": "candidate:0 1 UDP 50 100.100.100.224 30001 typ host",
      "sdpMLineIndex": 0
    },
    {
      "candidate": "candidate:0 1 UDP 50 200.200.200.224 30001 typ host",
      "sdpMLineIndex": 0
    }
  ],
  "code": 200,
  "command": "offer",
  "id": 398464881,
  "peer_id": 0,
  "sdp": {
    "sdp": "v\u003d0\r\no\u003dOvenMediaEngine 101 2 IN IP4 127.0.0.1\r\ns\u003d-\r\nt\u003d0 0\r\na\u003dgroup:BUNDLE Namq79 zqiyfb\r\na\u003dgroup:LS Namq79 zqiyfb\r\na\u003dmsid-semantic:WMS x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0\r\na\u003dfingerprint:sha-256 28:C2:15:E1:99:22:8B:03:4C:FD:37:6D:BB:8F:51:A6:F6:ED:82:D7:D5:11:6C:7E:D6:52:FB:9E:53:5E:B3:29\r\na\u003dice-options:trickle\r\na\u003dice-ufrag:lBkCdN\r\na\u003dice-pwd:dUJkHe6WnD9BGqv5wy1c2YAT3jXtSVPx\r\nm\u003dvideo 9 UDP/TLS/RTP/SAVPF 98\r\nc\u003dIN IP4 0.0.0.0\r\na\u003dsendonly\r\na\u003dmid:Namq79\r\na\u003dsetup:actpass\r\na\u003drtcp-mux\r\na\u003drtcp-rsize\r\na\u003dmsid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v\r\na\u003dextmap:1 urn:ietf:params:rtp-hdrext:framemarking\r\na\u003dextmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na\u003drtpmap:98 H264/90000\r\na\u003dfmtp:98 packetization-mode\u003d1;profile-level-id\u003d42e01f;level-asymmetry-allowed\u003d1\r\na\u003drtcp-fb:98 goog-remb\r\na\u003dssrc:2346570055 cname:mhfc98pgYLJuHO4t\r\na\u003dssrc:2346570055 msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v\r\na\u003dssrc:2346570055 mslabel:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0\r\na\u003dssrc:2346570055 label:n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v\r\nm\u003daudio 9 UDP/TLS/RTP/SAVPF 110\r\nc\u003dIN IP4 0.0.0.0\r\na\u003dsendonly\r\na\u003dmid:zqiyfb\r\na\u003dsetup:actpass\r\na\u003drtcp-mux\r\na\u003drtcp-rsize\r\na\u003dmsid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B\r\na\u003dextmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na\u003drtpmap:110 OPUS/48000/2\r\na\u003dfmtp:110 sprop-stereo\u003d1;stereo\u003d1;minptime\u003d10;useinbandfec\u003d1\r\na\u003dssrc:1357213020 cname:mhfc98pgYLJuHO4t\r\na\u003dssrc:1357213020 msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B\r\na\u003dssrc:1357213020 mslabel:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0\r\na\u003dssrc:1357213020 label:ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B\r\n",
    "type": "offer"
  }
}
08:12:17.758 [ReadingThread] INFO media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| candidates: [RTCIceCandidate@1901779946 [sdpMid=null, sdpMLineIndex=0, sdp=candidate:0 1 UDP 50 222.235.208.2 30001 typ host, serverUrl=null], RTCIceCandidate@835514348 [sdpMid=null, sdpMLineIndex=0, sdp=candidate:0 1 UDP 50 192.168.5.224 30001 typ host, serverUrl=null], RTCIceCandidate@59414389 [sdpMid=null, sdpMLineIndex=0, sdp=candidate:0 1 UDP 50 100.100.100.224 30001 typ host, serverUrl=null], RTCIceCandidate@1222808019 [sdpMid=null, sdpMLineIndex=0, sdp=candidate:0 1 UDP 50 200.200.200.224 30001 typ host, serverUrl=null]]
08:12:17.947 [ReadingThread] INFO media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| Parsed Remote Sdp: 
v=0
o=OvenMediaEngine 101 2 IN IP4 127.0.0.1
s=-
t=0 0
a=ice-ufrag:lBkCdN
a=ice-pwd:dUJkHe6WnD9BGqv5wy1c2YAT3jXtSVPx
a=fingerprint:sha-256 28:C2:15:E1:99:22:8B:03:4C:FD:37:6D:BB:8F:51:A6:F6:ED:82:D7:D5:11:6C:7E:D6:52:FB:9E:53:5E:B3:29
a=ice-options:trickle
a=msid-semantic: WMS x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0
a=group:BUNDLE Namq79 zqiyfb
a=group:LS Namq79 zqiyfb
m=video 9 UDP/TLS/RTP/SAVPF 98
c=IN IP4 0.0.0.0
a=rtpmap:98 H264/90000
a=fmtp:98 packetization-mode=1;profile-level-id=42e01f;level-asymmetry-allowed=1
a=rtcp-fb:98 goog-remb
a=extmap:1 urn:ietf:params:rtp-hdrext:framemarking
a=extmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
a=setup:actpass
a=mid:Namq79
a=msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v
a=sendonly
a=ssrc:2346570055 cname:mhfc98pgYLJuHO4t
a=ssrc:2346570055 msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v
a=ssrc:2346570055 mslabel:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0
a=ssrc:2346570055 label:n9VmA0YoipgxDqBW4wQCPrJI5deMjfZ7EH1v
a=rtcp-mux
a=rtcp-rsize
m=audio 9 UDP/TLS/RTP/SAVPF 110
c=IN IP4 0.0.0.0
a=rtpmap:110 OPUS/48000/2
a=fmtp:110 sprop-stereo=1;stereo=1;minptime=10;useinbandfec=1
a=extmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
a=setup:actpass
a=mid:zqiyfb
a=msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B
a=sendonly
a=ssrc:1357213020 cname:mhfc98pgYLJuHO4t
a=ssrc:1357213020 msid:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0 ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B
a=ssrc:1357213020 mslabel:x4Nz7KqwiePrS3Rl8kG1YWcnMuLO2Z6UmAf0
a=ssrc:1357213020 label:ZyKMxqnv6tcW3mDGJRVIT14L0oUugHNSOf9B
a=rtcp-mux
a=rtcp-rsize

08:12:18.010 [ReadingThread] DEBUG session.MediaInfo - |MediaInfo(abcd1234)| priorityRtp: SdpRtp(payload=98, codec=H264, rate=90000, encoding=null), remoteVideoRtpInfo: H264/90000
08:12:18.020 [ReadingThread] DEBUG session.MediaInfo - |MediaInfo(abcd1234)| remoteVideoDesc.getMid: Namq79
08:12:18.021 [ReadingThread] DEBUG session.MediaInfo - |MediaInfo(abcd1234)| priorityRtp: SdpRtp(payload=110, codec=OPUS, rate=48000, encoding=2), remoteAudioRtpInfo: OPUS/48000/2
08:12:18.021 [ReadingThread] DEBUG session.MediaInfo - |MediaInfo(abcd1234)| remoteAudioDesc.getMid: zqiyfb
08:12:18.021 [ReadingThread] DEBUG media.core.stream.util.WebSocketPortManager - |WebSocketPortManager| Success to get port(=50000) resource in Queue.
08:12:18.021 [ReadingThread] DEBUG media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| Stun server port : 50000
08:12:18.045 [ReadingThread] INFO media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| Parsed Local Sdp: 
v=0
o=- 5390844643874690457 2 IN IP4 127.0.0.1
s=-
t=0 0
a=msid-semantic:  WMS
a=group:BUNDLE Namq79 zqiyfb
m=video 9 UDP/TLS/RTP/SAVPF 98
c=IN IP4 0.0.0.0
a=rtpmap:98 H264/90000
a=fmtp:98 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f
a=rtcp:9 IN IP4 0.0.0.0
a=rtcp-fb:98 goog-remb
a=extmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
a=setup:active
a=mid:Namq79
a=recvonly
a=ice-ufrag:stQP
a=ice-pwd:iofyzd2rdUjIL+N6TASfyp6k
a=fingerprint:sha-256 4E:AF:E8:F6:89:8E:92:D6:52:99:EC:44:F3:C6:77:76:E1:74:AA:19:5F:12:86:69:FE:66:87:FE:94:AA:9B:50
a=ice-options:trickle
a=rtcp-mux
a=rtcp-rsize
m=audio 9 UDP/TLS/RTP/SAVPF 110
c=IN IP4 0.0.0.0
a=rtpmap:110 OPUS/48000/2
a=fmtp:110 minptime=10;useinbandfec=1
a=rtcp:9 IN IP4 0.0.0.0
a=extmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
a=setup:active
a=mid:zqiyfb
a=recvonly
a=ice-ufrag:stQP
a=ice-pwd:iofyzd2rdUjIL+N6TASfyp6k
a=fingerprint:sha-256 4E:AF:E8:F6:89:8E:92:D6:52:99:EC:44:F3:C6:77:76:E1:74:AA:19:5F:12:86:69:FE:66:87:FE:94:AA:9B:50
a=ice-options:trickle
a=rtcp-mux

08:12:18.047 [ReadingThread] DEBUG media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| serverAddressList: [/222.235.208.2:30001, /192.168.5.224:30001, /100.100.100.224:30001, /200.200.200.224:30001] (4)


4) Send ANSWER & CANDIDATE to OME (set trickle mode by ome offer)

08:12:18.055 [ReadingThread] DEBUG media.core.webrtc.websocket.command.OmeAnswer - OmeAnswer: 
{
  "id": 398464881,
  "peer_id": 0,
  "command": "answer",
  "sdp": {
    "type": "answer",
    "sdp": "v\u003d0\r\no\u003d- 5390844643874690457 2 IN IP4 127.0.0.1\r\ns\u003d-\r\nt\u003d0 0\r\na\u003dmsid-semantic:  WMS\r\na\u003dgroup:BUNDLE Namq79 zqiyfb\r\nm\u003dvideo 9 UDP/TLS/RTP/SAVPF 98\r\nc\u003dIN IP4 0.0.0.0\r\na\u003drtpmap:98 H264/90000\r\na\u003dfmtp:98 level-asymmetry-allowed\u003d1;packetization-mode\u003d1;profile-level-id\u003d42e01f\r\na\u003drtcp:9 IN IP4 0.0.0.0\r\na\u003drtcp-fb:98 goog-remb\r\na\u003dextmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na\u003dsetup:active\r\na\u003dmid:Namq79\r\na\u003drecvonly\r\na\u003dice-ufrag:stQP\r\na\u003dice-pwd:iofyzd2rdUjIL+N6TASfyp6k\r\na\u003dfingerprint:sha-256 4E:AF:E8:F6:89:8E:92:D6:52:99:EC:44:F3:C6:77:76:E1:74:AA:19:5F:12:86:69:FE:66:87:FE:94:AA:9B:50\r\na\u003dice-options:trickle\r\na\u003drtcp-mux\r\na\u003drtcp-rsize\r\nm\u003daudio 9 UDP/TLS/RTP/SAVPF 110\r\nc\u003dIN IP4 0.0.0.0\r\na\u003drtpmap:110 OPUS/48000/2\r\na\u003dfmtp:110 minptime\u003d10;useinbandfec\u003d1\r\na\u003drtcp:9 IN IP4 0.0.0.0\r\na\u003dextmap:4 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\r\na\u003dsetup:active\r\na\u003dmid:zqiyfb\r\na\u003drecvonly\r\na\u003dice-ufrag:stQP\r\na\u003dice-pwd:iofyzd2rdUjIL+N6TASfyp6k\r\na\u003dfingerprint:sha-256 4E:AF:E8:F6:89:8E:92:D6:52:99:EC:44:F3:C6:77:76:E1:74:AA:19:5F:12:86:69:FE:66:87:FE:94:AA:9B:50\r\na\u003dice-options:trickle\r\na\u003drtcp-mux\r\n"
  }
}
08:12:18.056 [ReadingThread] DEBUG media.core.webrtc.websocket.command.OmeCandidate - OmeCandidate: 
{
  "id": 398464881,
  "peer_id": 0,
  "command": "candidate",
  "candidates": [
    {
      "candidate": "candidate:1943860970 1 udp 7999815546 3db4c579-7dcd-48f7-baba-2037bfc47e70.local 50000 typ host generation 0 ufrag stQP",
      "sdpMid": "Namq79",
      "sdpMLineIndex": 0
    }
  ]
}
08:12:18.061 [ReadingThread] DEBUG media.core.stream.manager.ChannelMaster - |ChannelMaster(abcd1234)| Success to register the channel. (localAddress=/10.254.110.220:50000, isBlocking=false)
08:12:18.067 [ReadingThread] DEBUG media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| Success to allocate the media channel.


5) Recv some NOTIFICATION from OME > not useful yet...

08:12:18.068 [ReadingThread] INFO media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| Message >> {
  "command": "notification",
  "message": {
    "auto": false,
    "file_name": "Bpb80Lov",
    "name": "Bpb80Lov",
    "renditions": [
      {
        "audio_track": {
          "audio": {
            "bitrate": "128000",
            "bypass": false,
            "channel": 2,
            "codec": "OPUS",
            "samplerate": 48000
          },
          "id": 3,
          "name": "audio_0",
          "type": "Audio"
        },
        "name": "default",
        "video_track": {
          "id": 0,
          "name": "bypass_video",
          "type": "Video",
          "video": {
            "bypass": true
          }
        }
      }
    ]
  },
  "type": "playlist"
}
08:12:18.069 [ReadingThread] INFO media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| Message >> {
  "command": "notification",
  "message": {
    "auto": false,
    "rendition_name": "default"
  },
  "type": "rendition_changed"
}


6) Process DTLS Handshake

08:12:18.427 [DTLS-Client-1] DEBUG umedia.core.rtp.crypto.DtlsSrtpClient - client notifyServerCertificate
08:12:18.591 [DTLS-Client-1] DEBUG umedia.core.rtp.crypto.DtlsSrtpClient - DtlsSrtpClient: Preparing SRTP Shared Secret...
08:12:18.592 [DTLS-Client-1] DEBUG umedia.core.rtp.crypto.DtlsSrtpClient - DtlsSrtpClient: SRTP Policy [authType=1] [encType=1]
08:12:18.592 [DTLS-Client-1] DEBUG umedia.core.rtp.crypto.DtlsSrtpClient - DtlsSrtpClient: Done.
08:12:18.599 [DTLS-Client-1] DEBUG media.core.stream.model.DataChannel - |DataChannel(abcd1234)| DTLS handshake completed for RTP candidate.


[OME Log]

[2022-10-21 08:12:17.612] I [SPRtcSig-T9101:12418] Signalling | rtc_signalling_server.cpp:231  | New client is connected: <ClientSocket: 0x7f7168000b80, #301, Connected, TCP, Nonblocking, 192.168.5.249:49708>
[2022-10-21 08:12:18.084] I [SPRtcSig-T9101:12418] Monitor | stream_metrics.cpp:116  | A new session has started playing #default#app/abcd1234 on the WebRTC publisher. WebRTC(1)/Stream total(1)/App total(1)
[2022-10-21 08:12:18.167] I [SPICE-U30001:12440] ICE | ice_port.cpp:779  | Add the client to the port list: 192.168.5.249:50000 / Packet type : 0 GateType : 0


7) Send DESCRIBE Response to VLC

08:12:18.682 [nioEventLoopGroup-2-1] DEBUG config.base.DefaultConfig - this.audio: audio %d RTP/AVP %d / audioPayloadType: 110 / audioRtpInfo: OPUS/48000/2
08:12:18.682 [nioEventLoopGroup-2-1] DEBUG config.base.DefaultConfig - this.video: video %d RTP/AVP %d / videoPayloadType: 98 / videoRtpInfo: H264/90000
08:12:18.685 [nioEventLoopGroup-2-1] DEBUG config.base.DefaultConfig - (0) Local SDP=
v=0
o=W_AMF_0 1666307538677 0 IN IP4 127.0.0.1
s=streaming
t=0 0
c=IN IP4 127.0.0.1
m=audio 0 RTP/AVP 110
a=rtpmap:110 OPUS/48000/2
a=control:trackID=1
a=ptime:20
a=sendonly
m=video 0 RTP/AVP 98
a=rtpmap:98 H264/90000
a=control:trackID=2
a=sendonly

08:12:18.685 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) [DESCRIBE] > Success to send the response: DefaultFullHttpResponse(decodeResult: success, version: RTSP/1.0, content: UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf(ridx: 0, widx: 263, cap: 512))
RTSP/1.0 200 OK
content-type: application/sdp
content-length: 263
cseq: 3
connection: keep-alive
date: 2022-10-21T08:12:18.685493
cache-control: no-cache


8) Recv SETUP Request from VLC & Send Response to VLC

08:12:18.690 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () < SETUP
DefaultHttpRequest(decodeResult: success, version: RTSP/1.0)
SETUP rtsp://127.0.0.1:8554/abcd1234/trackID=1 RTSP/1.0
CSeq: 4
User-Agent: LibVLC/3.0.17.3 (LIVE555 Streaming Media v2016.11.28)
Transport: RTP/AVP;unicast;client_port=54650-54651
08:12:18.690 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () Call-ID: abcd1234/trackID=1
08:12:18.690 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - conferenceId: abcd1234, trackId: 1
08:12:18.690 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) Current sessionId is [438758].
08:12:18.696 [nioEventLoopGroup-2-1] DEBUG media.core.stream.util.WebSocketPortManager - |WebSocketPortManager| Success to get port(=50002) resource in Queue.
08:12:18.706 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:1:438758)| Streamer(audio) is created. (conferenceId=abcd1234, trackId=1, localNetworkInfo={
  "listenIp": "127.0.0.1",
  "listenPort": 50002,
  "isTcp": false
})
08:12:18.709 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (abcd1234:1:438758) AudioContextStreamer is created. (sessionId=438758)
08:12:18.709 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) (abcd1234:1:438758) IPv4: /127.0.0.1
08:12:18.709 [nioEventLoopGroup-2-1] WARN media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) (abcd1234:1:438758) Destination ip is [127.0.0.1].
08:12:18.710 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) [SETUP] > Success to send the response: DefaultFullHttpResponse(decodeResult: success, version: RTSP/1.0, content: UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf(ridx: 0, widx: 0, cap: 0))
RTSP/1.0 200 OK
transport: RTP/AVP;unicast;client_port=54650-54651
session: 438758
cseq: 4
connection: keep-alive
date: 2022-10-21T08:12:18.710058
cache-control: no-cache

08:12:18.710 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) (abcd1234:1:438758) Success to setup the udp stream. (rtpDestIp=127.0.0.1, rtpDestPort=54650, rtcpDestPort=54651)
08:12:18.712 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () < SETUP
DefaultHttpRequest(decodeResult: success, version: RTSP/1.0)
SETUP rtsp://127.0.0.1:8554/abcd1234/trackID=2 RTSP/1.0
CSeq: 5
User-Agent: LibVLC/3.0.17.3 (LIVE555 Streaming Media v2016.11.28)
Transport: RTP/AVP;unicast;client_port=59782-59783
Session: 438758
08:12:18.712 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () Call-ID: abcd1234/trackID=2
08:12:18.712 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - conferenceId: abcd1234, trackId: 2
08:12:18.712 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) Current sessionId is [438758].
08:12:18.712 [nioEventLoopGroup-2-1] DEBUG media.core.stream.util.WebSocketPortManager - |WebSocketPortManager| Success to get port(=50004) resource in Queue.
08:12:18.713 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:2:438758)| Streamer(video) is created. (conferenceId=abcd1234, trackId=2, localNetworkInfo={
  "listenIp": "127.0.0.1",
  "listenPort": 50004,
  "isTcp": false
})
08:12:18.715 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (abcd1234:2:438758) VideoContextStreamer is created. (sessionId=438758)
08:12:18.715 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) (abcd1234:2:438758) IPv4: /127.0.0.1
08:12:18.715 [nioEventLoopGroup-2-1] WARN media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) (abcd1234:2:438758) Destination ip is [127.0.0.1].
08:12:18.715 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) [SETUP] > Success to send the response: DefaultFullHttpResponse(decodeResult: success, version: RTSP/1.0, content: UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf(ridx: 0, widx: 0, cap: 0))
RTSP/1.0 200 OK
transport: RTP/AVP;unicast;client_port=59782-59783
session: 438758
cseq: 5
connection: keep-alive
date: 2022-10-21T08:12:18.715264
cache-control: no-cache

08:12:18.715 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) (abcd1234:2:438758) Success to setup the udp stream. (rtpDestIp=127.0.0.1, rtpDestPort=59782, rtcpDestPort=59783)


9) Recv PLAY Request from VLC & Send Response to VLC

08:12:18.716 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () < PLAY
DefaultHttpRequest(decodeResult: success, version: RTSP/1.0)
PLAY rtsp://127.0.0.1:8554/abcd1234 RTSP/1.0
CSeq: 6
User-Agent: LibVLC/3.0.17.3 (LIVE555 Streaming Media v2016.11.28)
Session: 438758
Range: npt=0.000-
08:12:18.716 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (abcd1234:1:438758) AudioContextStreamer is selected. (sessionId=438758)
08:12:18.716 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (abcd1234:2:438758) VideoContextStreamer is selected. (sessionId=438758)
08:12:18.716 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () Call-ID: abcd1234
08:12:18.716 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) Current sessionId is [438758].
08:12:18.742 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:1:438758)| Success to connect the UDP Rtp endpoint. (targetNetworkInfo={
  "uri": "rtsp://127.0.0.1:8554/abcd1234",
  "destIp": "127.0.0.1",
  "rtpDestPort": 54650,
  "rtcpDestPort": 54651
})
08:12:18.759 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:1:438758)| Success to connect the UDP Rtcp endpoint. (targetNetworkInfo={
  "uri": "rtsp://127.0.0.1:8554/abcd1234",
  "destIp": "127.0.0.1",
  "rtpDestPort": 54650,
  "rtcpDestPort": 54651
})
08:12:18.765 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:2:438758)| Success to connect the UDP Rtp endpoint. (targetNetworkInfo={
  "uri": "rtsp://127.0.0.1:8554/abcd1234",
  "destIp": "127.0.0.1",
  "rtpDestPort": 59782,
  "rtcpDestPort": 59783
})
08:12:18.775 [MAIN_JobExecutor-0] DEBUG service.monitor.HaHandler - | cpu=[58.73], mem=[43.73/4096.00], thread=[41] | CallCount=[1]
08:12:18.775 [nioEventLoopGroup-2-1] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:2:438758)| Success to connect the UDP Rtcp endpoint. (targetNetworkInfo={
  "uri": "rtsp://127.0.0.1:8554/abcd1234",
  "destIp": "127.0.0.1",
  "rtpDestPort": 59782,
  "rtcpDestPort": 59783
})
08:12:18.856 [service-scheduler-2] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:2:438758)| [PLAY] > Success to send the response: DefaultFullHttpResponse(decodeResult: success, version: RTSP/1.0, content: UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf(ridx: 0, widx: 0, cap: 0))
RTSP/1.0 200 OK
server: 0
session: 438758
rtp-info: url=rtsp://127.0.0.1:8554/abcd1234/trackID=1;seq=32;rtptime=2631240,url=rtsp://127.0.0.1:8554/abcd1234/trackID=2;seq=202;rtptime=4934970


10) Set ICE Candidate

08:19:28.379 [service-scheduler-1] DEBUG media.core.stun.handler.IceHandler - |IceHandler(abcd1234)| Selected candidate=/192.168.5.224:30002 (local=/10.254.110.220:50006)


~~~
  
<img width="1562" alt="스크린샷 2022-10-21 오전 8 43 19" src="https://user-images.githubusercontent.com/37236920/197079474-24748bbf-aee9-436c-a686-2eb138652193.png">
  
~~~


11) Recv TEARDOWN from VCL & Send Response to VLC

08:19:52.605 [nioEventLoopGroup-2-2] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) () < TEARDOWN
DefaultHttpRequest(decodeResult: success, version: RTSP/1.0)
TEARDOWN rtsp://127.0.0.1:8554/abcd1234 RTSP/1.0
CSeq: 7
User-Agent: LibVLC/3.0.17.3 (LIVE555 Streaming Media v2016.11.28)
Session: 600797
08:19:52.607 [nioEventLoopGroup-2-2] INFO session.SessionManager - |SessionManager(d46c0324-9e18-431f-bc51-891b9d3e0131/abcd1234)| Call is deleted.
08:19:52.610 [nioEventLoopGroup-2-2] INFO umedia.core.scheduler.ServiceScheduler - Stopped scheduler!
08:19:52.619 [nioEventLoopGroup-2-2] DEBUG media.core.stream.manager.ChannelMaster - |ChannelMaster(abcd1234)| Stopping...
08:19:52.620 [nioEventLoopGroup-8-1] DEBUG media.core.rtsp.netty.handler.StreamerChannelHandler - (600797) Stop the streaming by [PortUnreachableException].
08:19:52.620 [nioEventLoopGroup-7-1] DEBUG media.core.rtsp.netty.handler.StreamerChannelHandler - (600797) Stop the streaming by [PortUnreachableException].
08:19:52.625 [nioEventLoopGroup-8-1] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:1:600797)| Streamer is finished.
08:19:52.625 [nioEventLoopGroup-8-1] DEBUG media.core.rtsp.netty.module.RtspNettyChannel - Streamer is deleted. (key=abcd1234:1:600797)
08:19:52.625 [nioEventLoopGroup-8-1] DEBUG media.core.rtsp.netty.handler.StreamerChannelHandler - (600797)  Finish to stream the media by [PortUnreachableException].
08:19:52.625 [nioEventLoopGroup-8-1] DEBUG media.core.rtsp.netty.handler.StreamerChannelHandler - (600797) Stop the streaming by [PortUnreachableException].
08:19:52.625 [nioEventLoopGroup-7-1] DEBUG media.core.rtsp.netty.handler.StreamerChannelHandler - (600797)  Finish to stream the media by [PortUnreachableException].
08:19:52.625 [nioEventLoopGroup-7-1] DEBUG media.core.rtsp.netty.handler.StreamerChannelHandler - (600797) Stop the streaming by [PortUnreachableException].
08:19:52.637 [nioEventLoopGroup-8-1] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:2:600797)| Streamer is finished.
08:19:52.638 [nioEventLoopGroup-8-1] DEBUG media.core.rtsp.netty.module.RtspNettyChannel - Streamer is deleted. (key=abcd1234:2:600797)
08:19:52.639 [nioEventLoopGroup-8-1] DEBUG media.core.rtsp.netty.handler.StreamerChannelHandler - (600797)  Finish to stream the media by [PortUnreachableException].
08:19:52.639 [nioEventLoopGroup-7-1] DEBUG media.core.rtsp.netty.handler.StreamerChannelHandler - (600797)  Finish to stream the media by [PortUnreachableException].
08:19:52.647 [nioEventLoopGroup-2-2] INFO umedia.core.scheduler.ServiceScheduler - Stopped scheduler!
08:19:52.647 [nioEventLoopGroup-2-2] DEBUG media.core.stream.manager.ChannelMaster - |ChannelMaster(abcd1234)| Stopped.
08:19:52.648 [nioEventLoopGroup-2-2] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) (abcd1234:1:600797) Stop the streaming.
08:19:52.648 [nioEventLoopGroup-2-2] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) (abcd1234:2:600797) Stop the streaming.
08:19:52.653 [nioEventLoopGroup-2-2] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) [TEARDOWN] > Success to send the response: DefaultFullHttpResponse(decodeResult: success, version: RTSP/1.0, content: UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf(ridx: 0, widx: 0, cap: 0))
RTSP/1.0 200 OK
session: 600797
cseq: 7
connection: keep-alive
date: 2022-10-21T08:19:52.653288
cache-control: no-cache

08:19:52.659 [ReadingThread] WARN media.core.webrtc.websocket.service.WebSocketService - |WebSocketService(abcd1234)| DISCONNECTED or CLOSED by closedByServer(false)
08:19:52.663 [nioEventLoopGroup-2-2] WARN media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) RtspChannelHandler.Exception (cause=java.io.IOException: Connection reset by peer)
08:19:52.663 [nioEventLoopGroup-2-2] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:1:600797)| Streamer is finished.
08:19:52.663 [nioEventLoopGroup-2-2] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (abcd1234:1:600797) AudioContextStreamer is removed.
08:19:52.664 [nioEventLoopGroup-2-2] DEBUG media.core.rtsp.Streamer - |Streamer(abcd1234:2:600797)| Streamer is finished.
08:19:52.664 [nioEventLoopGroup-2-2] DEBUG media.core.rtsp.netty.handler.RtspChannelHandler - (abcd1234:2:600797) VideoContextStreamer is removed.
08:19:52.664 [nioEventLoopGroup-2-2] WARN media.core.rtsp.netty.handler.RtspChannelHandler - (RTSP_127.0.0.1:8554) RtspChannelHandler is inactive.


[OME Log]

[2022-10-21 08:19:21.455] I [SPRtcSig-T9101:12418] WebRTC Publisher | webrtc_publisher.cpp:701  | Stop command received : #default#app/abcd1234/101
[2022-10-21 08:19:21.456] I [SPRtcSig-T9101:12418] Monitor | stream_metrics.cpp:138  | A session has been stopped playing #default#app/abcd1234 on the WebRTC publisher. Concurrent Viewers[WebRTC(0)/Stream total(0)/App total(0)]
[2022-10-21 08:19:21.456] I [SPRtcSig-T9101:12418] Signalling | rtc_signalling_server.cpp:369  | Client is disconnected: HttpConnection(0x7f6ef8001040) : WebSocket <ClientSocket: 0x7f7168000b80, #301, Connected, TCP, Nonblocking, 192.168.5.249:49708> TLS(Disabled) (#default#app / abcd1234, ufrag: local: lBkCdN, remote: stQP)

~~~
  
## REST-API
~~~
1. 세션(호) 개수 조회
  - Method : GET
  - URI : http://[IP]:[PORT]/s/v1/conference_count

2. CPU 사용량 조회
  - Method : GET
  - URI : http://[IP]:[PORT]/s/v1/cpu_usage

3. 힙 메모리 사용량 조회
  - Method : GET
  - URI : http://[IP]:[PORT]/s/v1/heap_memory_usage

4. 프로세스 내 구동 중인 전체 쓰레드 개수 조회
  - Method : GET
  - URI : http://[IP]:[PORT]/s/v1/total_thread_count

~~~
  
## 배포 방법
~~~
1. Intellij 를 켠다.
2. 터미널을 연다.
3. mvn package 를 입력한다.
4. 패키징 완료 후, mvn rpm:rpm 을 입력한다.
5. 원하는 서버로 옮긴다. (scp)
6. RPM 을 섶치힌다. (> sudo rpm -Uvh jrtsp_gw-0.0.1-1.noarch.rpm)
7. 설정 파일을 수정한다.
  - run.sh : jmx listen ip
  - application.yml : webrtc server uri
8. bin 폴더 안에 run.sh 를 조작한다.
  - 프로그램 시작 : ./run.sh start
  - 프로그램 재시작 : ./run.sh restart
  - 프로그램 종료 : ./run.sh stop
  - 프로그램 기동 상태 확인 : ./run.sh status
~~~
  
