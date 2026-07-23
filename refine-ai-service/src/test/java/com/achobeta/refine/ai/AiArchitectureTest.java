package com.achobeta.refine.ai;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.achobeta.refine.ai")
class AiArchitectureTest {
    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule services_do_not_depend_on_infrastructure = noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule controllers_do_not_depend_on_mappers = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper");

    @ArchTest
    static final ArchRule controllers_do_not_call_external_ports_directly = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAPackage("..application.port..");

    @ArchTest
    static final ArchRule use_case_services_reside_in_application_packages = classes()
            .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
            .should().resideInAPackage("..application..");
}
