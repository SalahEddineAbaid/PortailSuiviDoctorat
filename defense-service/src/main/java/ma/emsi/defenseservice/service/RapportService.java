package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.entity.Rapport;
import ma.emsi.defenseservice.repository.RapportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RapportService {

    @Autowired
    private RapportRepository rapportRepository;

    public Rapport submit(Rapport r) {
        r.setSubmissionDate(LocalDateTime.now());
        return rapportRepository.save(r);
    }

    public List<Rapport> getByDefenseRequest(Long defenseRequestId) {
        return rapportRepository.findByDefenseRequestId(defenseRequestId);
    }
}

