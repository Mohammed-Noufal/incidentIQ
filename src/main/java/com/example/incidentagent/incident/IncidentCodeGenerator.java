package com.example.incidentagent.incident;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Generates sequential incident codes formatted as INC-%03d (e.g. INC-019).
 */
@Component
public class IncidentCodeGenerator {

    private static final Pattern CODE_PATTERN = Pattern.compile("INC-(\\d+)");

    private final IncidentRepository incidentRepository;

    public IncidentCodeGenerator(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public synchronized String nextCode() {
        List<Incident> all = incidentRepository.findAll();
        int max = 0;
        for (Incident inc : all) {
            if (inc.getCode() != null) {
                Matcher m = CODE_PATTERN.matcher(inc.getCode());
                if (m.find()) {
                    try {
                        int num = Integer.parseInt(m.group(1));
                        if (num > max) {
                            max = num;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return String.format("INC-%03d", max + 1);
    }
}

