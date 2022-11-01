package org.kkukie.jrtsp_gw.media.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @class public class WebSocketPortManager
 * @brief WebSocketPortManager class
 */
public class WebSocketPortManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketPortManager.class);

    private static WebSocketPortManager webSocketPortManager = null;
    private final ConcurrentLinkedQueue<Integer> channelQueues;
    private final int portGap = 2;
    private int localPortMin = 0;
    private int localPortMax = 0;

    ////////////////////////////////////////////////////////////////////////////////

    public WebSocketPortManager() {
        channelQueues = new ConcurrentLinkedQueue<>();
    }

    public static WebSocketPortManager getInstance() {
        if (webSocketPortManager == null) {
            webSocketPortManager = new WebSocketPortManager();
        }

        return webSocketPortManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void initResource(int localPortMin, int localPortMax) {
        this.localPortMin = localPortMin;
        this.localPortMax = localPortMax;

        for (int idx = localPortMin; idx <= localPortMax; idx += portGap) {
            try {
                channelQueues.add(idx);
            } catch (Exception e) {
                logger.error("|WebSocketPortManager| Exception from port resource in Queue", e);
                return;
            }
        }

        logger.info("|WebSocketPortManager| Ready to use the port resources in Queue. (port range: {} - {}, gap={})",
                localPortMin, localPortMax, portGap
        );
    }

    public void releaseResource() {
        channelQueues.clear();
        logger.info("|WebSocketPortManager| Release all port resources in Queue. (port range: {} - {}, gap={})",
                localPortMin, localPortMax, portGap
        );
    }

    public int takePort() {
        if (channelQueues.isEmpty()) {
            logger.warn("|WebSocketPortManager| Port resource Queue is empty.");
            return -1;
        }

        int port = -1;
        try {
            Integer value = channelQueues.poll();
            if (value != null) {
                port = value;
            }
        } catch (Exception e) {
            logger.warn("|WebSocketPortManager| Exception to get port resource in Queue.", e);
        }

        logger.debug("|WebSocketPortManager| Success to get port(={}) resource from Queue.", port);
        return port;
    }

    public void restorePort(int port) {
        if (!channelQueues.contains(port)) {
            try {
                channelQueues.offer(port);
            } catch (Exception e) {
                logger.warn("|WebSocketPortManager| Exception to restore port(={}) resource in Queue.", port, e);
            }
        }
    }

    public void removePort(int port) {
        try {
            channelQueues.remove(port);
        } catch (Exception e) {
            logger.warn("|WebSocketPortManager| Exception to remove to port(={}) resource in Queue.", port, e);
        }
    }

}
