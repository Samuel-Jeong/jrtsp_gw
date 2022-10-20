package org.kkukie.jrtsp_gw.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "default")
@Getter
@Setter
public class DefaultConfig extends AbstractConfig {

    private String id = null;
    private String serverUri = null;
    private String applicationName = null;
    private int sendBufSize = 0;
    private int recvBufSize = 0;
    private int streamThreadPoolSize = 1;
    private String localListenIp = null;
    private int localRtspListenPort = 0;
    private int localPortMin = 0;
    private int localPortMax = 0;

}
