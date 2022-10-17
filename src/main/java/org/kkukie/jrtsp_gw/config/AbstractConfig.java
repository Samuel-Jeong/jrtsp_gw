package org.kkukie.jrtsp_gw.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class AbstractConfig {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String toString() {
        return gson.toJson(this);
    }

}
