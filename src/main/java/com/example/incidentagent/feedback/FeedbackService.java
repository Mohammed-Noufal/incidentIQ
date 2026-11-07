package com.example.incidentagent.feedback;

import com.example.incidentagent.common.exception.ApiException;
import com.example.incidentagent.common.exception.ErrorCode;
import com.example.incidentagent.feedback.dto.FeedbackRequest;
import com.example.incidentagent.feedback.dto.FeedbackResponse;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public FeedbackResponse submitFeedback(FeedbackRequest request) {
        // Honeypot spam check: if website field is filled, silently discard spam
        if (request.website() != null && !request.website().isBlank()) {
            log.warn("Spam feedback submission detected from email {}", request.email());
            return new FeedbackResponse(-1L, request.name(), request.rating(), Instant.now(), "SUBMITTED");
        }

        UserFeedback entity = new UserFeedback(
                request.name().trim(),
                request.email().trim().toLowerCase(java.util.Locale.ROOT),
                request.rating(),
                request.message().trim()
        );

        entity = feedbackRepository.save(entity);
        log.info("Received user feedback ID {} with rating {}", entity.getId(), entity.getRating());

        return new FeedbackResponse(
                entity.getId(),
                entity.getName(),
                entity.getRating(),
                entity.getCreatedAt(),
                "SUBMITTED"
        );
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> listFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(f -> new FeedbackResponse(f.getId(), f.getName(), f.getRating(), f.getCreatedAt(), "SUBMITTED"))
                .toList();
    }
}

