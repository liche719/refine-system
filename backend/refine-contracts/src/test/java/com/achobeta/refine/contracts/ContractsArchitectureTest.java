package com.achobeta.refine.contracts;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.achobeta.refine.contracts")
class ContractsArchitectureTest {
    @ArchTest
    static final ArchRule contracts_do_not_depend_on_services = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.achobeta.refine.identity..",
                    "com.achobeta.refine.learning..",
                    "com.achobeta.refine.ai..",
                    "com.achobeta.refine.gateway..");
}
