package com.example.cteexample.post.service

import com.example.cteexample.post.domain.PostComment
import com.example.cteexample.post.repository.PostCommentCteRow
import com.example.cteexample.post.repository.PostCommentRepository
import com.example.cteexample.post.service.dto.CommentTreeNode
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PostCommentService(
    private val postCommentRepository: PostCommentRepository
) {

    fun findTopCommentTreesWithCte(postId: Long, limit: Int = DEFAULT_RANKING_LIMIT): List<CommentTreeNode> {
        require(limit > 0) { "limit must be greater than zero" }

        val rows = postCommentRepository.findTopCommentTreesUsingCte(postId = postId, maxRanking = limit)
        if (rows.isEmpty()) {
            return emptyList()
        }

        val nodeMap = rows.associate { row ->
            row.id to MutableCommentNode(
                id = row.id,
                parentId = row.parentId,
                review = row.review,
                createdOn = row.createdOn,
                score = row.score
            )
        }

        val rootCandidates = linkNodes(rows, nodeMap)
        rootCandidates.forEach { computeTotalScore(it) }

        return rootCandidates
            .sortedWith(compareByDescending<MutableCommentNode> { it.totalScore }.thenBy { it.id })
            .take(limit)
            .map { it.toDto() }
    }

    fun findTopCommentTreesWithAggregation(postId: Long, limit: Int = DEFAULT_RANKING_LIMIT): List<CommentTreeNode> {
        require(limit > 0) { "limit must be greater than zero" }

        val comments = postCommentRepository.findByPostId(postId)
        if (comments.isEmpty()) {
            return emptyList()
        }

        val nodeMap = comments.associate { comment ->
            comment.id to comment.toMutableNode()
        }

        val rootCandidates = linkNodes(comments, nodeMap)
        rootCandidates.forEach { computeTotalScore(it) }

        return rootCandidates
            .sortedWith(compareByDescending<MutableCommentNode> { it.totalScore }.thenBy { it.id })
            .take(limit)
            .map { it.toDto() }
    }

    private fun linkNodes(rows: List<*>, nodeMap: Map<Long, MutableCommentNode>): MutableList<MutableCommentNode> {
        val roots = mutableListOf<MutableCommentNode>()
        rows.forEach { element ->
            when (element) {
                is PostComment -> linkNode(nodeMap.getValue(element.id), element.parentId, nodeMap, roots)
                is PostCommentCteRow -> linkNode(nodeMap.getValue(element.id), element.parentId, nodeMap, roots)
                else -> throw IllegalArgumentException("Unsupported element type: ${element?.javaClass?.name}")
            }
        }
        return roots
    }

    private fun linkNode(
        node: MutableCommentNode,
        parentId: Long?,
        nodeMap: Map<Long, MutableCommentNode>,
        roots: MutableList<MutableCommentNode>
    ) {
        if (parentId != null) {
            val parentNode = nodeMap[parentId]
            if (parentNode != null) {
                parentNode.children += node
                return
            }
        }
        roots += node
    }

    private fun computeTotalScore(node: MutableCommentNode): Int {
        if (node.children.isEmpty()) {
            node.totalScore = node.score
            return node.totalScore
        }

        val childrenScore = node.children.sumOf { child -> computeTotalScore(child) }
        node.totalScore = node.score + childrenScore
        return node.totalScore
    }

    private fun MutableCommentNode.toDto(): CommentTreeNode =
        CommentTreeNode(
            id = id,
            parentId = parentId,
            review = review,
            createdOn = createdOn,
            score = score,
            totalScore = totalScore,
            children = children
                .sortedBy { it.id }
                .map { it.toDto() }
        )

    private fun PostComment.toMutableNode(): MutableCommentNode =
        MutableCommentNode(
            id = id,
            parentId = parentId,
            review = review,
            createdOn = createdOn,
            score = score
        )

    data class MutableCommentNode(
        val id: Long,
        val parentId: Long?,
        val review: String,
        val createdOn: LocalDateTime,
        val score: Int,
        var totalScore: Int = score,
        val children: MutableList<MutableCommentNode> = mutableListOf()
    )

    companion object {
        private const val DEFAULT_RANKING_LIMIT = 3
    }
}
