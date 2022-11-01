package org.kkukie.jrtsp_gw.media.core.stream.rtsp.statistics;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Slf4j
public class RtpRxStatistics {

    private final String id;

    private final ScheduledExecutorService scheduledExecutorService;

    private AtomicLong totalBytes = new AtomicLong(0);

    private AtomicLong bitrate = new AtomicLong(0);

    public RtpRxStatistics() {
        id = UUID.randomUUID().toString().substring(0, 10);
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
                runnable -> new Thread(
                        runnable,
                        id + ":" + runnable.getClass().getSimpleName()
                )
        );
    }

    public void calculate(long kb) {
        if (kb <= 0) {
            return;
        }

        totalBytes.addAndGet(kb);
    }

    public void start() {
        StatisticsPrinter statisticsPrinter = new StatisticsPrinter();
        scheduledExecutorService.scheduleAtFixedRate(
                statisticsPrinter,
                0, 1000, TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        scheduledExecutorService.shutdown();
    }

    private class StatisticsPrinter implements Runnable {

        @Override
        public void run() {
            long kb = totalBytes.get() * 8;
            kb /= 1024;
            bitrate.set(kb);
            log.debug("|RtpRxStatistics.StatisticsPrinter| [{}] Bitrate = < {} > (kbps)", id, bitrate.get());
            totalBytes.set(0);
        }

    }

}
