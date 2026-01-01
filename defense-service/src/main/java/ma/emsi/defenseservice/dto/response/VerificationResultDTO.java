package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResultDTO {

    private boolean peutAutoriser;
    private Map<String, Boolean> verificationsDetaillees = new HashMap<>();
    private List<String> blocages = new ArrayList<>();

    public VerificationResultDTO(boolean peutAutoriser) {
        this.peutAutoriser = peutAutoriser;
    }

    public void addVerification(String key, boolean passed) {
        verificationsDetaillees.put(key, passed);
    }

    public void addBlocage(String message) {
        blocages.add(message);
    }
}
