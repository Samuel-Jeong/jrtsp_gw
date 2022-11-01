package org.kkukie.jrtsp_gw.service.monitor;


import org.kkukie.jrtsp_gw.service.scheduler.job.Job;
import org.kkukie.jrtsp_gw.service.scheduler.job.JobContainer;
import org.kkukie.jrtsp_gw.service.system.SystemManager;
import org.kkukie.jrtsp_gw.session.call.ConferenceMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaHandler extends JobContainer {

    private static final Logger logger = LoggerFactory.getLogger(HaHandler.class);

    ////////////////////////////////////////////////////////////////////////////////

    public HaHandler(Job haHandleJob) {
        setJob(haHandleJob);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void init() {
        getJob().setRunnable(() -> {
            SystemManager systemManager = SystemManager.getInstance();

            String cpuUsageStr = systemManager.getCpuUsage();
            String memoryUsageStr = systemManager.getHeapMemoryUsage();

            logger.debug("| cpu=[{}], mem=[{}], thread=[{}] | Conference=[{}]",
                    cpuUsageStr, memoryUsageStr, Thread.activeCount(), ConferenceMaster.getInstance().getConferenceInfoSize()
            );
        });
    }

}
