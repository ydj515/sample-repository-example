package com.example.cteexample.post.web

import com.example.cteexample.post.service.PostCommentService
import com.example.cteexample.post.service.dto.CommentTreeQueryResponse
import com.example.cteexample.post.service.dto.CommentTreeNode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

@RestController
@RequestMapping("/api/posts/{postId}/comments")
class PostCommentController(
    private val postCommentService: PostCommentService
) {

    @GetMapping("/top-trees/cte")
    fun getTopCommentTreesWithCte(
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "3") limit: Int
    ): CommentTreeQueryResponse {
        lateinit var trees: List<CommentTreeNode>
        val durationMillis = TimeUnit.NANOSECONDS.toMillis(
            measureNanoTime {
                trees = postCommentService.findTopCommentTreesWithCte(postId = postId, limit = limit)
            }
        )
        return CommentTreeQueryResponse(
            limit = limit,
            treeCount = trees.size,
            durationMillis = durationMillis,
            trees = trees
        )
    }

    @GetMapping("/top-trees/aggregation")
    fun getTopCommentTreesWithAggregation(
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "3") limit: Int
    ): CommentTreeQueryResponse {
        lateinit var trees: List<CommentTreeNode>
        val durationMillis = TimeUnit.NANOSECONDS.toMillis(
            measureNanoTime {
                trees = postCommentService.findTopCommentTreesWithAggregation(postId = postId, limit = limit)
            }
        )
        return CommentTreeQueryResponse(
            limit = limit,
            treeCount = trees.size,
            durationMillis = durationMillis,
            trees = trees
        )
    }
}
