package com.example.cteexample.post.service

import com.example.cteexample.post.domain.PostComment
import com.example.cteexample.post.repository.PostCommentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostCommentServiceTest @Autowired constructor(
    private val postCommentService: PostCommentService,
    private val postCommentRepository: PostCommentRepository
) {

    private val baseDateTime: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)

    @BeforeEach
    fun setUp() {
//        postCommentRepository.deleteAll()
//        insertSampleComments()
    }

    @Test
    fun `test1 cte query`() {
        val postId = 1L

        val cteResults = postCommentService.findTopCommentTreesWithCte(postId)
    }

    @Test
    fun `test1 aggregation logic return identical top trees`() {
        val postId = 1L

        val aggregatedResults = postCommentService.findTopCommentTreesWithAggregation(postId)
    }

    @Test
    fun `test2 cte query limit argument restricts number of top trees`() {
        val postId = 1L

        val cteResults = postCommentService.findTopCommentTreesWithCte(postId, limit = 2)
    }

    @Test
    fun `test2 aggregation logic return identical top trees limit argument`() {
        val postId = 1L

        val aggregatedResults = postCommentService.findTopCommentTreesWithAggregation(postId, limit = 2)
    }


    private fun insertSampleComments() {
        val comments = listOf(
            // Post 1 hierarchy
            comment(id = 1, parentId = null, score = 5, postId = 1),
            comment(id = 2, parentId = 1, score = 3, postId = 1),
            comment(id = 3, parentId = 1, score = 4, postId = 1),
            comment(id = 4, parentId = 2, score = 2, postId = 1),
            comment(id = 10, parentId = null, score = 8, postId = 1),
            comment(id = 11, parentId = 10, score = 6, postId = 1),
            comment(id = 12, parentId = 11, score = 1, postId = 1),
            comment(id = 20, parentId = null, score = 20, postId = 1),
            comment(id = 30, parentId = null, score = 2, postId = 1),
            comment(id = 31, parentId = 30, score = 1, postId = 1),

            // Another post to ensure filtering
            comment(id = 100, parentId = null, score = 50, postId = 2)
        )

        postCommentRepository.saveAll(comments)
    }

    private fun comment(id: Long, parentId: Long?, score: Int, postId: Long): PostComment =
        PostComment(
            id = id,
            parentId = parentId,
            review = "comment-$id",
            createdOn = baseDateTime.plusMinutes(id),
            score = score,
            postId = postId
        )
}
