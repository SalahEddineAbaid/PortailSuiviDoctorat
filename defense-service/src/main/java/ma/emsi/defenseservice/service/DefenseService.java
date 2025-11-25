package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.entity.Defense;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.repository.DefenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefenseService {

    @Autowired
    private DefenseRepository defenseRepository;

    public Defense schedule(Defense d) {
        return defenseRepository.save(d);
    }

    public Defense getByDefenseRequest(Long requestId) {
        return defenseRepository.findByDefenseRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Defense not found"));
    }

    public Defense updateStatus(Long id, DefenseStatus status) {
        Defense defense = defenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Defense not found"));

        defense.setStatus(status);
        return defenseRepository.save(defense);
    }
}

