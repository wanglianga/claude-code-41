package com.example.ridersafety.controller;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.AccidentType;
import com.example.ridersafety.model.Station;
import com.example.ridersafety.model.User;
import com.example.ridersafety.model.Weather;
import com.example.ridersafety.service.AccidentService;
import com.example.ridersafety.service.CurrentUser;
import com.example.ridersafety.util.Payload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccidentController {

    private final AccidentService accidentService;
    private final CurrentUser currentUser;

    // ---- 骑手端 ----

    @PostMapping("/rider/accidents")
    public Map<String, Object> report(Authentication auth, @RequestBody Map<String, Object> body) {
        User rider = currentUser.require(auth);
        String w = Payload.str(body, "weather");
        return accidentService.report(rider,
                AccidentType.valueOf(Payload.require(body, "type", "事故类型")),
                Payload.dateTime(body, "occurredAt"),
                Payload.require(body, "location", "事故地点"),
                Payload.str(body, "routeArea"),
                Payload.str(body, "orderNo"),
                Payload.str(body, "equipmentStatusDesc"),
                Payload.lng(body, "damagedEquipmentTypeId"),
                Payload.str(body, "injuryDesc"),
                Payload.str(body, "policeRecordNo"),
                Payload.str(body, "photoUrls"),
                w != null ? Weather.valueOf(w) : null);
    }

    @GetMapping("/rider/accidents")
    public List<Map<String, Object>> myAccidents(Authentication auth) {
        return accidentService.myAccidents(currentUser.require(auth));
    }

    @GetMapping("/rider/accidents/{id}")
    public Map<String, Object> myAccidentDetail(Authentication auth, @PathVariable Long id) {
        User rider = currentUser.require(auth);
        Map<String, Object> detail = accidentService.detail(id);
        Number riderId = (Number) detail.get("riderId");
        if (riderId == null || !rider.getId().equals(riderId.longValue())) {
            throw ApiException.forbidden("只能查看自己的事故记录");
        }
        return detail;
    }

    // ---- 站长端 ----

    @GetMapping("/manager/accidents")
    public List<Map<String, Object>> forStation(Authentication auth,
                                                @RequestParam(required = false) Long stationId,
                                                @RequestParam(required = false) String status) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return accidentService.accidentsForStation(s.getId(), status);
    }

    @GetMapping("/manager/accidents/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return accidentService.detail(id);
    }

    @PostMapping("/manager/accidents/{id}/review")
    public Map<String, Object> review(Authentication auth, @PathVariable Long id,
                                      @RequestBody Map<String, Object> body) {
        User manager = currentUser.require(auth);
        return accidentService.review(manager, id,
                Payload.bool(body, "wasDelivering"),
                Payload.bool(body, "wearingEquipment"),
                Payload.bool(body, "hasViolation"),
                Payload.str(body, "violationDesc"),
                Payload.bool(body, "insuranceNeeded"),
                Payload.bool(body, "materialsComplete"),
                Payload.str(body, "reviewNotes"),
                Payload.require(body, "result", "核查结论"));
    }

    @GetMapping("/manager/claims")
    public List<Map<String, Object>> claims(Authentication auth,
                                            @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return accidentService.claimsForStation(s.getId());
    }

    @PutMapping("/manager/claims/{id}")
    public Map<String, Object> updateClaim(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return accidentService.updateClaim(id,
                Payload.str(body, "status"),
                Payload.dec(body, "amount"),
                Payload.str(body, "materialsNotes"));
    }

    @GetMapping("/manager/reissues")
    public List<Map<String, Object>> reissues(Authentication auth,
                                              @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return accidentService.reissuesForStation(s.getId());
    }

    @PostMapping("/manager/reissues/{id}/process")
    public Map<String, Object> processReissue(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return accidentService.processReissue(id, Payload.require(body, "action", "处理动作"));
    }
}
