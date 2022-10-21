package org.kkukie.jrtsp_gw.util;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class StringManager {

    public static String getRandomString(int length) {
        return randomAlphanumeric(length);
    }

}
