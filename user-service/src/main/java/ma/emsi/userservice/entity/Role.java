package ma.emsi.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@Table(name = "roles")
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) // stocke la valeur texte de l'enum (ex: "ROLE_ADMIN")
    @Column(nullable = false, unique = true)
    private RoleName name;

    public Role() {

    }


    // Getters & Setters
}
