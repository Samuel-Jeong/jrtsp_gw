package org.kkukie.jrtsp_gw.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stun")
@Getter
@Setter
public class StunConfig extends AbstractConfig {

    private int harvestIntervalMs;

}
