package com.example.cteexample.post.service.dto

import java.time.LocalDateTime

data class CommentTreeNode(
    val id: Long,
    val parentId: Long?,
    val review: String,
    val createdOn: LocalDateTime,
    val score: Int,
    val totalScore: Int,
    val children: List<CommentTreeNode>
)
