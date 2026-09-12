package com.example.ridersafety.service;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.*;
import com.example.ridersafety.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentTypeRepository typeRepo;
    private final EquipmentStockRepository stockRepo;
    private final EquipmentIssueRepository issueRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> types() {
        return typeRepo.findAll().stream().map(DtoMapper::type).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> stockFor(Long stationId) {
        return stockRepo.findByStation_Id(stationId).stream().map(DtoMapper::stock).toList();
    }

    @Transactional
    public Map<String, Object> adjustStock(Long stockId, int delta) {
        EquipmentStock s = stockRepo.findById(stockId)
                .orElseThrow(() -> ApiException.notFound("库存记录不存在"));
        int next = s.getQuantity() + delta;
        if (next < 0) {
            throw ApiException.badRequest("库存不能为负数");
        }
        s.setQuantity(next);
        return DtoMapper.stock(stockRepo.save(s));
    }

    /** 站长给骑手发放装备：扣减库存 + 生成领用记录（含更换周期到期日） */
    @Transactional
    public Map<String, Object> issue(Station station, Long riderId, Long typeId, String size, String notes) {
        User rider = userRepo.findById(riderId)
                .orElseThrow(() -> ApiException.notFound("骑手不存在"));
        if (rider.getRole() != Role.RIDER) {
            throw ApiException.badRequest("目标用户不是骑手");
        }
        if (rider.getStation() == null || !rider.getStation().getId().equals(station.getId())) {
            throw ApiException.badRequest("该骑手不属于本站点");
        }
        EquipmentType type = typeRepo.findById(typeId)
                .orElseThrow(() -> ApiException.notFound("装备类型不存在"));
        if (size == null || size.isBlank()) {
            throw ApiException.badRequest("请选择尺码");
        }
        EquipmentStock stock = stockRepo
                .findByStation_IdAndEquipmentType_IdAndSize(station.getId(), typeId, size)
                .orElseThrow(() -> ApiException.badRequest("本站点没有该尺码的库存记录"));
        if (stock.getQuantity() <= 0) {
            throw ApiException.badRequest("库存不足，请先采购补货");
        }
        stock.setQuantity(stock.getQuantity() - 1);
        stockRepo.save(stock);

        EquipmentIssue issue = new EquipmentIssue();
        issue.setRider(rider);
        issue.setStation(station);
        issue.setEquipmentType(type);
        issue.setSize(size);
        issue.setIssuedAt(LocalDateTime.now());
        issue.setDepositPaid(type.getDeposit());
        issue.setStatus(IssueStatus.IN_USE);
        issue.setExpectedReplaceAt(LocalDate.now().plusMonths(type.getReplacementCycleMonths()));
        issue.setNotes(notes);
        return DtoMapper.issue(issueRepo.save(issue));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> issuesForStation(Long stationId, boolean expiredOnly) {
        return issueRepo.findByStation_IdOrderByIssuedAtDesc(stationId).stream()
                .map(DtoMapper::issue)
                .filter(m -> !expiredOnly || Boolean.TRUE.equals(m.get("expired")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myIssues(User rider) {
        return issueRepo.findByRider_IdOrderByIssuedAtDesc(rider.getId()).stream()
                .map(DtoMapper::issue).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ridersOfStation(Long stationId) {
        return userRepo.findByRoleAndStation_Id(Role.RIDER, stationId).stream()
                .map(DtoMapper::user).toList();
    }
}
