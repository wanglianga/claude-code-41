package com.example.ridersafety.controller;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.PolicyAdjustment;
import com.example.ridersafety.model.PolicyType;
import com.example.ridersafety.model.Station;
import com.example.ridersafety.model.User;
import com.example.ridersafety.repository.PolicyAdjustmentRepository;
import com.example.ridersafety.service.CurrentUser;
import com.example.ridersafety.service.DtoMapper;
import com.example.ridersafety.util.Payload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyAdjustmentRepository policyRepo;
    private final CurrentUser currentUser;

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Authentication auth,
                                          @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return policyRepo.findByStation_IdOrderByCreatedAtDesc(s.getId()).stream()
                .map(DtoMapper::policy).toList();
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(Authentication auth, @RequestBody Map<String, Object> body,
                                      @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        PolicyAdjustment p = new PolicyAdjustment(s,
                PolicyType.valueOf(Payload.require(body, "type", "规则类型")),
                Payload.require(body, "title", "标题"),
                Payload.require(body, "content", "内容"),
                u.getName());
        return DtoMapper.policy(policyRepo.save(p));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        if (!policyRepo.existsById(id)) {
            throw ApiException.notFound("规则不存在");
        }
        policyRepo.deleteById(id);
        return Map.of("ok", true);
    }
}
