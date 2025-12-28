package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "defense_requests", indexes = {
        @Index(name = "idx_doctorant_id", columnList = "doctorant_id"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefenseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long doctorantId;

    private LocalDateTime submissionDate;

    @Enumerated(EnumType.STRING)
    private DefenseRequestStatus status;

    private String rejectionReason;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "prerequisites_id")
    private Prerequisites prerequisites;

    @OneToMany(mappedBy = "defenseRequest", cascade = CascadeType.ALL)
    private List<Document> documents = new ArrayList<>();

    @OneToOne(mappedBy = "defenseRequest", cascade = CascadeType.ALL)
    private Jury jury;

    @OneToOne(mappedBy = "defenseRequest", cascade = CascadeType.ALL)
    private Defense defense;

    @OneToMany(mappedBy = "defenseRequest", cascade = CascadeType.ALL)
    private List<Rapport> rapports = new ArrayList<>();
}
