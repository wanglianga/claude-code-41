package com.example.ridersafety.service;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.*;
import com.example.ridersafety.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 雨季雨衣集中更换：根据骑手数量、历史损耗、尺码与天气预报生成补货/更换计划；
 * 站长确认后库存同步补货，骑手领取时同步押金与领用记录；
 * 未领取骑手可发送安全提醒，并影响其雨天接单提示。
 */
@Service
@RequiredArgsConstructor
public class RainPlanService {

    private static final String RAINCOAT_CODE = "RAINCOAT";
    private static final String DEFAULT_SIZE = "L";

    private final RainPlanRepository planRepo;
    private final RainPlanItemRepository itemRepo;
    private final RainPlanRestockRepository restockRepo;
    private final SafetyReminderRepository reminderRepo;
    private final UserRepository userRepo;
    private final EquipmentTypeRepository typeRepo;
    private final EquipmentStockRepository stockRepo;
    private final EquipmentIssueRepository issueRepo;

    // ---------- 计划生成 ----------

    /** 生成雨季雨衣补货与更换计划（DRAFT） */
    @Transactional
    public Map<String, Object> generate(User manager, Integer rainyDays, String forecast) {
        Station station = manager.getStation();
        if (station == null) {
            throw ApiException.badRequest("当前账号未绑定站点");
        }
        if (planRepo.existsByStation_IdAndStatusIn(station.getId(),
                List.of(RainPlanStatus.DRAFT, RainPlanStatus.CONFIRMED))) {
            throw ApiException.badRequest("已存在进行中的雨季计划，请先完成或取消");
        }
        EquipmentType raincoat = typeRepo.findByCode(RAINCOAT_CODE)
                .orElseThrow(() -> ApiException.notFound("雨衣装备类型不存在"));
        int days = rainyDays != null && rainyDays > 0 ? rainyDays : 15;
        String forecastText = (forecast != null && !forecast.isBlank()) ? forecast
                : "气象台预报：未来 30 天预计降雨 " + days + " 天，请提前做好雨季装备保障";

        List<User> riders = userRepo.findByRoleAndStation_Id(Role.RIDER, station.getId());
        if (riders.isEmpty()) {
            throw ApiException.badRequest("站点暂无骑手，无法生成计划");
        }

        RainPlan plan = new RainPlan();
        plan.setStation(station);
        plan.setTitle(station.getName() + " " + LocalDate.now().getYear() + " 年雨季雨衣集中更换计划");
        plan.setSeason(LocalDate.now().getYear() + "-雨季");
        plan.setRainyDays(days);
        plan.setWeatherForecast(forecastText);
        plan.setStatus(RainPlanStatus.DRAFT);
        plan.setTotalRiders(riders.size());
        plan.setCreatedBy(manager.getName());
        plan = planRepo.save(plan);

        // 逐骑手生成更换明细：尺码取自最近雨衣领用，无有效雨衣者列入待领取
        LocalDate today = LocalDate.now();
        List<RainPlanItem> items = new ArrayList<>();
        int needReplace = 0;
        for (User rider : riders) {
            List<EquipmentIssue> raincoats = issueRepo.findByRider_IdOrderByIssuedAtDesc(rider.getId()).stream()
                    .filter(i -> RAINCOAT_CODE.equals(i.getEquipmentType().getCode()))
                    .toList();
            String size = raincoats.stream().map(EquipmentIssue::getSize).findFirst().orElse(DEFAULT_SIZE);
            EquipmentIssue inUse = raincoats.stream()
                    .filter(i -> i.getStatus() == IssueStatus.IN_USE).findFirst().orElse(null);
            boolean valid = inUse != null && inUse.getExpectedReplaceAt() != null
                    && !inUse.getExpectedReplaceAt().isBefore(today);

            RainPlanItem item = new RainPlanItem();
            item.setPlan(plan);
            item.setRider(rider);
            item.setSize(size);
            item.setHadValidRaincoat(valid);
            if (valid) {
                item.setStatus(RainPlanItemStatus.SKIPPED);
            } else {
                item.setStatus(RainPlanItemStatus.PENDING);
                item.setOldIssueId(inUse != null ? inUse.getId() : null);
                needReplace++;
            }
            items.add(itemRepo.save(item));
        }
        plan.setNeedReplace(needReplace);
        planRepo.save(plan);

        // 按尺码计算补货：需求 = 待更换 + 排班缓冲(骑手数×20%) + 历史损耗补充(近90天发放×30%)
        LocalDateTime since90 = LocalDateTime.now().minusDays(90);
        List<EquipmentIssue> stationIssues = issueRepo.findByStation_IdOrderByIssuedAtDesc(station.getId());
        for (String size : raincoat.getSizes().split(",")) {
            String sz = size.trim();
            long riderCount = items.stream().filter(i -> i.getSize().equals(sz)).count();
            long need = items.stream().filter(i -> i.getSize().equals(sz)
                    && i.getStatus() == RainPlanItemStatus.PENDING).count();
            long historyLoss = stationIssues.stream()
                    .filter(i -> RAINCOAT_CODE.equals(i.getEquipmentType().getCode()))
                    .filter(i -> i.getSize().equals(sz))
                    .filter(i -> i.getIssuedAt().isAfter(since90))
                    .count();
            int shiftBuffer = (int) Math.ceil(riderCount * 0.2);
            int historyExtra = (int) Math.ceil(historyLoss * 0.3);
            int demand = (int) need + shiftBuffer + historyExtra;
            int stock = stockRepo.findByStation_IdAndEquipmentType_IdAndSize(
                    station.getId(), raincoat.getId(), sz).map(EquipmentStock::getQuantity).orElse(0);

            RainPlanRestock r = new RainPlanRestock();
            r.setPlan(plan);
            r.setSize(sz);
            r.setRiderCount((int) riderCount);
            r.setNeedReplace((int) need);
            r.setHistoryLoss((int) historyLoss);
            r.setShiftBuffer(shiftBuffer);
            r.setDemand(demand);
            r.setStockBefore(stock);
            r.setRestockQty(Math.max(0, demand - stock));
            r.setApplied(false);
            restockRepo.save(r);
        }
        return detail(plan.getId());
    }

