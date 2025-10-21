package com.example.cteexample.post.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class PostCommentRepositoryImpl(
    @PersistenceContext
    private val entityManager: EntityManager
) : PostCommentRepositoryCustom {

    override fun findTopCommentTreesUsingCte(postId: Long, maxRanking: Int): List<PostCommentCteRow> {
        val query = entityManager.createNativeQuery(CTE_QUERY)
        query.setParameter("postId", postId)
        query.setParameter("maxRanking", maxRanking)

        val results = query.resultList
        if (results.isEmpty()) {
            return emptyList()
        }

        return results.map { raw ->
            val columns = when (raw) {
                is Array<*> -> raw
                is List<*> -> raw.toTypedArray()
                else -> throw IllegalStateException("Unexpected query result type: ${raw?.javaClass?.name}")
            }
            PostCommentCteRow(
                id = (columns[0] as Number).toLong(),
                parentId = (columns[1] as Number?)?.toLong(),
                review = columns[2] as String,
                createdOn = toLocalDateTime(columns[3]),
                score = (columns[4] as Number).toInt(),
                totalScore = (columns[5] as Number).toInt(),
                rootId = (columns[6] as Number).toLong(),
                ranking = (columns[7] as Number).toInt()
            )
        }
    }

    private fun toLocalDateTime(value: Any?): LocalDateTime {
        return when (value) {
            null -> throw IllegalArgumentException("created_on column cannot be null")
            is Timestamp -> value.toLocalDateTime()
            is LocalDateTime -> value
            else -> throw IllegalArgumentException("Unsupported datetime type: ${value.javaClass.name}")
        }
    }

    companion object {
        private const val CTE_QUERY = """
            WITH RECURSIVE
            post_comment_score (id, root_id, post_id, parent_id, review, created_on, score) AS (
                SELECT id, id AS root_id, post_id, parent_id, review, created_on, score
                FROM post_comment
                WHERE post_id = :postId AND parent_id IS NULL
                
                UNION ALL
                
                SELECT pc.id, pcs.root_id, pc.post_id, pc.parent_id, pc.review, pc.created_on, pc.score
                FROM post_comment pc
                JOIN post_comment_score pcs ON pc.parent_id = pcs.id
            ),
            total_score_comment (id, parent_id, review, created_on, score, root_id, total_score) AS (
                SELECT
                    id, parent_id, review, created_on, score, root_id,
                    SUM(score) OVER (PARTITION BY root_id) AS total_score
                FROM post_comment_score
            ),
            total_score_ranking (id, parent_id, review, created_on, score, total_score, root_id, ranking) AS (
                SELECT
                    id, parent_id, review, created_on, score, total_score, root_id,
                    DENSE_RANK() OVER (ORDER BY total_score DESC) AS ranking
                FROM total_score_comment
            )
            SELECT id, parent_id, review, created_on, score, total_score, root_id, ranking
            FROM total_score_ranking
            WHERE ranking <= :maxRanking
            ORDER BY total_score DESC, id ASC
        """
    }
}
