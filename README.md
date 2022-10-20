# jrtsp_gw
- RTSP Gateway with WAS
- We can get the stream from OvenMediaEngine
  
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
3. Request the stream to [jrtsp_gw] by [RTSP Client(vlc)].
~~~
  
## REST-API
~~~
1. 세션(호) 개수 조회
  - Method : GET
  - URI : http://[IP]:[PORT]/s/v1/call_count

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
  
