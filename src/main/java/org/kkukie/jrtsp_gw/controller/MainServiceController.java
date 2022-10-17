package org.kkukie.jrtsp_gw.controller;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.service.ServiceManager;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class MainServiceController {

    ServiceManager serviceManager;

    public MainServiceController(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceManager.loop();
    }

}
