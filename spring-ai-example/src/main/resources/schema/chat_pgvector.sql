CREATE EXTENSION  IF NOT EXISTS  vector;

DROP TABLE IF EXISTS chat_pgvector CASCADE;
CREATE TABLE IF NOT EXISTS public.chat_pgvector (
    id VARCHAR(255) PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1536)
);
