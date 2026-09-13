package com.example.ridersafety.service;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.*;
import com.example.ridersafety.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccidentService {

    private final AccidentReportRepository accidentRepo;
    private final AccidentReviewRepository reviewRepo;
    private final InsuranceClaimRepository claimRepo;
    private final EquipmentReissueRepository reissueRepo;
    private final RiderAssessmentRepository assessmentRepo;
    private final EquipmentIssueRepository issueRepo;
    private final EquipmentStockRepository stockRepo;
    private final EquipmentTypeRepository typeRepo;
    private final AttachmentService attachmentService;

    /** 骑手提交事故申报 */
    @Transactional
    public Map<String, Object> report(User rider, AccidentType type, LocalDateTime occurredAt, String location,
                                      String routeArea, String orderNo, String equipmentStatusDesc,
                                      Long damagedEquipmentTypeId, String injuryDesc, String policeRecordNo,
                                      String photoUrls, Weather weather, List<Long> photoIds) {
        AccidentReport a = new AccidentReport();
        a.setRider(rider);
        a.setType(type);
        a.setOccurredAt(occurredAt != null ? occurredAt : LocalDateTime.now());
        a.setLocation(location);
        a.setRouteArea(routeArea);
        a.setOrderNo(orderNo);
        a.setEquipmentStatusDesc(equipmentStatusDesc);
        if (damagedEquipmentTypeId != null) {
            a.setDamagedEquipmentType(typeRepo.findById(damagedEquipmentTypeId)
                    .orElseThrow(() -> ApiException.notFound("装备类型不存在")));
        }
        a.setInjuryDesc(injuryDesc);
        a.setPoliceRecordNo(policeRecordNo);
        a.setPhotoUrls(photoUrls);
        a.setPhotoIds(AttachmentService.joinIds(photoIds));
        a.setWeather(weather);
        a.setStatus(AccidentStatus.PENDING);
        return withPhotos(accidentRepo.save(a));
    }

    /** 事故 DTO + 附件照片记录（列表与详情共用，保证 photos 一致） */
    private Map<String, Object> withPhotos(AccidentReport a) {
        Map<String, Object> m = DtoMapper.accident(a);
        m.put("photos", attachmentService.photosOf(a.getPhotoIds()));
        return m;
    }

    /** 某时刻骑手的装备配备状态：未配备 / 有效 / 已过期 */
    private Map<String, Object> protectionAt(User rider, String typeCode, LocalDateTime at) {
        Map<String, Object> m = new LinkedHashMap<>();
        EquipmentIssue active = issueRepo.findByRider_IdOrderByIssuedAtDesc(rider.getId()).stream()
                .filter(i -> typeCode.equals(i.getEquipmentType().getCode()))
                .filter(i -> !i.getIssuedAt().isAfter(at))
                .filter(i -> i.getReturnedAt() == null || i.getReturnedAt().isAfter(at))
                .findFirst().orElse(null);
        if (active == null) {
            m.put("status", "NONE");
            m.put("label", "未配备");
            return m;
        }
        boolean valid = active.getExpectedReplaceAt() != null
                && !active.getExpectedReplaceAt().isBefore(at.toLocalDate());
        m.put("status", valid ? "VALID" : "EXPIRED");
        m.put("label", valid ? "有效" : "已过期");
        m.put("size", active.getSize());
        m.put("issuedAt", active.getIssuedAt());
        m.put("expectedReplaceAt", active.getExpectedReplaceAt());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myAccidents(User rider) {
        return accidentRepo.findByRider_IdOrderByOccurredAtDesc(rider.getId()).stream()
                .map(this::withPhotos).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> accidentsForStation(Long stationId, String status) {
        return accidentRepo.findByRider_Station_IdOrderByOccurredAtDesc(stationId).stream()
                .filter(a -> status == null || a.getStatus().name().equals(status))
                .map(this::withPhotos).toList();
    }

    /** 事故详情：事故 + 核查 + 保险 + 补发 + 关联考核，串在同一事件视图 */
    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long accidentId) {
        AccidentReport a = accidentRepo.findById(accidentId)
                .orElseThrow(() -> ApiException.notFound("事故不存在"));
        Map<String, Object> m = new LinkedHashMap<>(withPhotos(a));
        // 防护配置追溯：事故发生时骑手的雨衣/头盔配备与有效期状态
        Map<String, Object> protection = new LinkedHashMap<>();
        protection.put("raincoat", protectionAt(a.getRider(), "RAINCOAT", a.getOccurredAt()));
        protection.put("helmet", protectionAt(a.getRider(), "HELMET", a.getOccurredAt()));
        m.put("protection", protection);
        reviewRepo.findByAccident_Id(accidentId).ifPresent(r -> m.put("review", DtoMapper.review(r)));
        m.put("claims", claimRepo.findByAccident_Id(accidentId).stream().map(DtoMapper::claim).toList());
        m.put("reissues", reissueRepo.findByAccident_Id(accidentId).stream().map(DtoMapper::reissue).toList());
        m.put("assessments", assessmentRepo.findByRider_IdOrderByCreatedAtDesc(a.getRider().getId()).stream()
                .filter(x -> x.getAccident() != null && x.getAccident().getId().equals(accidentId))
                .map(DtoMapper::assessment).toList());
        return m;
    }

    /**
     * 站长核查：确认是否在配送中、是否佩戴装备、是否违规、是否需要保险材料；
     * 核查通过时联动生成保险理赔单、装备补发单、骑手考核扣分。
     */
    @Transactional
    public Map<String, Object> review(User manager, Long accidentId, Boolean wasDelivering,
                                      Boolean wearingEquipment, Boolean hasViolation, String violationDesc,
                                      Boolean insuranceNeeded, Boolean materialsComplete,
                                      String reviewNotes, String result) {
        AccidentReport a = accidentRepo.findById(accidentId)
                .orElseThrow(() -> ApiException.notFound("事故不存在"));
        if (manager.getStation() != null && a.getRider().getStation() != null
                && !manager.getStation().getId().equals(a.getRider().getStation().getId())) {
            throw ApiException.forbidden("只能核查本站点骑手的事故");
        }
        if (a.getStatus() == AccidentStatus.APPROVED || a.getStatus() == AccidentStatus.REJECTED) {
            throw ApiException.badRequest("该事故已完成核查");
        }
        if (!"APPROVED".equals(result) && !"REJECTED".equals(result)) {
            throw ApiException.badRequest("核查结论必须是 APPROVED 或 REJECTED");
        }
        AccidentReview rv = new AccidentReview();
        rv.setAccident(a);
        rv.setReviewer(manager);
        rv.setWasDelivering(wasDelivering);
        rv.setWearingEquipment(wearingEquipment);
        rv.setHasViolation(hasViolation);
        rv.setViolationDesc(violationDesc);
        rv.setInsuranceNeeded(insuranceNeeded);
        rv.setMaterialsComplete(materialsComplete);
        rv.setReviewNotes(reviewNotes);
        rv.setResult(result);
        reviewRepo.save(rv);

        a.setStatus("APPROVED".equals(result) ? AccidentStatus.APPROVED : AccidentStatus.REJECTED);
        accidentRepo.save(a);

        if ("APPROVED".equals(result)) {
            // 联动 1：需要保险 → 生成理赔单
            if (Boolean.TRUE.equals(insuranceNeeded)) {
                InsuranceClaim claim = new InsuranceClaim();
                claim.setAccident(a);
                claim.setClaimNo(String.format("INS-%d-%04d", LocalDate.now().getYear(), a.getId()));
                claim.setStatus(ClaimStatus.DRAFT);
                claim.setMaterialsNotes(Boolean.TRUE.equals(materialsComplete)
                        ? "材料齐全" : "材料缺失，请骑手 3 日内补齐（交警记录/照片/医疗单据）");
                claimRepo.save(claim);
            }
            // 联动 2：装备损坏 → 生成补发单
            if (a.getDamagedEquipmentType() != null) {
                EquipmentReissue reissue = new EquipmentReissue();
                reissue.setAccident(a);
                reissue.setRider(a.getRider());
                reissue.setEquipmentType(a.getDamagedEquipmentType());
                String size = issueRepo.findByRider_IdAndStatus(a.getRider().getId(), IssueStatus.IN_USE).stream()
                        .filter(i -> i.getEquipmentType().getId().equals(a.getDamagedEquipmentType().getId()))
                        .map(EquipmentIssue::getSize).findFirst()
                        .orElse(a.getDamagedEquipmentType().getSizes() != null
                                ? a.getDamagedEquipmentType().getSizes().split(",")[0] : "通用");
                reissue.setSize(size);
                reissue.setReason("事故 #" + a.getId() + " 中装备损坏补发");
                reissueRepo.save(reissue);
            }
            // 联动 3：违规 / 未佩戴装备 → 考核扣分
            if (Boolean.TRUE.equals(hasViolation)) {
                assessmentRepo.save(new RiderAssessment(a.getRider(), a, -10,
                        "事故核查确认违规骑行" + (violationDesc != null ? "：" + violationDesc : "")));
            }
            if (Boolean.FALSE.equals(wearingEquipment)) {
                assessmentRepo.save(new RiderAssessment(a.getRider(), a, -5, "事故时未按要求佩戴装备"));
            }
        }
        return detail(accidentId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> claimsForStation(Long stationId) {
        return claimRepo.findByAccident_Rider_Station_IdOrderByCreatedAtDesc(stationId).stream()
                .map(DtoMapper::claim).toList();
    }

    @Transactional
    public Map<String, Object> updateClaim(Long claimId, String status, BigDecimal amount, String materialsNotes) {
        InsuranceClaim c = claimRepo.findById(claimId)
                .orElseThrow(() -> ApiException.notFound("理赔单不存在"));
        if (status != null) {
            c.setStatus(ClaimStatus.valueOf(status));
        }
        if (amount != null) {
            c.setAmount(amount);
        }
        if (materialsNotes != null) {
            c.setMaterialsNotes(materialsNotes);
        }
        c.setUpdatedAt(LocalDateTime.now());
        return DtoMapper.claim(claimRepo.save(c));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> reissuesForStation(Long stationId) {
        return reissueRepo.findByRider_Station_IdOrderByCreatedAtDesc(stationId).stream()
                .map(DtoMapper::reissue).toList();
    }

    /** 处理补发单：发放（扣库存 + 生成新领用记录）或取消 */
    @Transactional
    public Map<String, Object> processReissue(Long reissueId, String action) {
        EquipmentReissue r = reissueRepo.findById(reissueId)
                .orElseThrow(() -> ApiException.notFound("补发单不存在"));
        if (r.getStatus() != ReissueStatus.PENDING) {
            throw ApiException.badRequest("该补发单已处理");
        }
        if ("ISSUE".equals(action)) {
            Station station = r.getRider().getStation();
            EquipmentStock stock = stockRepo.findByStation_IdAndEquipmentType_IdAndSize(
                            station.getId(), r.getEquipmentType().getId(), r.getSize())
                    .orElseThrow(() -> ApiException.badRequest("站点没有该尺码库存记录，无法补发"));
            if (stock.getQuantity() <= 0) {
                throw ApiException.badRequest("站点库存不足，无法补发，请先采购");
            }
            stock.setQuantity(stock.getQuantity() - 1);
            stockRepo.save(stock);

            EquipmentIssue issue = new EquipmentIssue();
            issue.setRider(r.getRider());
            issue.setStation(station);
            issue.setEquipmentType(r.getEquipmentType());
            issue.setSize(r.getSize());
            issue.setIssuedAt(LocalDateTime.now());
            issue.setDepositPaid(BigDecimal.ZERO);
            issue.setStatus(IssueStatus.IN_USE);
            issue.setExpectedReplaceAt(LocalDate.now().plusMonths(r.getEquipmentType().getReplacementCycleMonths()));
            issue.setNotes("事故 #" + r.getAccident().getId() + " 补发（免押金）");
            issueRepo.save(issue);
            r.setStatus(ReissueStatus.ISSUED);
        } else if ("CANCEL".equals(action)) {
            r.setStatus(ReissueStatus.CANCELLED);
        } else {
            throw ApiException.badRequest("不支持的动作: " + action);
        }
        r.setProcessedAt(LocalDateTime.now());
        return DtoMapper.reissue(reissueRepo.save(r));
    }
}
