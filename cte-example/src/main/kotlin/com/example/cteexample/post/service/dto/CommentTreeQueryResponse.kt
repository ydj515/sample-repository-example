package com.example.cteexample.post.service.dto

data class CommentTreeQueryResponse(
    val limit: Int,
    val treeCount: Int,
    val durationMillis: Long,
    val trees: List<CommentTreeNode>
)
