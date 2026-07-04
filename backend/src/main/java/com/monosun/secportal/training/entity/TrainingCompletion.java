package com.monosun.secportal.training.entity;

import com.monosun.secportal.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_completions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "user_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private TrainingCourse course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer score;
    private Boolean passed;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime completedAt = LocalDateTime.now();
}
