package com.monosun.secportal.committee.entity;

import com.monosun.secportal.auth.entity.User;
import com.monosun.secportal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "committee_meetings",
        indexes = {
                @Index(name = "idx_committee_year", columnList = "year"),
                @Index(name = "idx_committee_year_session", columnList = "year,session_no", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitteeMeeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(name = "session_no", nullable = false)
    private int sessionNo;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "meeting_date")
    private LocalDate meetingDate;

    @Column(length = 300)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String attendees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PLANNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<CommitteeFile> files = new ArrayList<>();

    public enum Status { PLANNED, COMPLETED, CANCELLED }
}
