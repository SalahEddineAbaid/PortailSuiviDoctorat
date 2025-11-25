package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.emsi.defenseservice.enums.JuryStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "juries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Jury {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long directorId;

    @Enumerated(EnumType.STRING)
    private JuryStatus status;

    private LocalDateTime proposalDate;
    private LocalDateTime validationDate;

    @OneToOne
    @JoinColumn(name = "defense_request_id")
    private DefenseRequest defenseRequest;

    @OneToMany(mappedBy = "jury", cascade = CascadeType.ALL)
    private List<JuryMember> members = new ArrayList<>();
}
