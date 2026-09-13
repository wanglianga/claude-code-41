package com.example.ridersafety.service;

import com.example.ridersafety.model.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 实体 -> 前端 JSON 视图（必须在事务内调用，避免懒加载问题） */
public class DtoMapper {

    public static Map<String, Object> station(Station s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("city", s.getCity());
        m.put("address", s.getAddress());
        return m;
    }

    public static Map<String, Object> user(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("name", u.getName());
        m.put("role", u.getRole().name());
        m.put("phone", u.getPhone());
        m.put("stationId", u.getStation() != null ? u.getStation().getId() : null);
        m.put("stationName", u.getStation() != null ? u.getStation().getName() : null);
        return m;
    }

    public static Map<String, Object> type(EquipmentType t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("code", t.getCode());
        m.put("name", t.getName());
        m.put("deposit", t.getDeposit());
        m.put("replacementCycleMonths", t.getReplacementCycleMonths());
        m.put("sizes", t.getSizes() != null ? t.getSizes().split(",") : new String[0]);
        m.put("description", t.getDescription());
        return m;
    }

    public static Map<String, Object> stock(EquipmentStock s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("stationId", s.getStation().getId());
        m.put("typeId", s.getEquipmentType().getId());
        m.put("typeName", s.getEquipmentType().getName());
        m.put("typeCode", s.getEquipmentType().getCode());
        m.put("size", s.getSize());
        m.put("quantity", s.getQuantity());
        return m;
    }

    public static Map<String, Object> issue(EquipmentIssue i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("riderId", i.getRider().getId());
        m.put("riderName", i.getRider().getName());
        m.put("stationName", i.getStation().getName());
        m.put("typeId", i.getEquipmentType().getId());
        m.put("typeName", i.getEquipmentType().getName());
        m.put("typeCode", i.getEquipmentType().getCode());
        m.put("size", i.getSize());
        m.put("issuedAt", i.getIssuedAt());
        m.put("depositPaid", i.getDepositPaid());
        m.put("status", i.getStatus().name());
        m.put("expectedReplaceAt", i.getExpectedReplaceAt());
        m.put("returnedAt", i.getReturnedAt());
        m.put("notes", i.getNotes());
        boolean expired = i.getStatus() == IssueStatus.IN_USE && i.getExpectedReplaceAt() != null
                && i.getExpectedReplaceAt().isBefore(java.time.LocalDate.now());
        m.put("expired", expired);
        return m;
    }

