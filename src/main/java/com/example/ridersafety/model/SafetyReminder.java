package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 骑手安全提醒（雨季未领取雨衣等） */
@Entity
@Table(name = "safety_reminders")
@Getter
@Setter
@NoArgsConstructor
public class SafetyReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private RainPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderType type = ReminderType.GENERAL;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean readFlag = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public SafetyReminder(User rider, RainPlan plan, ReminderType type, String title, String content) {
        this.rider = rider;
        this.plan = plan;
        this.type = type;
        this.title = title;
        this.content = content;
    }
}
