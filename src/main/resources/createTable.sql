CREATE TABLE article_links
(
    id           UUID PRIMARY KEY,
    team_name    TEXT NOT NULL,
    article_url  TEXT NOT NULL UNIQUE,
    article_date DATE NOT NULL,
    parsed       BOOLEAN DEFAULT false
);

CREATE TABLE articles
(
    id             VARCHAR(64) PRIMARY KEY, -- hash(article_url)
    team_name      VARCHAR(255) NOT NULL,
    article_url    TEXT         NOT NULL,
    published_date DATE         NOT NULL,
    title          TEXT         NOT NULL,
    author         TEXT,
    content        TEXT         NOT NULL,
    image_url      TEXT,
    links          JSONB,
    created_at     TIMESTAMP DEFAULT now()
);