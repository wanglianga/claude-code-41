package com.example.ridersafety.controller;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.Station;
import com.example.ridersafety.model.User;
import com.example.ridersafety.model.Weather;
import com.example.ridersafety.service.CurrentUser;
import com.example.ridersafety.service.ReplacementService;
import com.example.ridersafety.util.Payload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReplacementController {

    private final ReplacementService replacementService;
    private final CurrentUser currentUser;

    // ---- 骑手端 ----

    @PostMapping("/rider/replacements")
    public Map<String, Object> create(Authentication auth, @RequestBody Map<String, Object> body) {
        User rider = currentUser.require(auth);
        Weather weather = null;
        String w = Payload.str(body, "weather");
        if (w != null) {
            weather = Weather.valueOf(w);
        }
        return replacementService.create(rider,
                Payload.lng(body, "issueId"),
                Payload.require(body, "reason", "更换原因"),
                weather,
                Payload.str(body, "wearPhotos"),
                Payload.idList(body, "photoIds"));
    }

    @GetMapping("/rider/replacements")
    public List<Map<String, Object>> myRequests(Authentication auth) {
        return replacementService.myRequests(currentUser.require(auth));
    }

    // ---- 站长端 ----

    @GetMapping("/manager/replacements")
    public List<Map<String, Object>> forStation(Authentication auth,
                                                @RequestParam(required = false) Long stationId,
                                                @RequestParam(required = false) String status) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return replacementService.requestsForStation(s.getId(), status);
    }

    @PostMapping("/manager/replacements/{id}/process")
    public Map<String, Object> process(Authentication auth, @PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {
        User manager = currentUser.require(auth);
        String action = Payload.require(body, "action", "处理动作");
        if (!List.of("APPROVE_FREE", "APPROVE_DEPOSIT", "REPAIR", "REJECT").contains(action)) {
            throw ApiException.badRequest("action 必须是 APPROVE_FREE / APPROVE_DEPOSIT / REPAIR / REJECT");
        }
        return replacementService.process(manager, id, action,
                Payload.dec(body, "depositDeducted"), Payload.str(body, "note"));
    }
}
