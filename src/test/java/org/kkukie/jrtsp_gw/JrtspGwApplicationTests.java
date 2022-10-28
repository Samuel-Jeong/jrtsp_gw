package org.kkukie.jrtsp_gw;

import org.junit.jupiter.api.Test;
import org.kkukie.jrtsp_gw.config.DefaultConfig;
import org.kkukie.jrtsp_gw.config.DtlsConfig;
import org.kkukie.jrtsp_gw.config.SdpConfig;
import org.kkukie.jrtsp_gw.config.StunConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@EnableConfigurationProperties({DefaultConfig.class, SdpConfig.class, DtlsConfig.class, StunConfig.class})
class JrtspGwApplicationTests {

	@Test
	void contextLoads() {
	}

}
