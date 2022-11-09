package org.kkukie.jrtsp_gw.service;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.config.ConfigManager;
import org.kkukie.jrtsp_gw.config.DefaultConfig;
import org.kkukie.jrtsp_gw.media.core.stream.rtsp.netty.NettyChannelManager;
import org.kkukie.jrtsp_gw.media.core.util.WebSocketPortManager;
import org.kkukie.jrtsp_gw.service.monitor.HaHandler;
import org.kkukie.jrtsp_gw.service.scheduler.job.Job;
import org.kkukie.jrtsp_gw.service.scheduler.job.JobBuilder;
import org.kkukie.jrtsp_gw.service.scheduler.schedule.ScheduleManager;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.*;

@Service
@Slf4j
public class ServiceManager {

    public static final String MAIN_SCHEDULE_JOB = "MAIN";
    public static final String LONG_SESSION_REMOVE_SCHEDULE_JOB = "LONG_SESSION_REMOVE_JOB";

    private static final int DELAY = 1000;
    private static final int MAIN_SCHEDULE_THREAD_SIZE = 5;

    private static final ServiceManager serviceManager = new ServiceManager(); // lazy initialization
    private final ScheduleManager scheduleManager = new ScheduleManager();

    private final String tmpdir = System.getProperty("java.io.tmpdir");
    private final File lockFile = new File(tmpdir, System.getProperty("lock_file", "jrtsp_gw.lock"));
    private FileChannel fileChannel;
    private FileLock lock;

    private boolean isQuit = false;

    private final DefaultConfig defaultConfig;

    private ServiceManager() {
        Runtime.getRuntime().addShutdownHook(new ShutDownHookHandler("ShutDownHookHandler", Thread.currentThread()));

        defaultConfig = ConfigManager.getDefaultConfig();
    }

    public static ServiceManager getInstance() {
        return serviceManager;
    }

    private boolean start() {
        systemLock();

        WebSocketPortManager.getInstance().initResource(defaultConfig.getLocalPortMin(), defaultConfig.getLocalPortMax());

        NettyChannelManager.getInstance().openRtspChannel(
                defaultConfig.getLocalListenIp(),
                defaultConfig.getLocalRtspListenPort()
        );

        if (scheduleManager.initJob(MAIN_SCHEDULE_JOB, MAIN_SCHEDULE_THREAD_SIZE, MAIN_SCHEDULE_THREAD_SIZE * 2)) {
            // FOR CHECKING the availability of this program
            Job haHandleJob = new JobBuilder()
                    .setScheduleManager(scheduleManager)
                    .setName(HaHandler.class.getSimpleName())
                    .setInitialDelay(0)
                    .setInterval(DELAY)
                    .setTimeUnit(TimeUnit.MILLISECONDS)
                    .setPriority(5)
                    .setTotalRunCount(1)
                    .setIsLasted(true)
                    .build();
            HaHandler haHandler = new HaHandler(haHandleJob);
            haHandler.init();
            if (scheduleManager.startJob(MAIN_SCHEDULE_JOB, haHandler.getJob())) {
                log.debug("|ServiceManager| [+RUN] HA Handler");
            } else {
                log.warn("|ServiceManager| [-RUN FAIL] HA Handler");
                return false;
            }
        }

        log.debug("|ServiceManager| All services are opened.");
        return true;
    }

    public void stop() {
        WebSocketPortManager.getInstance().releaseResource();

        NettyChannelManager.getInstance().deleteRtspChannel();

        scheduleManager.stopAll(MAIN_SCHEDULE_JOB);

        systemUnLock();

        isQuit = true;
        log.debug("|ServiceManager| All services are closed.");
    }

    public void loop() {
        if (!start()) {
            log.error("|ServiceManager| Fail to start the program.");
            return;
        }

        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        while (!isQuit) {
            try {
                timeUnit.sleep(DELAY);
            } catch (InterruptedException e) {
                log.warn("|ServiceManager| loop.InterruptedException", e);
            }
        }
    }

    private void systemLock() {
        try {
            fileChannel = FileChannel.open(lockFile.toPath(), CREATE, READ, WRITE);
            lock = fileChannel.tryLock();
            if (lock == null) {
                log.error("|ServiceManager|Process is already running.");
                Thread.sleep(500L);
                System.exit(1);
            }
        } catch (Exception e) {
            log.error("|ServiceManager| systemLock.Exception.", e);
        }
    }

    private void systemUnLock() {
        try {
            if (lock != null) {
                lock.release();
            }

            if (fileChannel != null) {
                fileChannel.close();
            }

            Files.delete(lockFile.toPath());
        } catch (IOException e) {
            //ignore
        }
    }

    /**
     * @class private static class ShutDownHookHandler extends Thread
     * @brief Graceful Shutdown 을 처리하는 클래스
     * Runtime.getRuntime().addShutdownHook(*) 에서 사용됨
     */
    private static class ShutDownHookHandler extends Thread {

        // shutdown 로직 후에 join 할 thread
        private final Thread target;

        public ShutDownHookHandler(String name, Thread target) {
            super(name);

            this.target = target;
            log.debug("|ServiceManager| ShutDownHookHandler is initiated. (target={})", target.getName());
        }

        /**
         * @fn public void run ()
         * @brief 정의된 Shutdown 로직을 수행하는 함수
         */
        @Override
        public void run() {
            try {
                shutDown();
                target.join();
                log.debug("|ServiceManager| ShutDownHookHandler's target is finished successfully. (target={})", target.getName());
            } catch (Exception e) {
                log.warn("|ServiceManager| ShutDownHookHandler.run.Exception", e);
            }
        }

        /**
         * @fn private void shutDown ()
         * @brief Runtime 에서 선언된 Handler 에서 사용할 서비스 중지 함수
         */
        private void shutDown() {
            log.warn("|ServiceManager| Process is about to quit. (Ctrl+C)");
            ServiceManager.getInstance().stop();
        }
    }

}
