package com.example.ridersafety.config;

import com.example.ridersafety.model.*;
import com.example.ridersafety.repository.*;
import com.example.ridersafety.service.AccidentService;
import com.example.ridersafety.service.ReplacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 首次启动时初始化演示数据（幂等：仅当用户表为空时执行） */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StationRepository stationRepo;
    private final UserRepository userRepo;
    private final EquipmentTypeRepository typeRepo;
    private final EquipmentStockRepository stockRepo;
    private final EquipmentIssueRepository issueRepo;
    private final AccidentReportRepository accidentRepo;
    private final TrainingRecordRepository trainingRepo;
    private final RiderAssessmentRepository assessmentRepo;
    private final PolicyAdjustmentRepository policyRepo;
    private final EquipmentReissueRepository reissueRepo;
    private final InsuranceClaimRepository claimRepo;
    private final ReplacementService replacementService;
    private final AccidentService accidentService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) {
            return;
        }
        // ---- 站点 ----
        Station s1 = stationRepo.save(new Station("城东配送站", "杭州", "文三路 100 号"));
        Station s2 = stationRepo.save(new Station("城西配送站", "杭州", "余杭塘路 200 号"));

        // ---- 用户 ----
        userRepo.save(new User("admin", passwordEncoder.encode("admin123"), "系统管理员", Role.ADMIN, null));
        User m1 = userRepo.save(new User("manager1", passwordEncoder.encode("manager123"), "陈明", Role.STATION_MANAGER, s1));
        User m2 = userRepo.save(new User("manager2", passwordEncoder.encode("manager123"), "刘芳", Role.STATION_MANAGER, s2));
        User r1 = rider("rider01", "张伟", s1);
        User r2 = rider("rider02", "李强", s1);
        User r3 = rider("rider03", "王芳", s2);
        User r4 = rider("rider04", "赵磊", s2);

        // ---- 装备类型 ----
        EquipmentType helmet = type("HELMET", "安全头盔", "100", 12, "M,L,XL", "骑行安全头盔，卡扣需系紧");
        EquipmentType vest = type("VEST", "反光衣", "30", 6, "M,L,XL,XXL", "夜间反光背心");
        EquipmentType raincoat = type("RAINCOAT", "雨衣", "50", 6, "L,XL,XXL", "分体式防雨雨衣");
        EquipmentType box = type("THERMAL_BOX", "保温箱", "150", 18, "标准,大容量", "餐品保温箱，需定期清洁消毒");
        EquipmentType mount = type("BIKE_MOUNT", "电动车支架", "40", 12, "通用", "手机/导航支架");
        EquipmentType charger = type("CHARGER", "充电器", "80", 12, "48V,60V", "电动车原装充电器");

        // ---- 站点库存（雨衣库存刻意压低，用于雨季补货计划演示） ----
        EquipmentType[] types = {helmet, vest, raincoat, box, mount, charger};
        seedStock(s1, types, new int[][]{{5, 8, 6}, {6, 8, 8, 4}, {10, 1, 2}, {6, 4}, {8}, {6, 6}});
        seedStock(s2, types, new int[][]{{4, 6, 5}, {5, 6, 6, 3}, {8, 2, 1}, {5, 3}, {6}, {5, 5}});

        // ---- 历史领用（月数前：部分已超期，用于复盘与告警演示） ----
        EquipmentIssue r1Helmet = issue(r1, s1, helmet, "L", 14);      // 已过期
        issue(r1, s1, vest, "L", 3);
        EquipmentIssue r1Raincoat = issue(r1, s1, raincoat, "XL", 7);  // 已过期
        issue(r1, s1, box, "标准", 20);                                 // 已过期
        issue(r1, s1, charger, "48V", 2);

        issue(r2, s1, helmet, "M", 5);
        issue(r2, s1, vest, "M", 2);
        issue(r2, s1, raincoat, "L", 1);
        issue(r2, s1, box, "标准", 6);
        EquipmentIssue r2Mount = issue(r2, s1, mount, "通用", 13);     // 已过期

        issue(r3, s2, helmet, "M", 11);
        issue(r3, s2, vest, "M", 7);                                   // 已过期
        issue(r3, s2, raincoat, "L", 2);
        issue(r3, s2, box, "标准", 20);                                 // 已过期
        EquipmentIssue r3Charger = issue(r3, s2, charger, "48V", 15);  // 已过期

        issue(r4, s2, helmet, "XL", 1);
        issue(r4, s2, vest, "XL", 1);
        EquipmentIssue r4Raincoat = issue(r4, s2, raincoat, "XXL", 8); // 已过期
        issue(r4, s2, box, "大容量", 24);                               // 已过期
        issue(r4, s2, mount, "通用", 4);

        // ---- 更换申请（走真实规则引擎） ----
        // 待处理：雨季雨衣磨损
        replacementService.create(r1, r1Raincoat.getId(), "雨衣多处开线渗水，雨季无法继续使用", Weather.RAINY, "https://img.example.com/wear1.jpg", null);
        replacementService.create(r4, r4Raincoat.getId(), "雨季磨损严重，袖口破损", Weather.RAINY, "https://img.example.com/wear2.jpg", null);
        // 已处理：支架到期免费更换
        var req2 = replacementService.create(r2, r2Mount.getId(), "支架松动无法固定手机", Weather.SUNNY, null, null);
        replacementService.process(m1, ((Number) req2.get("id")).longValue(), "APPROVE_FREE", null, "已到更换周期，免费更换");
        // 已处理：充电器未妥善保管，扣押金更换（站长覆盖系统建议）
        var req3 = replacementService.create(r3, r3Charger.getId(), "充电器进水无法充电", Weather.SUNNY, null, null);
        replacementService.process(m2, ((Number) req3.get("id")).longValue(), "APPROVE_DEPOSIT", new BigDecimal("24"), "人为进水，扣减 30% 押金");

        // ---- 事故申报与核查（走真实联动逻辑） ----
        // 1. 张伟 交通事故（雨天、头盔碎裂）→ 核查通过 → 保险 + 补发头盔
        AccidentReport a1 = accident(r1, AccidentType.TRAFFIC_ACCIDENT, 10, "文三路与学院路交叉口", "文三路线",
                "ORD20260901001", "头盔外壳碎裂，反光衣完好", helmet, "手臂擦伤", "JZ-2026-0901", Weather.RAINY);
        accidentService.review(m1, a1.getId(), true, true, false, null, true, true,
                "配送途中被追尾，材料齐全，走保险流程", "APPROVED");
        Long claim1 = claimRepo.findByAccident_Id(a1.getId()).get(0).getId();
        accidentService.updateClaim(claim1, "SUBMITTED", new BigDecimal("2000"), "材料齐全，已提交保险公司");
        Long reissue1 = reissueRepo.findByAccident_Id(a1.getId()).get(0).getId();
        accidentService.processReissue(reissue1, "ISSUE");

        // 2. 张伟 装备损坏（保温箱开裂）→ 待核查
        accident(r1, AccidentType.EQUIPMENT_DAMAGE, 3, "文二路配送途中", "文三路线",
                "ORD20260909012", "保温箱箱体开裂，密封条老化，餐品温度无法保证", box, "无", null, Weather.RAINY);

        // 3. 李强 摔倒（未佩戴头盔、超速）→ 保险材料缺失 + 考核扣分
        AccidentReport a3 = accident(r2, AccidentType.FALL, 20, "古墩路下坡路段", "古墩路线",
                "ORD20260823005", "事故时未佩戴头盔", null, "膝盖挫伤", null, Weather.SUNNY);
        accidentService.review(m1, a3.getId(), true, false, true, "未佩戴头盔且超速行驶", true, false,
                "缺少交警事故认定书原件，限期补齐", "APPROVED");

        // 4. 王芳 餐品污染（保温箱老化）→ 补发保温箱
        AccidentReport a4 = accident(r3, AccidentType.FOOD_CONTAMINATION, 15, "余杭塘路配送点", "余杭塘线",
                "ORD20260828009", "保温箱内衬破损导致餐品洒漏污染", box, "无", null, Weather.EXTREME_HEAT);
        accidentService.review(m2, a4.getId(), true, true, false, null, false, true,
                "保温箱超期使用导致，非骑手人为责任", "APPROVED");

        // 5. 赵磊 交通事故（雨天）→ 核查中
        AccidentReport a5 = accident(r4, AccidentType.TRAFFIC_ACCIDENT, 5, "莫干山路红绿灯口", "莫干山路线",
                "ORD20260907003", "雨衣被刮破，车辆侧滑", raincoat, "轻微擦伤", "JZ-2026-0907", Weather.RAINY);
        a5.setStatus(AccidentStatus.UNDER_REVIEW);
        accidentRepo.save(a5);

        // 6. 李强 45 天前摔倒（已结案）
        AccidentReport a6 = accident(r2, AccidentType.FALL, 45, "文三路口避让行人摔倒", "文三路线",
                "ORD20260808002", "装备完好", null, "手腕扭伤", null, Weather.FOGGY);
        accidentService.review(m1, a6.getId(), true, true, false, null, false, true, "自行摔倒，无违规", "APPROVED");

        // 7. 张伟 60 天前摔倒（已结案）→ 构成配送压力信号
        AccidentReport a7 = accident(r1, AccidentType.FALL, 60, "学院路雨天路滑摔倒", "文三路线",
                "ORD20260725007", "雨衣湿滑视线受阻", null, "手肘擦伤", null, Weather.RAINY);
        accidentService.review(m1, a7.getId(), true, true, false, null, false, true, "雨天路滑，无违规", "APPROVED");

        // ---- 培训记录 ----
        trainingRepo.save(new TrainingRecord(r1, "雨天骑行安全培训", TrainingCategory.SAFETY, LocalDateTime.now().minusMonths(13), 85));
        trainingRepo.save(new TrainingRecord(r2, "新骑手安全培训", TrainingCategory.SAFETY, LocalDateTime.now().minusMonths(2), 92));
        trainingRepo.save(new TrainingRecord(r2, "装备正确使用与保养", TrainingCategory.EQUIPMENT_USE, LocalDateTime.now().minusMonths(2), 88));
        trainingRepo.save(new TrainingRecord(r3, "食品安全事故预防", TrainingCategory.SAFETY, LocalDateTime.now().minusMonths(4), 90));
        trainingRepo.save(new TrainingRecord(r3, "保温箱清洁与餐品安全", TrainingCategory.FOOD_SAFETY, LocalDateTime.now().minusMonths(4), 95));

        // ---- 考核（额外手工加分） ----
        assessmentRepo.save(new RiderAssessment(r1, null, 5, "季度安全之星奖励"));

        // ---- 站点规则调整 ----
        policyRepo.save(new PolicyAdjustment(s1, PolicyType.PROCUREMENT, "雨季装备加量采购",
                "雨季雨衣、头盔损耗集中，9 月起采购量上调 30%，安全库存提高到 20 件", "陈明"));
        policyRepo.save(new PolicyAdjustment(s1, PolicyType.TRAINING, "高风险路段强化培训",
                "文三路线近季度事故 3 起，每周增加一次雨天骑行安全培训", "陈明"));
        policyRepo.save(new PolicyAdjustment(s2, PolicyType.REVIEW_RULE, "事故材料完整性核查",
                "保险材料缺失的理赔单一律暂缓结案，骑手须 3 日内补齐交警记录与照片", "刘芳"));
    }

    private User rider(String username, String name, Station station) {
        return userRepo.save(new User(username, passwordEncoder.encode("rider123"), name, Role.RIDER, station));
    }

    private EquipmentType type(String code, String name, String deposit, int cycle, String sizes, String desc) {
        return typeRepo.save(new EquipmentType(code, name, new BigDecimal(deposit), cycle, sizes, desc));
    }

    private void seedStock(Station s, EquipmentType[] types, int[][] qty) {
        for (int i = 0; i < types.length; i++) {
            String[] sizes = types[i].getSizes().split(",");
            for (int j = 0; j < sizes.length; j++) {
                stockRepo.save(new EquipmentStock(s, types[i], sizes[j], qty[i][j]));
            }
        }
    }

    private EquipmentIssue issue(User rider, Station station, EquipmentType type, String size, int monthsAgo) {
        EquipmentIssue i = new EquipmentIssue();
        i.setRider(rider);
        i.setStation(station);
        i.setEquipmentType(type);
        i.setSize(size);
        i.setIssuedAt(LocalDateTime.now().minusMonths(monthsAgo));
        i.setDepositPaid(type.getDeposit());
        i.setStatus(IssueStatus.IN_USE);
        i.setExpectedReplaceAt(i.getIssuedAt().toLocalDate().plusMonths(type.getReplacementCycleMonths()));
        return issueRepo.save(i);
    }

    private AccidentReport accident(User rider, AccidentType type, int daysAgo, String location, String route,
                                    String orderNo, String equipDesc, EquipmentType damaged, String injury,
                                    String police, Weather weather) {
        AccidentReport a = new AccidentReport();
        a.setRider(rider);
        a.setType(type);
        a.setOccurredAt(LocalDateTime.now().minusDays(daysAgo));
        a.setLocation(location);
        a.setRouteArea(route);
        a.setOrderNo(orderNo);
        a.setEquipmentStatusDesc(equipDesc);
        a.setDamagedEquipmentType(damaged);
        a.setInjuryDesc(injury);
        a.setPoliceRecordNo(police);
        a.setWeather(weather);
        a.setStatus(AccidentStatus.PENDING);
        return accidentRepo.save(a);
    }
}
