package com.pharmatrace.controller;

import com.pharmatrace.config.PharmaTraceProperties;
import com.pharmatrace.model.ProductBatch;
import com.pharmatrace.model.TelemetryLog;
import com.pharmatrace.service.BatchCustodyService;
import com.pharmatrace.service.BlockchainNotarizationService;
import com.pharmatrace.service.TelemetryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller serving the Thymeleaf Corporate Light Dashboard.
 */
@Controller
public class DashboardViewController {

    private final TelemetryService telemetryService;
    private final BatchCustodyService custodyService;
    private final BlockchainNotarizationService blockchainService;
    private final PharmaTraceProperties properties;

    public DashboardViewController(TelemetryService telemetryService,
                                   BatchCustodyService custodyService,
                                   BlockchainNotarizationService blockchainService,
                                   PharmaTraceProperties properties) {
        this.telemetryService = telemetryService;
        this.custodyService = custodyService;
        this.blockchainService = blockchainService;
        this.properties = properties;
    }

    @GetMapping("/")
    public String index(Model model) {
        ProductBatch activeBatch = custodyService.getActiveBatch(properties.getDefaultBatchId());
        TelemetryLog latestLog = telemetryService.getLatestTelemetry().orElse(null);
        List<TelemetryLog> recentLogs = telemetryService.getRecentTelemetryLogs();

        model.addAttribute("batch", activeBatch);
        model.addAttribute("latestLog", latestLog);
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("hubs", BatchCustodyService.HUBS);
        model.addAttribute("authorizedUids", properties.getRfid().getAuthorizedUids());
        model.addAttribute("primaryRfid", "2AF52B07");
        model.addAttribute("tag1Rfid", "2AF52B07");
        model.addAttribute("tag2Rfid", "659A1707");
        model.addAttribute("evmConnected", blockchainService.isEvmConnected());
        model.addAttribute("evmVersion", blockchainService.getEvmClientVersion());
        model.addAttribute("tempMin", properties.getTemperature().getMin());
        model.addAttribute("tempMax", properties.getTemperature().getMax());
        model.addAttribute("totalLogs", telemetryService.getTotalLogsCount());
        model.addAttribute("totalBreaches", telemetryService.getBreachCount());

        return "index";
    }
}
