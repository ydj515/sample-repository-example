package com.example.cteexample.post.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "post_comment")
class PostComment(
    @Id
    val id: Long,

    @Column(name = "parent_id")
    val parentId: Long?,

    val review: String,

    @Column(name = "created_on")
    val createdOn: LocalDateTime,

    val score: Int,

    @Column(name = "post_id")
    val postId: Long
)
