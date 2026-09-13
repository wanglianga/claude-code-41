package com.example.ridersafety.controller;

import com.example.ridersafety.model.Station;
import com.example.ridersafety.model.User;
import com.example.ridersafety.service.CurrentUser;
import com.example.ridersafety.service.RainPlanService;
import com.example.ridersafety.util.Payload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RainPlanController {

    private final RainPlanService rainPlanService;
    private final CurrentUser currentUser;

    // ---- 站长端：雨季计划 ----

    @PostMapping("/manager/rain-plans/generate")
    public Map<String, Object> generate(Authentication auth, @RequestBody(required = false) Map<String, Object> body) {
        User manager = currentUser.require(auth);
        Integer rainyDays = body != null ? Payload.integer(body, "rainyDays") : null;
        String forecast = body != null ? Payload.str(body, "forecast") : null;
        Integer shiftsPerWeek = body != null ? Payload.integer(body, "shiftsPerWeek") : null;
        return rainPlanService.generate(manager, rainyDays, forecast, shiftsPerWeek);
    }

    @GetMapping("/manager/rain-plans")
    public List<Map<String, Object>> list(Authentication auth,
                                          @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return rainPlanService.list(s.getId());
    }

    @GetMapping("/manager/rain-plans/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return rainPlanService.detail(id);
    }

    @PostMapping("/manager/rain-plans/{id}/confirm")
    public Map<String, Object> confirm(Authentication auth, @PathVariable Long id) {
        return rainPlanService.confirm(currentUser.require(auth), id);
    }

    @PostMapping("/manager/rain-plans/{id}/cancel")
    public Map<String, Object> cancel(Authentication auth, @PathVariable Long id) {
        return rainPlanService.cancel(currentUser.require(auth), id);
    }

    @PostMapping("/manager/rain-plans/{id}/issue/{itemId}")
    public Map<String, Object> issueItem(Authentication auth, @PathVariable Long id, @PathVariable Long itemId) {
        return rainPlanService.issueItem(currentUser.require(auth), id, itemId);
    }

    @PostMapping("/manager/rain-plans/{id}/issue-all")
    public Map<String, Object> issueAll(Authentication auth, @PathVariable Long id) {
        return rainPlanService.issueAll(currentUser.require(auth), id);
    }

    @PostMapping("/manager/rain-plans/{id}/remind")
    public Map<String, Object> remind(Authentication auth, @PathVariable Long id) {
        return rainPlanService.remind(currentUser.require(auth), id);
    }

    // ---- 骑手端：安全提醒 + 雨天接单提示 ----

    @GetMapping("/rider/reminders")
    public List<Map<String, Object>> myReminders(Authentication auth) {
        return rainPlanService.myReminders(currentUser.require(auth));
    }

    @PostMapping("/rider/reminders/{id}/read")
    public Map<String, Object> markRead(Authentication auth, @PathVariable Long id) {
        return rainPlanService.markReminderRead(currentUser.require(auth), id);
    }

    @GetMapping("/rider/rain-readiness")
    public Map<String, Object> rainReadiness(Authentication auth) {
        return rainPlanService.rainReadiness(currentUser.require(auth));
    }
}
