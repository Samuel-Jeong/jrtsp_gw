package org.kkukie.jrtsp_gw.controller;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.service.system.SystemManager;
import org.kkukie.jrtsp_gw.session.call.ConferenceMaster;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/s")
public class MainServiceController {

    public MainServiceController() {}

    @GetMapping("/v1/conference_count")
    public String getCallCountV1() {
        return String.valueOf(ConferenceMaster.getInstance().getConferenceInfoSize());
    }

    @GetMapping("/v1/cpu_usage")
    public String getCpuUsageV1() {
        return String.valueOf(SystemManager.getInstance().getCpuUsage());
    }

    @GetMapping("/v1/heap_memory_usage")
    public String getHeapMemoryUsageV1() {
        return String.valueOf(SystemManager.getInstance().getHeapMemoryUsage());
    }

    @GetMapping("/v1/total_thread_count")
    public String getTotalThreadCountV1() {
        return String.valueOf(Thread.activeCount());
    }

}
