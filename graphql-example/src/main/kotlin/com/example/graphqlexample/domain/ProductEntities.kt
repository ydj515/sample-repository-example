package com.example.graphqlexample.domain

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType

enum class ProductType {
    ELECTRONICS,
    CLOTHING
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
abstract class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var price: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    var productType: ProductType = ProductType.ELECTRONICS
)

@Entity
@DiscriminatorValue("ELECTRONICS")
class Electronics(
    name: String = "",
    price: Int = 0,
    @Column(nullable = true)
    var warrantyPeriod: Int = 0
) : Product(
    name = name,
    price = price,
    productType = ProductType.ELECTRONICS
)

@Entity
@DiscriminatorValue("CLOTHING")
class Clothing(
    name: String = "",
    price: Int = 0,
    @Column(nullable = true)
    var size: String = ""
) : Product(
    name = name,
    price = price,
    productType = ProductType.CLOTHING
)
