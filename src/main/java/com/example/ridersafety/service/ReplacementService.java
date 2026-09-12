package com.example.ridersafety.service;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.*;
import com.example.ridersafety.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReplacementService {

    private final ReplacementRequestRepository requestRepo;
    private final EquipmentIssueRepository issueRepo;
    private final EquipmentStockRepository stockRepo;
    private final RiderAssessmentRepository assessmentRepo;

    private static final Set<String> RAIN_SENSITIVE = Set.of("RAINCOAT", "HELMET", "VEST");

    /** 骑手提交更换申请，系统自动评估（磨损照片、天气、历史领用、站点库存） */
    @Transactional
    public Map<String, Object> create(User rider, Long issueId, String reason, Weather weather, String wearPhotos) {
        EquipmentIssue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> ApiException.notFound("领用记录不存在"));
        if (!issue.getRider().getId().equals(rider.getId())) {
            throw ApiException.forbidden("只能为自己的装备申请更换");
        }
        if (issue.getStatus() != IssueStatus.IN_USE) {
            throw ApiException.badRequest("该装备当前不在使用中，无法申请更换");
        }
        boolean hasPending = requestRepo.findByRider_IdOrderByCreatedAtDesc(rider.getId()).stream()
                .anyMatch(r -> r.getIssue().getId().equals(issueId) && r.getStatus() == ReplacementStatus.PENDING);
        if (hasPending) {
            throw ApiException.badRequest("该装备已有待处理的更换申请");
        }
        ReplacementRequest req = new ReplacementRequest();
        req.setRider(rider);
        req.setIssue(issue);
        req.setReason(reason);
        req.setWeather(weather);
        req.setWearPhotos(wearPhotos);
        evaluate(req);
        return DtoMapper.replacement(requestRepo.save(req));
    }

    /**
     * 规则引擎：根据使用时长 vs 更换周期、天气加速损耗、近 6 个月更换频率、站点库存，
     * 给出建议：免费更换 / 押金扣减更换 / 维修 / 驳回。
     */
    private void evaluate(ReplacementRequest req) {
        EquipmentIssue issue = req.getIssue();
        EquipmentType type = issue.getEquipmentType();
        LocalDateTime now = LocalDateTime.now();
        long monthsUsed = ChronoUnit.MONTHS.between(issue.getIssuedAt(), now);
        int cycle = type.getReplacementCycleMonths();

        StringBuilder eval = new StringBuilder();
        eval.append("装备已使用 ").append(monthsUsed).append(" 个月，标准更换周期 ").append(cycle).append(" 个月。");

        // 天气加速损耗：雨季/雪季对雨衣、头盔、反光衣损耗加速，周期按 70% 计
        boolean rainy = req.getWeather() == Weather.RAINY || req.getWeather() == Weather.SNOWY;
        double effectiveCycle = cycle;
        if (rainy && RAIN_SENSITIVE.contains(type.getCode())) {
            effectiveCycle = cycle * 0.7;
            eval.append("当前为雨/雪季，").append(type.getName()).append("属雨季高损耗装备，考核周期按 70% 计（")
                    .append(String.format("%.1f", effectiveCycle)).append(" 个月）。");
        }

        // 历史领用：近 6 个月同类型已批准更换次数
        long recentApproved = requestRepo.findByRider_IdOrderByCreatedAtDesc(req.getRider().getId()).stream()
                .filter(r -> r.getIssue().getEquipmentType().getId().equals(type.getId()))
                .filter(r -> r.getStatus() == ReplacementStatus.APPROVED_FREE
                        || r.getStatus() == ReplacementStatus.APPROVED_DEPOSIT)
                .filter(r -> r.getCreatedAt().isAfter(now.minusMonths(6)))
                .count();
        eval.append("近 6 个月同类装备已更换 ").append(recentApproved).append(" 次。");

        // 站点库存
        int stock = stockRepo.findByStation_IdAndEquipmentType_IdAndSize(
                        issue.getStation().getId(), type.getId(), issue.getSize())
                .map(EquipmentStock::getQuantity).orElse(0);
        eval.append("站点该尺码当前库存 ").append(stock).append(" 件。");

        ReplacementStatus decision;
        BigDecimal deduct = BigDecimal.ZERO;
        if (recentApproved >= 2) {
            decision = ReplacementStatus.APPROVED_DEPOSIT;
            deduct = type.getDeposit().multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
            eval.append("结论：更换频率异常（≥2 次/半年），判定为人为损耗风险，建议扣减 50% 押金后更换。");
        } else if (monthsUsed >= effectiveCycle) {
            if (stock > 0) {
                decision = ReplacementStatus.APPROVED_FREE;
                eval.append("结论：已达到更换周期，属正常磨损，建议免费更换。");
            } else {
                decision = ReplacementStatus.REPAIR;
                eval.append("结论：已达更换周期但站点库存不足，建议先维修并通知站点采购补货。");
            }
        } else if (monthsUsed >= effectiveCycle * 0.6) {
            if (stock > 0) {
                decision = ReplacementStatus.APPROVED_DEPOSIT;
                deduct = type.getDeposit().multiply(new BigDecimal("0.3")).setScale(2, RoundingMode.HALF_UP);
                eval.append("结论：未达更换周期但磨损明显，建议扣减 30% 押金后更换。");
            } else {
                decision = ReplacementStatus.REPAIR;
                eval.append("结论：未达更换周期且库存不足，建议维修处理。");
            }
        } else {
            decision = ReplacementStatus.REPAIR;
            eval.append("结论：使用时间较短，磨损程度不支持整体更换，建议维修。");
        }
        req.setEvaluation(eval.toString());
        req.setSuggestedDecision(decision);
        req.setDepositDeducted(deduct);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myRequests(User rider) {
        return requestRepo.findByRider_IdOrderByCreatedAtDesc(rider.getId()).stream()
                .map(DtoMapper::replacement).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> requestsForStation(Long stationId, String status) {
        return requestRepo.findByRider_Station_IdOrderByCreatedAtDesc(stationId).stream()
                .filter(r -> status == null || r.getStatus().name().equals(status))
                .map(DtoMapper::replacement).toList();
    }

    /** 站长处理更换申请：免费更换 / 押金扣减更换 / 维修 / 驳回 */
    @Transactional
    public Map<String, Object> process(User manager, Long requestId, String action,
                                       BigDecimal depositDeducted, String note) {
        ReplacementRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("申请不存在"));
        if (req.getStatus() != ReplacementStatus.PENDING) {
            throw ApiException.badRequest("该申请已处理过");
        }
        if (manager.getStation() != null
                && !manager.getStation().getId().equals(req.getIssue().getStation().getId())) {
            throw ApiException.forbidden("只能处理本站点的申请");
        }
        switch (action) {
            case "APPROVE_FREE" -> {
                req.setStatus(ReplacementStatus.APPROVED_FREE);
                req.setDepositDeducted(BigDecimal.ZERO);
                doReplace(req, BigDecimal.ZERO);
            }
            case "APPROVE_DEPOSIT" -> {
                BigDecimal deduct = depositDeducted != null ? depositDeducted : req.getDepositDeducted();
                if (deduct == null || deduct.signum() <= 0) {
                    throw ApiException.badRequest("请填写押金扣减金额");
                }
                req.setStatus(ReplacementStatus.APPROVED_DEPOSIT);
                req.setDepositDeducted(deduct);
                doReplace(req, deduct);
                assessmentRepo.save(new RiderAssessment(req.getRider(), null, -5,
                        "装备未达周期更换，扣减押金 " + deduct + " 元（申请 #" + req.getId() + "）"));
            }
            case "REPAIR" -> req.setStatus(ReplacementStatus.REPAIR);
            case "REJECT" -> req.setStatus(ReplacementStatus.REJECTED);
            default -> throw ApiException.badRequest("不支持的处理动作: " + action);
        }
        req.setProcessedBy(manager);
        req.setProcessedAt(LocalDateTime.now());
        req.setProcessNote(note);
        return DtoMapper.replacement(requestRepo.save(req));
    }

    /** 执行更换：旧装备标记已更换，扣库存，生成新领用记录 */
    private void doReplace(ReplacementRequest req, BigDecimal deduct) {
        EquipmentIssue old = req.getIssue();
        EquipmentType type = old.getEquipmentType();
        EquipmentStock stock = stockRepo.findByStation_IdAndEquipmentType_IdAndSize(
                        old.getStation().getId(), type.getId(), old.getSize())
                .orElseThrow(() -> ApiException.badRequest("站点没有该尺码库存记录，无法更换"));
        if (stock.getQuantity() <= 0) {
            throw ApiException.badRequest("站点库存不足，无法执行更换，请先采购补货");
        }
        stock.setQuantity(stock.getQuantity() - 1);
        stockRepo.save(stock);

        old.setStatus(IssueStatus.REPLACED);
        old.setReturnedAt(LocalDateTime.now());
        issueRepo.save(old);

        EquipmentIssue ni = new EquipmentIssue();
        ni.setRider(old.getRider());
        ni.setStation(old.getStation());
        ni.setEquipmentType(type);
        ni.setSize(old.getSize());
        ni.setIssuedAt(LocalDateTime.now());
        ni.setDepositPaid(type.getDeposit().subtract(deduct).max(BigDecimal.ZERO));
        ni.setStatus(IssueStatus.IN_USE);
        ni.setExpectedReplaceAt(LocalDate.now().plusMonths(type.getReplacementCycleMonths()));
        ni.setNotes("更换申请 #" + req.getId() + " 换发新装备");
        issueRepo.save(ni);
    }
}
