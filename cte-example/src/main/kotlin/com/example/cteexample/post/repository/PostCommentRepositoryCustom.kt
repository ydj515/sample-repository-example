package com.example.cteexample.post.repository

import java.time.LocalDateTime

interface PostCommentRepositoryCustom {
    fun findTopCommentTreesUsingCte(postId: Long, maxRanking: Int): List<PostCommentCteRow>
}

data class PostCommentCteRow(
    val id: Long,
    val parentId: Long?,
    val review: String,
    val createdOn: LocalDateTime,
    val score: Int,
    val totalScore: Int,
    val rootId: Long,
    val ranking: Int
)
