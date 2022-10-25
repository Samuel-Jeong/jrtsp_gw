package org.kkukie.jrtsp_gw.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "session")
@Getter
@Setter
public class SessionConfig {

    private int maxSessionCount;

    @Override
    public String toString() {
        return "SessionConfig{" +
                "maxSessionCount=" + maxSessionCount +
                '}';
    }

}
