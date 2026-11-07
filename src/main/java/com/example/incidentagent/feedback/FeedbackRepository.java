package com.example.incidentagent.feedback;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<UserFeedback, Long> {
    List<UserFeedback> findAllByOrderByCreatedAtDesc();
}

