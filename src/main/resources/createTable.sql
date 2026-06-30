CREATE TABLE article_links
(
    id            UUID PRIMARY KEY,
    article_url   TEXT NOT NULL UNIQUE,
    article_date  DATE NOT NULL,
    status        TEXT NOT NULL DEFAULT 'NEW',
    error_message TEXT
);

CREATE TABLE articles
(
    id             VARCHAR(64) PRIMARY KEY,
    category       VARCHAR(255),
    article_url    TEXT         NOT NULL,
    published_date DATE         NOT NULL,
    title          TEXT         NOT NULL,
    sub_title      TEXT,
    author         TEXT,
    author_link    TEXT,
    content        TEXT         NOT NULL,
    image_url      TEXT,
    caption        TEXT,
    created_at     TIMESTAMP DEFAULT now()
);
