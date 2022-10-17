package org.kkukie.jrtsp_gw.service.scheduler.schedule.unit;

import org.kkukie.jrtsp_gw.service.scheduler.job.Job;
import org.kkukie.jrtsp_gw.service.scheduler.schedule.handler.JobScheduler;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class JobAdder implements Runnable {

    private final JobScheduler jobScheduler;
    private final Job job;
    private final int executorIndex;
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

    public JobAdder(JobScheduler jobScheduler, Job job, int executorIndex) {
        this.jobScheduler = jobScheduler;
        this.job = job;
        this.executorIndex = executorIndex;
    }

    @Override
    public void run() {
        if (job.isLasted()) {
            scheduledThreadPoolExecutor.scheduleAtFixedRate(
                    () -> {
                        if (isJobFinished(job)) {
                            jobScheduler.cancel(job);
                        } else {
                            jobScheduler.addJobToExecutor(executorIndex, job);
                        }
                    },
                    job.getInitialDelay(), job.getInterval(), job.getTimeUnit()
            );
        } else {
            jobScheduler.addJobToExecutor(executorIndex, job);
        }
    }

    public void stop() {
        scheduledThreadPoolExecutor.shutdown();
    }

    public boolean isJobFinished(Job job) {
        if (job == null) {
            return true;
        }

        return job.getIsFinished() ||
                (!job.isLasted() && (job.decCurRemainRunCount() < 0));
    }

}
