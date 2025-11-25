package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.emsi.defenseservice.enums.MemberRole;
import ma.emsi.defenseservice.enums.MemberStatus;

@Entity
@Table(name = "jury_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JuryMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String affiliation;
    private String grade;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @ManyToOne
    @JoinColumn(name = "jury_id")
    private Jury jury;
}
