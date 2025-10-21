package com.example.cteexample.post.repository

import com.example.cteexample.post.domain.PostComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostCommentRepository : JpaRepository<PostComment, Long>, PostCommentRepositoryCustom {
    @Query("select pc from PostComment pc where pc.postId = :postId")
    fun findByPostId(postId: Long): List<PostComment>
}
