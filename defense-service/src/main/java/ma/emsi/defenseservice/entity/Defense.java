package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.enums.Mention;

import java.time.LocalDateTime;

@Entity
@Table(name = "defenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Defense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime defenseDate;
    private String location;
    private String room;

    @Enumerated(EnumType.STRING)
    private DefenseStatus status;

    private String procesVerbalUrl;

    @Enumerated(EnumType.STRING)
    private Mention mention;

    private boolean publicationRecommended;

    private String juryComments;

    private LocalDateTime deliberationDate;

    @OneToOne
    @JoinColumn(name = "defense_request_id")
    private DefenseRequest defenseRequest;
}
