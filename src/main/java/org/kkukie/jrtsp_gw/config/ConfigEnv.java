package org.kkukie.jrtsp_gw.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class ConfigEnv {

    private final Environment environment;

    public String getStringProperty(String key){
        return environment.getProperty(key);
    }

    public String[] getStringArrayProperty(String key) {
        String property = environment.getProperty(key);
        String[] split = Objects.requireNonNull(property).split(",");
        String[] newSplit = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            newSplit[i] = split[i].trim();
        }
        return newSplit;
    }

    public int getIntProperty(String key) {
        return Integer.parseInt(Objects.requireNonNull(environment.getProperty(key)));
    }

}
