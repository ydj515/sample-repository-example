package com.example.quality

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class ArchitectureKonsistTest :
    FunSpec({
        val scope = Konsist.scopeFromProduction()

        test("all expected layers contain production sources") {
            scope.files.shouldNotBeEmpty()
            listOf("presentation", "application", "domain", "repository").forEach { layer ->
                scope.files.filter { it.packagee?.name?.startsWith("com.example.quality.$layer.") == true }.shouldNotBeEmpty()
            }
        }

        test("layers follow the allowed dependency directions") {
            scope.assertArchitecture {
                val presentation = Layer("Presentation", "com.example.quality.presentation..")
                val application = Layer("Application", "com.example.quality.application..")
                val domain = Layer("Domain", "com.example.quality.domain..")
                val repository = Layer("Repository", "com.example.quality.repository..")

                presentation.doesNotDependOn(repository)
                application.doesNotDependOn(presentation, repository)
                repository.doesNotDependOn(presentation, application)
                domain.dependsOnNothing()
            }
        }

        test("domain has no Spring dependency and presentation has no repository dependency") {
            scope.files.filter { it.packagee?.name?.startsWith("com.example.quality.domain.") == true }.forEach { file ->
                file.imports.any { it.name.startsWith("org.springframework.") } shouldBe false
            }
            scope.files.filter { it.packagee?.name?.startsWith("com.example.quality.presentation.") == true }.forEach { file ->
                file.imports.any { it.name.endsWith("Repository") } shouldBe false
            }
        }

        test("repository contracts are interfaces in their owned packages") {
            val repositories = scope.interfaces().filter { it.name.endsWith("Repository") }
            repositories.map { it.name }.toSet() shouldBe setOf("TodoRepository", "JpaTodoRepository")
            scope.classes().none { it.name.endsWith("Repository") } shouldBe true
            scope.files
                .single { it.name == "TodoRepository" }
                .packagee
                ?.name shouldBe "com.example.quality.domain.todo"
            scope.files
                .single { it.name == "JpaTodoRepository" }
                .packagee
                ?.name shouldBe "com.example.quality.repository.todo"
        }
    })
