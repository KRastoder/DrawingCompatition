CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE drawings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    description TEXT,
    image_data BYTEA NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_drawings_created_at ON drawings (created_at);
CREATE INDEX idx_drawings_user_id ON drawings (user_id);

CREATE TABLE votes (
    id BIGSERIAL PRIMARY KEY,
    drawing_id BIGINT NOT NULL REFERENCES drawings (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_votes_drawing_user UNIQUE (drawing_id, user_id)
);

CREATE INDEX idx_votes_drawing_id ON votes (drawing_id);
CREATE INDEX idx_votes_user_id ON votes (user_id);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    drawing_id BIGINT NOT NULL REFERENCES drawings (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_drawing_id ON comments (drawing_id);
CREATE INDEX idx_comments_user_id ON comments (user_id);
