package ma.emsi.userservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.repository.RoleRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void initRoles() {
        for (RoleName rn : RoleName.values()) {
            if (roleRepository.findByName(rn).isEmpty()) {
                roleRepository.save(new Role(null, rn));
            }
        }
    }
}
