package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.emsi.defenseservice.enums.MemberRole;
import ma.emsi.defenseservice.enums.MemberStatus;

@Entity
@Table(name = "jury_members", indexes = {
        @Index(name = "idx_professor_id", columnList = "professor_id"),
        @Index(name = "idx_jury_id", columnList = "jury_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JuryMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professor_id", nullable = false)
    private Long professorId; // ✅ Référence au user-service (plus de duplication)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @ManyToOne
    @JoinColumn(name = "jury_id")
    private Jury jury;
}
