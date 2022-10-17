package org.kkukie.jrtsp_gw.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dtls")
@Getter
@Setter
public class DtlsConfig extends AbstractConfig {

    private String keyPath;
    private String certPath;

}
