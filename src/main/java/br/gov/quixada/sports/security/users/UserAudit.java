package br.gov.quixada.sports.security.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "db_user_audit")
public class UserAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false, length = 254)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserAuditAction action;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 254)
    private String targetEmail;

    @Column(columnDefinition = "text")
    private String changes;
}
