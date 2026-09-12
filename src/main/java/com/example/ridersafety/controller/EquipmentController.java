package com.example.ridersafety.controller;

import com.example.ridersafety.model.Station;
import com.example.ridersafety.model.User;
import com.example.ridersafety.repository.StationRepository;
import com.example.ridersafety.service.CurrentUser;
import com.example.ridersafety.service.DtoMapper;
import com.example.ridersafety.service.EquipmentService;
import com.example.ridersafety.util.Payload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final CurrentUser currentUser;
    private final StationRepository stationRepository;

    @GetMapping("/stations")
    public List<Map<String, Object>> stations() {
        return stationRepository.findAll().stream().map(DtoMapper::station).toList();
    }

    @GetMapping("/equipment/types")
    public List<Map<String, Object>> types() {
        return equipmentService.types();
    }

    // ---- 骑手端 ----

    @GetMapping("/rider/equipment")
    public List<Map<String, Object>> myEquipment(Authentication auth) {
        return equipmentService.myIssues(currentUser.require(auth));
    }

    // ---- 站长端 ----

    @GetMapping("/manager/stock")
    public List<Map<String, Object>> stock(Authentication auth,
                                           @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return equipmentService.stockFor(s.getId());
    }

    @PostMapping("/manager/stock/{id}/adjust")
    public Map<String, Object> adjustStock(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int delta = Payload.integer(body, "delta") != null ? Payload.integer(body, "delta") : 0;
        return equipmentService.adjustStock(id, delta);
    }

    @PostMapping("/manager/issue")
    public Map<String, Object> issue(Authentication auth, @RequestBody Map<String, Object> body,
                                     @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return equipmentService.issue(s,
                Payload.lng(body, "riderId"),
                Payload.lng(body, "equipmentTypeId"),
                Payload.str(body, "size"),
                Payload.str(body, "notes"));
    }

    @GetMapping("/manager/issues")
    public List<Map<String, Object>> issues(Authentication auth,
                                            @RequestParam(required = false) Long stationId,
                                            @RequestParam(defaultValue = "false") boolean expiredOnly) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return equipmentService.issuesForStation(s.getId(), expiredOnly);
    }

    @GetMapping("/manager/riders")
    public List<Map<String, Object>> riders(Authentication auth,
                                            @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return equipmentService.ridersOfStation(s.getId());
    }
}
