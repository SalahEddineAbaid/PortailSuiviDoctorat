package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.entity.Prerequisites;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.repository.PrerequisitesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PrerequisitesService {

    @Autowired
    private PrerequisitesRepository prerequisitesRepository;

    public Prerequisites save(Prerequisites p) {
        return prerequisitesRepository.save(p);
    }

    public Prerequisites getById(Long id) {
        return prerequisitesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prerequisites not found"));
    }

    public Prerequisites validate(Long id, boolean isValid) {
        Prerequisites pre = getById(id);
        pre.setValid(isValid);
        return prerequisitesRepository.save(pre);
    }
}

