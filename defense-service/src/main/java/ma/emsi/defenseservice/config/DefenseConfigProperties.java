package ma.emsi.defenseservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for defense service validation rules
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
@Configuration
@ConfigurationProperties(prefix = "defense")
public class DefenseConfigProperties {

    private final Prerequis prerequis = new Prerequis();
    private final Jury jury = new Jury();

    public Prerequis getPrerequis() {
        return prerequis;
    }

    public Jury getJury() {
        return jury;
    }

    /**
     * Prerequisites configuration
     */
    public static class Prerequis {
        /**
         * Minimum required Q1/Q2 publications
         * Requirement 8.1
         */
        private int minPublicationsQ1Q2 = 2;

        /**
         * Minimum required conferences
         * Requirement 8.2
         */
        private int minConferences = 2;

        /**
         * Minimum required training hours
         * Requirement 8.3
         */
        private int minHeuresFormation = 200;

        public int getMinPublicationsQ1Q2() {
            return minPublicationsQ1Q2;
        }

        public void setMinPublicationsQ1Q2(int minPublicationsQ1Q2) {
            this.minPublicationsQ1Q2 = minPublicationsQ1Q2;
        }

        public int getMinConferences() {
            return minConferences;
        }

        public void setMinConferences(int minConferences) {
            this.minConferences = minConferences;
        }

        public int getMinHeuresFormation() {
            return minHeuresFormation;
        }

        public void setMinHeuresFormation(int minHeuresFormation) {
            this.minHeuresFormation = minHeuresFormation;
        }
    }

    /**
     * Jury composition configuration
     */
    public static class Jury {
        /**
         * Minimum jury members
         * Requirement 8.4
         */
        private int minMembres = 4;

        /**
         * Minimum rapporteurs
         * Requirement 8.5
         */
        private int minRapporteurs = 2;

        public int getMinMembres() {
            return minMembres;
        }

        public void setMinMembres(int minMembres) {
            this.minMembres = minMembres;
        }

        public int getMinRapporteurs() {
            return minRapporteurs;
        }

        public void setMinRapporteurs(int minRapporteurs) {
            this.minRapporteurs = minRapporteurs;
        }
    }
}
