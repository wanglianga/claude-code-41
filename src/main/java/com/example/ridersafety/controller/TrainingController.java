package com.example.ridersafety.controller;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.*;
import com.example.ridersafety.repository.TrainingRecordRepository;
import com.example.ridersafety.repository.UserRepository;
import com.example.ridersafety.service.CurrentUser;
import com.example.ridersafety.service.DtoMapper;
import com.example.ridersafety.util.Payload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingRecordRepository trainingRepo;
    private final UserRepository userRepo;
    private final CurrentUser currentUser;

    @GetMapping("/rider/trainings")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myTrainings(Authentication auth) {
        User rider = currentUser.require(auth);
        return trainingRepo.findByRider_IdOrderByCompletedAtDesc(rider.getId()).stream()
                .map(DtoMapper::training).toList();
    }

    @GetMapping("/manager/trainings")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> forStation(Authentication auth,
                                                @RequestParam(required = false) Long stationId) {
        User u = currentUser.require(auth);
        Station s = currentUser.resolveStation(u, stationId);
        return trainingRepo.findByRider_Station_IdOrderByCompletedAtDesc(s.getId()).stream()
                .map(DtoMapper::training).toList();
    }

    @PostMapping("/manager/trainings")
    @Transactional
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        User rider = userRepo.findById(Payload.lng(body, "riderId"))
                .orElseThrow(() -> ApiException.notFound("骑手不存在"));
        TrainingRecord t = new TrainingRecord(rider,
                Payload.require(body, "title", "培训主题"),
                TrainingCategory.valueOf(Payload.require(body, "category", "培训类别")),
                LocalDateTime.now(),
                Payload.integer(body, "score"));
        return DtoMapper.training(trainingRepo.save(t));
    }
}
