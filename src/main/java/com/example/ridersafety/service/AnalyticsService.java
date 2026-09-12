package com.example.ridersafety.service;

import com.example.ridersafety.model.*;
import com.example.ridersafety.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AccidentReportRepository accidentRepo;
    private final AccidentReviewRepository reviewRepo;
    private final EquipmentIssueRepository issueRepo;
    private final ReplacementRequestRepository requestRepo;
    private final InsuranceClaimRepository claimRepo;
    private final RiderAssessmentRepository assessmentRepo;
    private final TrainingRecordRepository trainingRepo;
    private final UserRepository userRepo;

    private List<AccidentReport> accidents(Long stationId) {
        return stationId != null
                ? accidentRepo.findByRider_Station_IdOrderByOccurredAtDesc(stationId)
                : accidentRepo.findAll();
    }

    private List<EquipmentIssue> issues(Long stationId) {
        return stationId != null
                ? issueRepo.findByStation_IdOrderByIssuedAtDesc(stationId)
                : issueRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(Long stationId) {
        List<AccidentReport> accidents = accidents(stationId);
        List<EquipmentIssue> issues = issues(stationId);
        LocalDate today = LocalDate.now();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("riderCount", stationId != null
                ? userRepo.findByRoleAndStation_Id(Role.RIDER, stationId).size()
                : userRepo.findByRole(Role.RIDER).size());
        m.put("equipmentInUse", issues.stream().filter(i -> i.getStatus() == IssueStatus.IN_USE).count());
        m.put("equipmentExpired", issues.stream().filter(i -> i.getStatus() == IssueStatus.IN_USE
                && i.getExpectedReplaceAt() != null && i.getExpectedReplaceAt().isBefore(today)).count());
        m.put("pendingReplacements", requestRepo.findByStatus(ReplacementStatus.PENDING).stream()
                .filter(r -> stationId == null || r.getIssue().getStation().getId().equals(stationId)).count());
        m.put("pendingAccidents", accidents.stream()
                .filter(a -> a.getStatus() == AccidentStatus.PENDING || a.getStatus() == AccidentStatus.UNDER_REVIEW).count());
        m.put("accidentsThisMonth", accidents.stream()
                .filter(a -> a.getOccurredAt().getMonth() == today.getMonth()
                        && a.getOccurredAt().getYear() == today.getYear()).count());
        m.put("openClaims", (stationId != null
                ? claimRepo.findByAccident_Rider_Station_IdOrderByCreatedAtDesc(stationId) : claimRepo.findAll())
                .stream().filter(c -> c.getStatus() == ClaimStatus.DRAFT || c.getStatus() == ClaimStatus.SUBMITTED).count());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> accidentsByRider(Long stationId) {
        List<AccidentReport> accidents = accidents(stationId);
        Map<Long, List<AccidentReport>> byRider = accidents.stream()
                .collect(Collectors.groupingBy(a -> a.getRider().getId()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (var e : byRider.entrySet()) {
            AccidentReport first = e.getValue().get(0);
            int points = assessmentRepo.findByRider_IdOrderByCreatedAtDesc(e.getKey()).stream()
                    .mapToInt(RiderAssessment::getPointsChange).sum();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("riderId", e.getKey());
            m.put("riderName", first.getRider().getName());
            m.put("stationName", first.getRider().getStation() != null ? first.getRider().getStation().getName() : null);
            m.put("accidentCount", e.getValue().size());
            m.put("assessmentScore", 100 + points);
            m.put("lastAccidentAt", e.getValue().stream().map(AccidentReport::getOccurredAt)
                    .max(LocalDateTime::compareTo).orElse(null));
            result.add(m);
        }
        result.sort((a, b) -> Long.compare((long) b.get("accidentCount"), (long) a.get("accidentCount")));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> accidentsByRoute(Long stationId) {
        Map<String, Long> grouped = accidents(stationId).stream()
                .collect(Collectors.groupingBy(a -> a.getRouteArea() != null ? a.getRouteArea() : "未填写",
                        Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("routeArea", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> accidentsByWeather(Long stationId) {
        Map<String, Long> grouped = accidents(stationId).stream()
                .collect(Collectors.groupingBy(a -> a.getWeather() != null ? a.getWeather().name() : "未知",
                        Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("weather", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).toList();
    }

    /** 装备消耗：各类型累计发放、近 90 天发放、当前在用、已过期 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> equipmentConsumption(Long stationId) {
        List<EquipmentIssue> issues = issues(stationId);
        LocalDateTime since90 = LocalDateTime.now().minusDays(90);
        LocalDate today = LocalDate.now();
        Map<Long, List<EquipmentIssue>> byType = issues.stream()
                .collect(Collectors.groupingBy(i -> i.getEquipmentType().getId()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (var e : byType.entrySet()) {
            List<EquipmentIssue> list = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("typeName", list.get(0).getEquipmentType().getName());
            m.put("typeCode", list.get(0).getEquipmentType().getCode());
            m.put("totalIssued", list.size());
            m.put("issuedLast90d", list.stream().filter(i -> i.getIssuedAt().isAfter(since90)).count());
            m.put("inUse", list.stream().filter(i -> i.getStatus() == IssueStatus.IN_USE).count());
            m.put("expiredInUse", list.stream().filter(i -> i.getStatus() == IssueStatus.IN_USE
                    && i.getExpectedReplaceAt() != null && i.getExpectedReplaceAt().isBefore(today)).count());
            result.add(m);
        }
        result.sort(Comparator.comparing(m -> (String) m.get("typeCode")));
        return result;
    }

    /** 风险告警：雨季集中损坏、头盔过期、保温箱食品安全、保险材料缺失 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> alerts(Long stationId) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 雨季装备集中损坏：近 30 天雨/雪天提交的更换申请数
        long rainyDamage = requestRepo.findByStatus(ReplacementStatus.PENDING).stream()
                .filter(r -> stationId == null || r.getIssue().getStation().getId().equals(stationId))
                .filter(r -> r.getCreatedAt().isAfter(now.minusDays(30)))
                .filter(r -> r.getWeather() == Weather.RAINY || r.getWeather() == Weather.SNOWY)
                .count();
        if (rainyDamage >= 2) {
            alerts.add(alert("HIGH", "雨季装备集中损坏",
                    "近 30 天雨/雪天气相关更换申请 " + rainyDamage + " 起，装备损耗明显加速",
                    "建议上调雨季装备采购量，并加强雨天装备使用培训"));
        }

        // 2. 头盔过期
        long expiredHelmets = issues(stationId).stream()
                .filter(i -> i.getStatus() == IssueStatus.IN_USE)
                .filter(i -> "HELMET".equals(i.getEquipmentType().getCode()))
                .filter(i -> i.getExpectedReplaceAt() != null && i.getExpectedReplaceAt().isBefore(LocalDate.now()))
                .count();
        if (expiredHelmets > 0) {
            alerts.add(alert("HIGH", "头盔过期未更换",
                    "当前有 " + expiredHelmets + " 顶在用头盔已超过更换周期，存在严重安全隐患",
                    "建议立即安排强制更换，并核查头盔采购批次"));
        }

        // 3. 保温箱超期 → 食品安全风险
        long foodRisk = accidents(stationId).stream()
                .filter(a -> a.getType() == AccidentType.FOOD_CONTAMINATION)
                .filter(a -> a.getDamagedEquipmentType() != null
                        && "THERMAL_BOX".equals(a.getDamagedEquipmentType().getCode()))
                .count();
        long expiredBoxes = issues(stationId).stream()
                .filter(i -> i.getStatus() == IssueStatus.IN_USE)
                .filter(i -> "THERMAL_BOX".equals(i.getEquipmentType().getCode()))
                .filter(i -> i.getExpectedReplaceAt() != null && i.getExpectedReplaceAt().isBefore(LocalDate.now()))
                .count();
        if (foodRisk > 0 || expiredBoxes > 0) {
            alerts.add(alert("MEDIUM", "保温箱食品安全风险",
                    "已发生 " + foodRisk + " 起餐品污染事故；当前 " + expiredBoxes + " 个在用保温箱已超期",
                    "建议优先更换超期保温箱，并增加食品安全与保温箱清洁培训"));
        }

        // 4. 保险材料缺失
        long missingMaterials = (stationId != null
                ? claimRepo.findByAccident_Rider_Station_IdOrderByCreatedAtDesc(stationId) : claimRepo.findAll())
                .stream()
                .filter(c -> c.getStatus() == ClaimStatus.DRAFT || c.getStatus() == ClaimStatus.SUBMITTED)
                .filter(c -> {
                    Optional<AccidentReview> rv = reviewRepo.findByAccident_Id(c.getAccident().getId());
                    return rv.map(r -> Boolean.FALSE.equals(r.getMaterialsComplete())).orElse(false);
                })
                .count();
        if (missingMaterials > 0) {
            alerts.add(alert("MEDIUM", "保险材料缺失",
                    "有 " + missingMaterials + " 份理赔单材料不齐全，可能影响赔付进度",
                    "建议完善事故审核规则：材料不齐不予结案，并限期补齐"));
        }
        return alerts;
    }

    private Map<String, Object> alert(String level, String title, String detail, String suggestion) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", level);
        m.put("title", title);
        m.put("detail", detail);
        m.put("suggestion", suggestion);
        return m;
    }

    /**
     * 事故复盘归因：对每起已核查事故，判断是否与装备老化、未佩戴装备、
     * 配送压力（90 天内多起事故）、培训缺失（近 12 个月无安全培训）相关。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> retrospective(Long stationId) {
        List<AccidentReport> accidents = accidents(stationId);
        List<Map<String, Object>> items = new ArrayList<>();
        int agingCount = 0, notWearingCount = 0, pressureCount = 0, trainingGapCount = 0;

        for (AccidentReport a : accidents) {
            Optional<AccidentReview> reviewOpt = reviewRepo.findByAccident_Id(a.getId());
            if (reviewOpt.isEmpty()) continue;
            AccidentReview review = reviewOpt.get();
            User rider = a.getRider();
            LocalDateTime occurred = a.getOccurredAt();
            List<String> factors = new ArrayList<>();

            // 装备老化：事故发生时骑手有在用装备已过更换周期
            List<String> agedTypes = issueRepo.findByRider_IdOrderByIssuedAtDesc(rider.getId()).stream()
                    .filter(i -> !i.getIssuedAt().isAfter(occurred))
                    .filter(i -> i.getStatus() == IssueStatus.IN_USE || i.getStatus() == IssueStatus.REPLACED)
                    .filter(i -> i.getExpectedReplaceAt() != null
                            && i.getExpectedReplaceAt().isBefore(occurred.toLocalDate()))
                    .map(i -> i.getEquipmentType().getName())
                    .distinct().toList();
            boolean aging = !agedTypes.isEmpty();
            if (aging) {
                factors.add("装备老化(" + String.join("、", agedTypes) + "超期)");
                agingCount++;
            }
            // 未佩戴装备
            if (Boolean.FALSE.equals(review.getWearingEquipment())) {
                factors.add("未佩戴装备");
                notWearingCount++;
            }
            // 配送压力：90 天内发生 2 起及以上事故
            long recentAccidents = accidents.stream()
                    .filter(x -> x.getRider().getId().equals(rider.getId()))
                    .filter(x -> !x.getOccurredAt().isAfter(occurred))
                    .filter(x -> x.getOccurredAt().isAfter(occurred.minusDays(90)))
                    .count();
            if (recentAccidents >= 2) {
                factors.add("配送压力(90天内" + recentAccidents + "起事故)");
                pressureCount++;
            }
            // 培训缺失：近 12 个月无安全类培训
            boolean trained = trainingRepo.findByRider_IdOrderByCompletedAtDesc(rider.getId()).stream()
                    .anyMatch(t -> t.getCategory() == TrainingCategory.SAFETY
                            && t.getCompletedAt().isAfter(occurred.minusMonths(12)));
            if (!trained) {
                factors.add("培训缺失(近12个月无安全培训)");
                trainingGapCount++;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("accidentId", a.getId());
            m.put("riderName", rider.getName());
            m.put("type", a.getType().name());
            m.put("occurredAt", occurred);
            m.put("routeArea", a.getRouteArea());
            m.put("weather", a.getWeather() != null ? a.getWeather().name() : null);
            m.put("factors", factors);
            items.add(m);
        }

        int total = items.size();
        List<String> suggestions = new ArrayList<>();
        if (total > 0) {
            if (agingCount * 2 >= total) {
                suggestions.add("装备老化与事故相关性高（" + agingCount + "/" + total + " 起），建议缩短采购/更换周期并强制到期更换");
            }
            if (notWearingCount > 0) {
                suggestions.add("存在未佩戴装备事故（" + notWearingCount + " 起），建议将装备佩戴纳入日常考核与抽查");
            }
            if (pressureCount > 0) {
                suggestions.add("部分骑手短期内多起事故（" + pressureCount + " 起），建议评估配送强度与排班合理性");
            }
            if (trainingGapCount > 0) {
                suggestions.add("培训缺失骑手事故占比高（" + trainingGapCount + " 起），建议对高风险区域骑手加强安全培训");
            }
            if (suggestions.isEmpty()) {
                suggestions.add("暂未发现明显共性归因，继续保持现有采购与培训节奏");
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalReviewed", total);
        summary.put("equipmentAging", agingCount);
        summary.put("notWearing", notWearingCount);
        summary.put("deliveryPressure", pressureCount);
        summary.put("trainingGap", trainingGapCount);
        summary.put("suggestions", suggestions);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("summary", summary);
        return result;
    }
}
