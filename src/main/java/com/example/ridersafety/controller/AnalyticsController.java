package com.example.ridersafety.controller;

import com.example.ridersafety.model.Station;
import com.example.ridersafety.model.User;
import com.example.ridersafety.service.AnalyticsService;
import com.example.ridersafety.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUser currentUser;

    /** 站长看自己站点；管理员不传 stationId 时看全部 */
    private Long scope(Authentication auth, Long stationId) {
        User u = currentUser.require(auth);
        if (u.getStation() != null) {
            return u.getStation().getId();
        }
        return stationId;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication auth, @RequestParam(required = false) Long stationId) {
        return analyticsService.dashboard(scope(auth, stationId));
    }

    @GetMapping("/accidents/by-rider")
    public List<Map<String, Object>> byRider(Authentication auth, @RequestParam(required = false) Long stationId) {
        return analyticsService.accidentsByRider(scope(auth, stationId));
    }

    @GetMapping("/accidents/by-route")
    public List<Map<String, Object>> byRoute(Authentication auth, @RequestParam(required = false) Long stationId) {
        return analyticsService.accidentsByRoute(scope(auth, stationId));
    }

    @GetMapping("/accidents/by-weather")
    public List<Map<String, Object>> byWeather(Authentication auth, @RequestParam(required = false) Long stationId) {
        return analyticsService.accidentsByWeather(scope(auth, stationId));
    }

    @GetMapping("/equipment/consumption")
    public List<Map<String, Object>> consumption(Authentication auth, @RequestParam(required = false) Long stationId) {
        return analyticsService.equipmentConsumption(scope(auth, stationId));
    }

    @GetMapping("/alerts")
    public List<Map<String, Object>> alerts(Authentication auth, @RequestParam(required = false) Long stationId) {
        return analyticsService.alerts(scope(auth, stationId));
    }

    @GetMapping("/retrospective")
    public Map<String, Object> retrospective(Authentication auth, @RequestParam(required = false) Long stationId) {
        return analyticsService.retrospective(scope(auth, stationId));
    }
}
