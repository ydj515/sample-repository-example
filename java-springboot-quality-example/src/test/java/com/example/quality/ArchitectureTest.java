package com.example.quality;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.junit.jupiter.api.Tag;

@AnalyzeClasses(
    packages = "com.example.quality",
    importOptions = ImportOption.DoNotIncludeTests.class)
@Tag("architecture")
class ArchitectureTest {
  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule domain_does_not_depend_on_presentation =
      noClasses()
          .that()
          .resideInAnyPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..presentation..",
              "..application..",
              "com.example.quality.repository..",
              "org.springframework..");

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule application_does_not_depend_on_presentation =
      noClasses()
          .that()
          .resideInAnyPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..presentation..", "com.example.quality.repository..");

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule presentation_does_not_access_repositories =
      noClasses()
          .that()
          .resideInAPackage("..presentation..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.example.quality.repository..");

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule presentation_does_not_access_domain_repository =
      noClasses()
          .that()
          .resideInAPackage("..presentation..")
          .should()
          .dependOnClassesThat()
          .haveSimpleName("TodoRepository");

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule repository_does_not_depend_on_upper_layers =
      noClasses()
          .that()
          .resideInAPackage("com.example.quality.repository..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..application..", "..presentation..");

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule domain_repository_is_interface =
      classes()
          .that()
          .resideInAPackage("..domain..")
          .and()
          .haveSimpleNameEndingWith("Repository")
          .should()
          .beInterfaces();

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule jpa_repository_belongs_to_repository_layer =
      classes()
          .that()
          .areAssignableTo(org.springframework.data.jpa.repository.JpaRepository.class)
          .should()
          .resideInAPackage("com.example.quality.repository..");

  @ArchTest
  static final com.tngtech.archunit.lang.ArchRule layers_are_free_of_cycles =
      slices().matching("com.example.quality.(*)..").should().beFreeOfCycles();
}
