package ma.emsi.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.enums.AccountStatus;
import ma.emsi.userservice.repository.RoleRepository;
import ma.emsi.userservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Initialise les données de base au démarrage de l'application.
 * 
 * Logique métier des rôles :
 * - ADMIN : Créé au démarrage, gère les utilisateurs et les campagnes
 * - DIRECTEUR : Créé par l'Admin, valide les inscriptions de ses doctorants
 * - DOCTORANT : S'inscrit lui-même via le formulaire public
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData() {
        return args -> {
            log.info("🚀 Initialisation des données de base...");

            // 1. Créer les rôles s'ils n'existent pas
            Role roleAdmin = createRoleIfNotExists(RoleName.ROLE_ADMIN);
            Role roleDirecteur = createRoleIfNotExists(RoleName.ROLE_DIRECTEUR);
            Role roleDoctorant = createRoleIfNotExists(RoleName.ROLE_DOCTORANT);

            // 2. Créer le compte Admin par défaut s'il n'existe pas
            createAdminIfNotExists(roleAdmin);

            // 3. Créer des comptes de test
            createTestAccounts(roleDirecteur, roleDoctorant);

            log.info("✅ Initialisation des données terminée");
        };
    }

    private Role createRoleIfNotExists(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    log.info("📝 Création du rôle: {}", roleName);
                    Role role = new Role();
                    role.setName(roleName);
                    return roleRepository.save(role);
                });
    }

    private void createAdminIfNotExists(Role roleAdmin) {
        String adminEmail = "admin@emsi.ma";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("👤 Création du compte Admin par défaut: {}", adminEmail);

            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin@2025!"));
            admin.setFirstName("Administrateur");
            admin.setLastName("Système");
            admin.setPhoneNumber("0600000000");
            admin.setAdresse("EMSI Casablanca");
            admin.setVille("Casablanca");
            admin.setPays("Maroc");
            admin.setEnabled(true);
            admin.setAccountStatus(AccountStatus.ACTIVE);
            admin.setRoles(Set.of(roleAdmin));

            userRepository.save(admin);
            log.info("✅ Compte Admin créé avec succès");
        } else {
            log.info("ℹ️ Compte Admin existe déjà");
        }
    }

    private void createTestAccounts(Role roleDirecteur, Role roleDoctorant) {
        // Compte Directeur de test
        createUserIfNotExists(
                "directeur@emsi.ma",
                "Directeur@2025!",
                "Mohammed",
                "Alami",
                "0611111111",
                roleDirecteur,
                "Directeur");

        // Compte Doctorant de test
        createUserIfNotExists(
                "doctorant@emsi.ma",
                "Doctorant@2025!",
                "Ahmed",
                "Benali",
                "0622222222",
                roleDoctorant,
                "Doctorant");
    }

    private void createUserIfNotExists(String email, String password, String firstName,
            String lastName, String phone, Role role, String roleLabel) {
        if (userRepository.findByEmail(email).isEmpty()) {
            log.info("👤 Création du compte {} de test: {}", roleLabel, email);

            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhoneNumber(phone);
            user.setAdresse("Casablanca");
            user.setVille("Casablanca");
            user.setPays("Maroc");
            user.setEnabled(true);
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setRoles(Set.of(role));

            userRepository.save(user);
            log.info("✅ Compte {} créé", roleLabel);
        }
    }
}
