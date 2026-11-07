CREATE TABLE user_feedback (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(254) NOT NULL,
    rating     INT          NOT NULL,
    message    TEXT         NOT NULL,
    created_at DATETIME(6)  NOT NULL
);

