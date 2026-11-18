-- Bootstrap for an empty PostgreSQL database. Run with psql -v ON_ERROR_STOP=1.
BEGIN;

CREATE TABLE article_links (
    id TEXT PRIMARY KEY,
    article_url TEXT NOT NULL UNIQUE,
    article_date DATE,
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    error_message TEXT,
    discovery_title TEXT,
    discovery_description TEXT,
    menu_paths JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE articles (
    id TEXT PRIMARY KEY,
    article_url TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    subtitles TEXT,
    published_date DATE,
    author TEXT,
    author_link TEXT,
    content TEXT NOT NULL DEFAULT '',
    raw_html TEXT,
    affiliate_disclosure TEXT,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    footer_tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    related_stories JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE image (
    id TEXT PRIMARY KEY,
    article_id TEXT NOT NULL REFERENCES articles (id),
    image_url TEXT NOT NULL,
    caption TEXT,
    position INTEGER NOT NULL,
    download_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    local_file_path TEXT,
    file_size BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE videos (
    id TEXT PRIMARY KEY,
    article_id TEXT NOT NULL REFERENCES articles (id),
    source_url TEXT NOT NULL,
    position INTEGER NOT NULL,
    platform TEXT,
    youtube_video_id TEXT,
    download_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    local_file_path TEXT,
    file_size BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX image_article_position_idx ON image (article_id, position);
CREATE INDEX videos_article_position_idx ON videos (article_id, position);
CREATE INDEX articles_export_order_idx ON articles (published_date DESC NULLS LAST, article_url);

COMMIT;
