package com.example.ridersafety.controller;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.*;
import com.example.ridersafety.repository.*;
import com.example.ridersafety.service.CurrentUser;
import com.example.ridersafety.service.DtoMapper;
import com.example.ridersafety.util.Payload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssessmentController {

    private final RiderAssessmentRepository assessmentRepo;
    private final UserRepository userRepo;
    private final CurrentUser currentUser;

    @GetMapping("/rider/assessments")
    @Transactional(readOnly = true)
    public Map<String, Object> myAssessments(Authentication auth) {
        User rider = currentUser.require(auth);
        List<RiderAssessment> list = assessmentRepo.findByRider_IdOrderByCreatedAtDesc(rider.getId());
        int score = 100 + list.stream().mapToInt(RiderAssessment::getPointsChange).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", score);
        m.put("records", list.stream().map(DtoMapper::assessment).toList());
        return m;
    }

    @GetMapping("/manager/assessments")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> forStation(Authentication auth,
                                                @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return assessmentRepo.findByRider_Station_IdOrderByCreatedAtDesc(s.getId()).stream()
                .map(DtoMapper::assessment).toList();
    }

    @PostMapping("/manager/assessments")
    @Transactional
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        User rider = userRepo.findById(Payload.lng(body, "riderId"))
                .orElseThrow(() -> ApiException.notFound("骑手不存在"));
        Integer points = Payload.integer(body, "pointsChange");
        if (points == null || points == 0) {
            throw ApiException.badRequest("请填写非零的加减分值");
        }
        RiderAssessment a = new RiderAssessment(rider, null, points,
                Payload.require(body, "reason", "考核原因"));
        return DtoMapper.assessment(assessmentRepo.save(a));
    }
}