    public static Map<String, Object> replacement(ReplacementRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("riderId", r.getRider().getId());
        m.put("riderName", r.getRider().getName());
        EquipmentIssue i = r.getIssue();
        m.put("issueId", i.getId());
        m.put("typeName", i.getEquipmentType().getName());
        m.put("typeCode", i.getEquipmentType().getCode());
        m.put("size", i.getSize());
        m.put("issuedAt", i.getIssuedAt());
        m.put("expectedReplaceAt", i.getExpectedReplaceAt());
        m.put("reason", r.getReason());
        m.put("wearPhotos", r.getWearPhotos());
        m.put("photoIds", r.getPhotoIds());
        m.put("weather", r.getWeather() != null ? r.getWeather().name() : null);
        m.put("status", r.getStatus().name());
        m.put("evaluation", r.getEvaluation());
        m.put("suggestedDecision", r.getSuggestedDecision() != null ? r.getSuggestedDecision().name() : null);
        m.put("depositDeducted", r.getDepositDeducted());
        m.put("processedBy", r.getProcessedBy() != null ? r.getProcessedBy().getName() : null);
        m.put("processedAt", r.getProcessedAt());
        m.put("processNote", r.getProcessNote());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    public static Map<String, Object> accident(AccidentReport a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("riderId", a.getRider().getId());
        m.put("riderName", a.getRider().getName());
        m.put("stationName", a.getRider().getStation() != null ? a.getRider().getStation().getName() : null);
        m.put("type", a.getType().name());
        m.put("occurredAt", a.getOccurredAt());
        m.put("location", a.getLocation());
        m.put("routeArea", a.getRouteArea());
        m.put("orderNo", a.getOrderNo());
        m.put("equipmentStatusDesc", a.getEquipmentStatusDesc());
        m.put("damagedEquipmentTypeId", a.getDamagedEquipmentType() != null ? a.getDamagedEquipmentType().getId() : null);
        m.put("damagedEquipmentTypeName", a.getDamagedEquipmentType() != null ? a.getDamagedEquipmentType().getName() : null);
        m.put("injuryDesc", a.getInjuryDesc());
        m.put("policeRecordNo", a.getPoliceRecordNo());
        m.put("photoUrls", a.getPhotoUrls());
        m.put("photoIds", a.getPhotoIds());
        m.put("weather", a.getWeather() != null ? a.getWeather().name() : null);
        m.put("status", a.getStatus().name());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    public static Map<String, Object> review(AccidentReview r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("accidentId", r.getAccident().getId());
        m.put("reviewerName", r.getReviewer().getName());
        m.put("wasDelivering", r.getWasDelivering());
        m.put("wearingEquipment", r.getWearingEquipment());
        m.put("hasViolation", r.getHasViolation());
        m.put("violationDesc", r.getViolationDesc());
        m.put("insuranceNeeded", r.getInsuranceNeeded());
        m.put("materialsComplete", r.getMaterialsComplete());
        m.put("reviewNotes", r.getReviewNotes());
        m.put("result", r.getResult());
        m.put("reviewedAt", r.getReviewedAt());
        return m;
    }

    public static Map<String, Object> claim(InsuranceClaim c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("accidentId", c.getAccident().getId());
        m.put("riderName", c.getAccident().getRider().getName());
        m.put("claimNo", c.getClaimNo());
        m.put("amount", c.getAmount());
        m.put("status", c.getStatus().name());
        m.put("materialsNotes", c.getMaterialsNotes());
        m.put("createdAt", c.getCreatedAt());
        m.put("updatedAt", c.getUpdatedAt());
        return m;
    }

    public static Map<String, Object> reissue(EquipmentReissue r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("accidentId", r.getAccident().getId());
        m.put("riderId", r.getRider().getId());
        m.put("riderName", r.getRider().getName());
        m.put("typeName", r.getEquipmentType().getName());
        m.put("size", r.getSize());
        m.put("reason", r.getReason());
        m.put("status", r.getStatus().name());
        m.put("createdAt", r.getCreatedAt());
        m.put("processedAt", r.getProcessedAt());
        return m;
    }

    public static Map<String, Object> assessment(RiderAssessment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("riderId", a.getRider().getId());
        m.put("riderName", a.getRider().getName());
        m.put("accidentId", a.getAccident() != null ? a.getAccident().getId() : null);
        m.put("pointsChange", a.getPointsChange());
        m.put("reason", a.getReason());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    public static Map<String, Object> training(TrainingRecord t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("riderId", t.getRider().getId());
        m.put("riderName", t.getRider().getName());
        m.put("title", t.getTitle());
        m.put("category", t.getCategory().name());
        m.put("completedAt", t.getCompletedAt());
        m.put("score", t.getScore());
        return m;
    }

    public static Map<String, Object> policy(PolicyAdjustment p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("stationName", p.getStation().getName());
        m.put("type", p.getType().name());
        m.put("title", p.getTitle());
        m.put("content", p.getContent());
        m.put("createdBy", p.getCreatedBy());
        m.put("createdAt", p.getCreatedAt());
        return m;
    }

    public static Map<String, Object> rainPlan(RainPlan p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("stationId", p.getStation().getId());
        m.put("stationName", p.getStation().getName());
        m.put("title", p.getTitle());
        m.put("season", p.getSeason());
        m.put("rainyDays", p.getRainyDays());
        m.put("weatherFactor", p.getWeatherFactor());
        m.put("shiftsPerWeek", p.getShiftsPerWeek());
        m.put("weatherForecast", p.getWeatherForecast());
        m.put("status", p.getStatus().name());
        m.put("totalRiders", p.getTotalRiders());
        m.put("needReplace", p.getNeedReplace());
        m.put("createdBy", p.getCreatedBy());
        m.put("confirmedBy", p.getConfirmedBy());
        m.put("createdAt", p.getCreatedAt());
        m.put("confirmedAt", p.getConfirmedAt());
        return m;
    }

    public static Map<String, Object> rainPlanItem(RainPlanItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("planId", i.getPlan().getId());
        m.put("riderId", i.getRider().getId());
        m.put("riderName", i.getRider().getName());
        m.put("size", i.getSize());
        m.put("hadValidRaincoat", i.getHadValidRaincoat());
        m.put("oldIssueId", i.getOldIssueId());
        m.put("newIssueId", i.getNewIssueId());
        m.put("status", i.getStatus().name());
        m.put("remindedAt", i.getRemindedAt());
        m.put("issuedAt", i.getIssuedAt());
        return m;
    }

    public static Map<String, Object> rainPlanRestock(RainPlanRestock r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("size", r.getSize());
        m.put("riderCount", r.getRiderCount());
        m.put("needReplace", r.getNeedReplace());
        m.put("lossBase", r.getLossBase());
        m.put("lossSources", r.getLossSources());
        m.put("expectedLoss", r.getExpectedLoss());
        m.put("shiftBuffer", r.getShiftBuffer());
        m.put("demand", r.getDemand());
        m.put("stockBefore", r.getStockBefore());
        m.put("restockQty", r.getRestockQty());
        m.put("applied", r.getApplied());
        return m;
    }

    public static Map<String, Object> reminder(SafetyReminder r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("riderId", r.getRider().getId());
        m.put("planId", r.getPlan() != null ? r.getPlan().getId() : null);
        m.put("type", r.getType().name());
        m.put("title", r.getTitle());
        m.put("content", r.getContent());
        m.put("read", r.getReadFlag());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }
}