    // ---------- 查询 ----------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long stationId) {
        return planRepo.findByStation_IdOrderByCreatedAtDesc(stationId).stream()
                .map(DtoMapper::rainPlan).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long planId) {
        RainPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> ApiException.notFound("计划不存在"));
        Map<String, Object> m = new LinkedHashMap<>(DtoMapper.rainPlan(plan));
        m.put("items", itemRepo.findByPlan_Id(planId).stream()
                .map(DtoMapper::rainPlanItem).toList());
        m.put("restocks", restockRepo.findByPlan_Id(planId).stream()
                .map(DtoMapper::rainPlanRestock).toList());
        return m;
    }

    // ---------- 确认 / 取消 ----------

    /** 站长确认计划：补货量写入库存 */
    @Transactional
    public Map<String, Object> confirm(User manager, Long planId) {
        RainPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> ApiException.notFound("计划不存在"));
        checkStation(manager, plan);
        if (plan.getStatus() != RainPlanStatus.DRAFT) {
            throw ApiException.badRequest("只有待确认状态的计划可以确认");
        }
        EquipmentType raincoat = typeRepo.findByCode(RAINCOAT_CODE).orElseThrow();
        for (RainPlanRestock r : restockRepo.findByPlan_Id(planId)) {
            if (Boolean.TRUE.equals(r.getApplied())) {
                continue;
            }
            if (r.getRestockQty() > 0) {
                EquipmentStock stock = stockRepo.findByStation_IdAndEquipmentType_IdAndSize(
                                plan.getStation().getId(), raincoat.getId(), r.getSize())
                        .orElseGet(() -> stockRepo.save(new EquipmentStock(
                                plan.getStation(), raincoat, r.getSize(), 0)));
                stock.setQuantity(stock.getQuantity() + r.getRestockQty());
                stockRepo.save(stock);
            }
            r.setApplied(true);
            restockRepo.save(r);
        }
        plan.setStatus(RainPlanStatus.CONFIRMED);
        plan.setConfirmedAt(LocalDateTime.now());
        plan.setConfirmedBy(manager.getName());
        planRepo.save(plan);
        return detail(planId);
    }

    @Transactional
    public Map<String, Object> cancel(User manager, Long planId) {
        RainPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> ApiException.notFound("计划不存在"));
        checkStation(manager, plan);
        if (plan.getStatus() != RainPlanStatus.DRAFT) {
            throw ApiException.badRequest("只有待确认状态的计划可以取消");
        }
        plan.setStatus(RainPlanStatus.CANCELLED);
        planRepo.save(plan);
        return detail(planId);
    }

    // ---------- 领取发放 ----------

    /** 为单个计划明细发放雨衣：扣库存、记押金、旧雨衣标记更换、生成新领用 */
    @Transactional
    public Map<String, Object> issueItem(User manager, Long planId, Long itemId) {
        RainPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> ApiException.notFound("计划不存在"));
        checkStation(manager, plan);
        if (plan.getStatus() != RainPlanStatus.CONFIRMED) {
            throw ApiException.badRequest("计划未确认，无法发放");
        }
        RainPlanItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("计划明细不存在"));
        if (!item.getPlan().getId().equals(planId)) {
            throw ApiException.badRequest("明细不属于该计划");
        }
        if (item.getStatus() != RainPlanItemStatus.PENDING) {
            throw ApiException.badRequest("该明细无需发放或已发放");
        }
        doIssue(plan, item);
        refreshPlanStatus(plan);
        return detail(planId);
    }

    /** 一键发放全部待领取明细；库存不足的明细保留待领取并返回说明 */
    @Transactional
    public Map<String, Object> issueAll(User manager, Long planId) {
        RainPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> ApiException.notFound("计划不存在"));
        checkStation(manager, plan);
        if (plan.getStatus() != RainPlanStatus.CONFIRMED) {
            throw ApiException.badRequest("计划未确认，无法发放");
        }
        List<String> failures = new ArrayList<>();
        int issued = 0;
        for (RainPlanItem item : itemRepo.findByPlan_IdAndStatus(planId, RainPlanItemStatus.PENDING)) {
            try {
                doIssue(plan, item);
                issued++;
            } catch (ApiException e) {
                failures.add(item.getRider().getName() + "（" + item.getSize() + "）: " + e.getMessage());
            }
        }
        refreshPlanStatus(plan);
        Map<String, Object> result = new LinkedHashMap<>(detail(planId));
        result.put("issuedCount", issued);
        result.put("failures", failures);
        return result;
    }

    private void doIssue(RainPlan plan, RainPlanItem item) {
        User rider = item.getRider();
        if (rider.getStation() == null || !rider.getStation().getId().equals(plan.getStation().getId())) {
            throw ApiException.badRequest("骑手不属于计划站点");
        }
        EquipmentType raincoat = typeRepo.findByCode(RAINCOAT_CODE).orElseThrow();
        EquipmentStock stock = stockRepo.findByStation_IdAndEquipmentType_IdAndSize(
                        plan.getStation().getId(), raincoat.getId(), item.getSize())
                .orElseThrow(() -> ApiException.badRequest("站点没有 " + item.getSize() + " 尺码库存记录"));
        if (stock.getQuantity() <= 0) {
            throw ApiException.badRequest(item.getSize() + " 尺码库存不足");
        }
        stock.setQuantity(stock.getQuantity() - 1);
        stockRepo.save(stock);

        // 旧雨衣（如仍在用）标记为已更换
        if (item.getOldIssueId() != null) {
            issueRepo.findById(item.getOldIssueId()).ifPresent(old -> {
                if (old.getStatus() == IssueStatus.IN_USE) {
                    old.setStatus(IssueStatus.REPLACED);
                    old.setReturnedAt(LocalDateTime.now());
                    issueRepo.save(old);
                }
            });
        }
        EquipmentIssue issue = new EquipmentIssue();
        issue.setRider(rider);
        issue.setStation(plan.getStation());
        issue.setEquipmentType(raincoat);
        issue.setSize(item.getSize());
        issue.setIssuedAt(LocalDateTime.now());
        issue.setDepositPaid(raincoat.getDeposit());
        issue.setStatus(IssueStatus.IN_USE);
        issue.setExpectedReplaceAt(LocalDate.now().plusMonths(raincoat.getReplacementCycleMonths()));
        issue.setNotes("雨季集中更换计划 #" + plan.getId() + " 发放（押金 " + raincoat.getDeposit() + " 元）");
        issue = issueRepo.save(issue);

        item.setStatus(RainPlanItemStatus.ISSUED);
        item.setNewIssueId(issue.getId());
        item.setIssuedAt(LocalDateTime.now());
        itemRepo.save(item);
    }

    private void refreshPlanStatus(RainPlan plan) {
        long pending = itemRepo.findByPlan_IdAndStatus(plan.getId(), RainPlanItemStatus.PENDING).size();
        if (pending == 0 && plan.getStatus() == RainPlanStatus.CONFIRMED) {
            plan.setStatus(RainPlanStatus.COMPLETED);
            planRepo.save(plan);
        }
    }

    // ---------- 安全提醒 ----------

    /** 向全部未领取骑手发送安全提醒 */
    @Transactional
    public Map<String, Object> remind(User manager, Long planId) {
        RainPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> ApiException.notFound("计划不存在"));
        checkStation(manager, plan);
        if (plan.getStatus() != RainPlanStatus.CONFIRMED) {
            throw ApiException.badRequest("计划未确认，无法发送提醒");
        }
        List<RainPlanItem> pending = itemRepo.findByPlan_IdAndStatus(planId, RainPlanItemStatus.PENDING);
        if (pending.isEmpty()) {
            throw ApiException.badRequest("没有未领取的骑手");
        }
        LocalDateTime now = LocalDateTime.now();
        for (RainPlanItem item : pending) {
            reminderRepo.save(new SafetyReminder(item.getRider(), plan, ReminderType.RAINCOAT_PICKUP,
                    "雨季雨衣领取提醒",
                    "雨季将至（预计降雨 " + plan.getRainyDays() + " 天），您尚未领取新雨衣（尺码 "
                            + item.getSize() + "）。请尽快到站点领取，雨天配送务必规范穿着，未领取将影响雨天接单安全提示。"));
            item.setRemindedAt(now);
            itemRepo.save(item);
        }
        Map<String, Object> result = new LinkedHashMap<>(detail(planId));
        result.put("remindedCount", pending.size());
        return result;
    }

    // ---------- 骑手端 ----------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myReminders(User rider) {
        return reminderRepo.findByRider_IdOrderByCreatedAtDesc(rider.getId()).stream()
                .map(DtoMapper::reminder).toList();
    }

    @Transactional
    public Map<String, Object> markReminderRead(User rider, Long reminderId) {
        SafetyReminder r = reminderRepo.findById(reminderId)
                .orElseThrow(() -> ApiException.notFound("提醒不存在"));
        if (!r.getRider().getId().equals(rider.getId())) {
            throw ApiException.forbidden("只能操作自己的提醒");
        }
        r.setReadFlag(true);
        return DtoMapper.reminder(reminderRepo.save(r));
    }

    /** 雨天接单提示：未领取/无有效雨衣的骑手将收到警示 */
    @Transactional(readOnly = true)
    public Map<String, Object> rainReadiness(User rider) {
        LocalDate today = LocalDate.now();
        EquipmentIssue validRaincoat = issueRepo.findByRider_IdAndStatus(rider.getId(), IssueStatus.IN_USE).stream()
                .filter(i -> RAINCOAT_CODE.equals(i.getEquipmentType().getCode()))
                .filter(i -> i.getExpectedReplaceAt() != null && !i.getExpectedReplaceAt().isBefore(today))
                .findFirst().orElse(null);

        List<RainPlanItem> pendingItems = itemRepo.findByRider_IdAndStatus(
                rider.getId(), RainPlanItemStatus.PENDING);
        // 仅统计已确认计划中的待领取
        List<RainPlanItem> activePending = pendingItems.stream()
                .filter(i -> i.getPlan().getStatus() == RainPlanStatus.CONFIRMED)
                .toList();
        long unread = reminderRepo.countByRider_IdAndReadFlagFalse(rider.getId());

        String weatherAlert = null;
        if (rider.getStation() != null) {
            weatherAlert = planRepo.findByStation_IdOrderByCreatedAtDesc(rider.getStation().getId()).stream()
                    .filter(p -> p.getStatus() == RainPlanStatus.CONFIRMED)
                    .map(RainPlan::getWeatherForecast).findFirst().orElse(null);
        }

        boolean pendingPickup = !activePending.isEmpty();
        String orderPrompt;
        if (pendingPickup) {
            orderPrompt = "⚠️ 雨天接单提示：您尚未领取雨季新雨衣（尺码 " + activePending.get(0).getSize()
                    + "），雨天配送缺少有效防雨防护，请尽快到站点领取后再接单";
        } else if (validRaincoat == null) {
            orderPrompt = "⚠️ 雨天接单提示：您的雨衣已过期或缺失，雨天配送请谨慎接单并尽快到站点更换";
        } else {
            orderPrompt = "雨天配送请规范穿着雨衣（有效期至 "
                    + validRaincoat.getExpectedReplaceAt() + "），注意路滑减速慢行";
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hasValidRaincoat", validRaincoat != null);
        m.put("raincoatExpiry", validRaincoat != null ? validRaincoat.getExpectedReplaceAt() : null);
        m.put("pendingPickup", pendingPickup);
        m.put("pendingPlanId", pendingPickup ? activePending.get(0).getPlan().getId() : null);
        m.put("pendingSize", pendingPickup ? activePending.get(0).getSize() : null);
        m.put("unreadReminders", unread);
        m.put("weatherAlert", weatherAlert);
        m.put("orderPrompt", orderPrompt);
        return m;
    }

    private void checkStation(User manager, RainPlan plan) {
        if (manager.getStation() != null
                && !manager.getStation().getId().equals(plan.getStation().getId())) {
            throw ApiException.forbidden("只能操作本站点的计划");
        }
    }
}
