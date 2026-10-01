CREATE TABLE expenses (
                          id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id    VARCHAR(64) NOT NULL,
                          amount     INT         NOT NULL,
                          category   VARCHAR(32) NOT NULL,
                          memo       TEXT,
                          created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- UTC で保存
                          INDEX idx_user_created (user_id, created_at)
);