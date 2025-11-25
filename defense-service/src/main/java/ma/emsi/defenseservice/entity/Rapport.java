package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rapports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rapport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reportUrl;
    private boolean favorable;
    private LocalDateTime submissionDate;

    @ManyToOne
    @JoinColumn(name = "defense_request_id")
    private DefenseRequest defenseRequest;

    @ManyToOne
    @JoinColumn(name = "jury_member_id")
    private JuryMember juryMember;
}
