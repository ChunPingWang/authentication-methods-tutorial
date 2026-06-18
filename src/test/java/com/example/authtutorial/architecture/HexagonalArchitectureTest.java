package com.example.authtutorial.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 用「測試」把六角形架構的相依規則<b>變成可執行的合約</b>。
 *
 * <p>這是初學者最容易忽略卻最重要的一環：架構若沒有自動化檢查，幾次重構後就會
 * 腐化。以下規則保證 domain / application 永遠不依賴框架或外圈轉接器。</p>
 *
 * <p>相依方向只能由外向內：adapter → application → domain。</p>
 */
@AnalyzeClasses(packages = "com.example.authtutorial", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta.persistence..")
                    .as("domain 層必須與框架解耦（不得依賴 Spring）");

    @ArchTest
    static final ArchRule application_should_not_depend_on_spring =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .as("application 層必須與框架解耦（依賴反轉，由組合根注入）");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .as("domain 不可依賴 application（相依方向由外向內）");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_adapters =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .as("domain 不可依賴任何轉接器");

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapters =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .as("application 只依賴埠 (port)，不可依賴具體轉接器");
}
