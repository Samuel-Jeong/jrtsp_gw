package org.kkukie.jrtsp_gw;

import org.kkukie.jrtsp_gw.config.DefaultConfig;
import org.kkukie.jrtsp_gw.config.DtlsConfig;
import org.kkukie.jrtsp_gw.config.SdpConfig;
import org.kkukie.jrtsp_gw.config.StunConfig;
import org.kkukie.jrtsp_gw.service.ServiceManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
})
//@EnableJpaAuditing
@EnableConfigurationProperties({DefaultConfig.class, SdpConfig.class, DtlsConfig.class, StunConfig.class})
public class JrtspGwApplication {

	public static void main(String[] args) {
		SpringApplication.run(JrtspGwApplication.class, args);

		ServiceManager instance = ServiceManager.getInstance();
		instance.loop();
	}

}
