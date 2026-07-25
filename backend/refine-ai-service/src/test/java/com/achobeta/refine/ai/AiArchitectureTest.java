package com.achobeta.refine.ai;

import com.achobeta.refine.ai.conversation.application.port.ConversationAiPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionAiPort;
import com.achobeta.refine.ai.ocr.application.port.OcrQuestionClassificationPort;
import com.achobeta.refine.ai.question.application.port.QuestionAiPort;
import com.achobeta.refine.ai.solve.application.port.SolveAiPort;
import com.achobeta.refine.ai.suggestion.application.port.SuggestionAiPort;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.core.importer.ImportOption;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.achobeta.refine.ai", importOptions = ImportOption.DoNotIncludeTests.class)
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

    @ArchTest
    static final ArchRule ai_ports_have_capability_local_adapters = classes()
            .that().implement(ConversationAiPort.class)
            .should().resideInAPackage("..conversation.infrastructure..");

    @ArchTest
    static final ArchRule solve_ports_have_capability_local_adapters = classes()
            .that().implement(SolveAiPort.class)
            .should().resideInAPackage("..solve.infrastructure..");

    @ArchTest
    static final ArchRule question_ports_have_capability_local_adapters = classes()
            .that().implement(QuestionAiPort.class)
            .should().resideInAPackage("..question.infrastructure..");

    @ArchTest
    static final ArchRule ocr_text_ports_have_capability_local_adapters = classes()
            .that().implement(OcrQuestionAiPort.class)
            .or().implement(OcrQuestionClassificationPort.class)
            .should().resideInAPackage("..ocr.infrastructure..");

    @ArchTest
    static final ArchRule suggestion_ports_have_capability_local_adapters = classes()
            .that().implement(SuggestionAiPort.class)
            .should().resideInAPackage("..suggestion.infrastructure..");
}
