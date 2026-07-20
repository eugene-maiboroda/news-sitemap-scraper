-- Run these statements one by one, outside an explicit transaction.
-- CONCURRENTLY avoids blocking the ongoing metadata extraction and normal reads/writes, but index
-- creation still consumes I/O. Prefer running it after the current backfill has completed.

-- Category export filters by category and paginates in id order.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_articles_category_id
    ON articles (category, id);

-- Topic membership always includes main_category and returns rows in id order.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_articles_main_category_id_extracted
    ON articles (main_category, id)
    WHERE topics_extracted = TRUE;

-- The topic membership query uses `topics @> [{"url": "..."}]` as an indexed pre-filter,
-- followed by exact positional and hierarchy checks for correctness.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_articles_topics_gin
    ON articles USING GIN (topics jsonb_path_ops)
    WHERE topics_extracted = TRUE;

-- Tags export currently performs one ordered table scan and classifies in Java. This index is for
-- the next optimization step where required tags are filtered with JSONB containment in SQL.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_articles_tags_gin
    ON articles USING GIN (tags jsonb_path_ops)
    WHERE topics_extracted = TRUE;

-- Keeps the extraction worker's "next pending rows" query cheap while future re-extractions run.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_articles_metadata_pending
    ON articles (id)
    WHERE topics_extracted = FALSE AND raw_html IS NOT NULL;

-- Refresh planner statistics after the backfill and index builds.
ANALYZE articles;
