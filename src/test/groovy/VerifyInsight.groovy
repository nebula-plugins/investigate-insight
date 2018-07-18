/**
 *
 *  Copyright 2018 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */


import com.google.common.collect.ImmutableMap
import spock.lang.Unroll

class VerifyInsight extends AbstractVerifyInsight {
    static def guava = 'guava'
    static def guavaDependency = 'com.google.guava:guava'
    static def guavaStatic = '18.0'
    static def guavaDynamic = '18.+'
    static def guavaForce = '14.0.1'
    static def guavaLock = '13.0'
    static def guavaRec = '19.0'
    static def replaceFromDep = 'com.google.collections:google-collections:1.0'

    static def mockito = 'mockito'
    static def mockitoDependency = 'org.mockito:mockito-all'
    static def mockitoStatic = '1.8.0'
    static def mockitoDynamic = '1.8.+'
    static def mockitoForce = '1.10.17'
    static def mockitoLock = '2.1.0'
    static def mockitoRec = '1.9.5'
    static def substituteWith = 'org.mockito:mockito-core:1.10.19'

    public static
    def lookupRequestedModuleIdentifier = ImmutableMap.of(guava, guavaDependency, mockito, mockitoDependency)
    public static def lookupDynamicResolveVersion = ImmutableMap.of(guava, guavaStatic, mockito, mockitoStatic)

    @Unroll
    def "#title - #insightSource"() {
        given:
        createSimpleBuildFile(insightSource)
        gradleVersion = insightSource

        createLockfileIfNeeded(dep, lockVersion)

        def dependencyHelper = new DependencyHelper()
        dependencyHelper.staticVersion = staticVersion
        dependencyHelper.dynamicVersion = dynamicVersion
        dependencyHelper.recommendedVersion = recVersion
        dependencyHelper.forceVersion = forceVersion
        dependencyHelper.lockVersion = lockVersion
        dependencyHelper.versionForDynamicToResolveTo = lookupDynamicResolveVersion[dep]

        dependencyHelper.requestedModuleIdentifier = dep
        dependencyHelper.replaceFrom = replaceFrom
        dependencyHelper.substituteWith = substitute
        dependencyHelper.lookupRequestedModuleIdentifier = lookupRequestedModuleIdentifier

        def version = dependencyHelper.findRequestedVersion() // static, dynamic, or recommended

        buildFile << """
            dependencies {
                compile 'org.slf4j:slf4j-api:1.7.25'
                compile 'org.slf4j:slf4j-simple:1.7.25'
                compile '${lookupRequestedModuleIdentifier[dep]}${version}'
                ${replaceFrom != null ? "compile '$replaceFrom'" : ''}
            }
            ${createForceConfigurationIfNeeded(dep, forceVersion, lookupRequestedModuleIdentifier)}
            """.stripIndent()

        createJavaSourceFile(projectDir, createMainFile())
        def tasks = tasksFor(dep)

        when:
        def result = runTasks(*tasks)

        then:
        DocWriter w = new DocWriter(title, insightSource, projectDir)
        w.writeCleanedUpBuildOutput('=== For the dependency under test ===\n' +
                "Tasks: ${tasks.join(' ')}\n\n" +
                result.output)
        w.writeProjectFiles()

        verifyOutput(result.output, dependencyHelper, dep, w)
        w.writeFooter('completed assertions')

        where:
        insightSource | dep     | staticVersion | dynamicVersion | recVersion | forceVersion | lockVersion | replaceFrom    | substitute     | title
//        ===plugins insight===
//        static
        plugins       | guava   | guavaStatic   | null           | null       | null         | null        | null           | null           | 'static'
        plugins       | guava   | guavaStatic   | null           | null       | guavaForce   | null        | null           | null           | 'static-force'
        plugins       | guava   | guavaStatic   | null           | null       | guavaForce   | guavaLock   | null           | null           | 'static-force-lock'
        plugins       | guava   | guavaStatic   | null           | null       | null         | guavaLock   | null           | null           | 'static-lock'
//         dynamic
        plugins       | guava   | null          | guavaDynamic   | null       | null         | null        | null           | null           | 'dynamic'
        plugins       | guava   | null          | guavaDynamic   | null       | guavaForce   | null        | null           | null           | 'dynamic-force'
        plugins       | guava   | null          | guavaDynamic   | null       | guavaForce   | guavaLock   | null           | null           | 'dynamic-force-lock'
        plugins       | guava   | null          | guavaDynamic   | null       | null         | guavaLock   | null           | null           | 'dynamic-lock'
//         recommendation
        plugins       | guava   | null          | null           | guavaRec   | null         | null        | null           | null           | 'rec'
        plugins       | guava   | null          | null           | guavaRec   | guavaForce   | null        | null           | null           | 'rec-force'
        plugins       | guava   | null          | null           | guavaRec   | guavaForce   | guavaLock   | null           | null           | 'rec-force-lock'
        plugins       | guava   | null          | null           | guavaRec   | null         | guavaLock   | null           | null           | 'rec-lock'
//        replacement - static
        plugins       | guava   | guavaStatic   | null           | null       | null         | null        | replaceFromDep | null           | 'replacement-static'
        plugins       | guava   | guavaStatic   | null           | null       | guavaForce   | null        | replaceFromDep | null           | 'replacement-static-force'
        plugins       | guava   | guavaStatic   | null           | null       | guavaForce   | guavaLock   | replaceFromDep | null           | 'replacement-static-force-lock'
        plugins       | guava   | guavaStatic   | null           | null       | null         | guavaLock   | replaceFromDep | null           | 'replacement-static-lock'
//        replacement - dynamic
        plugins       | guava   | null          | guavaDynamic   | null       | null         | null        | replaceFromDep | null           | 'replacement-dynamic'
        plugins       | guava   | null          | guavaDynamic   | null       | guavaForce   | null        | replaceFromDep | null           | 'replacement-dynamic-force'
        plugins       | guava   | null          | guavaDynamic   | null       | guavaForce   | guavaLock   | replaceFromDep | null           | 'replacement-dynamic-force-lock'
        plugins       | guava   | null          | guavaDynamic   | null       | null         | guavaLock   | replaceFromDep | null           | 'replacement-dynamic-lock'
//        replacement - with recommendation
        plugins       | guava   | null          | null           | guavaRec   | null         | null        | replaceFromDep | null           | 'replacement-rec'
        plugins       | guava   | null          | null           | guavaRec   | guavaForce   | null        | replaceFromDep | null           | 'replacement-rec-force'
        plugins       | guava   | null          | null           | guavaRec   | guavaForce   | guavaLock   | replaceFromDep | null           | 'replacement-rec-force-lock'
        plugins       | guava   | null          | null           | guavaRec   | null         | guavaLock   | replaceFromDep | null           | 'replacement-rec-lock'
//        substitution - static
        plugins       | mockito | mockitoStatic | null           | null       | null         | null        | null           | substituteWith | 'substitute-static'
        plugins       | mockito | mockitoStatic | null           | null       | mockitoForce | null        | null           | substituteWith | 'substitute-static-force'
        plugins       | mockito | mockitoStatic | null           | null       | mockitoForce | mockitoLock | null           | substituteWith | 'substitute-static-force-lock'
        plugins       | mockito | mockitoStatic | null           | null       | null         | mockitoLock | null           | substituteWith | 'substitute-static-lock'
//        substitution - dynamic
        plugins       | mockito | null          | mockitoDynamic | null       | null         | null        | null           | substituteWith | 'substitute-dynamic'
        plugins       | mockito | null          | mockitoDynamic | null       | mockitoForce | null        | null           | substituteWith | 'substitute-dynamic-force'
        plugins       | mockito | null          | mockitoDynamic | null       | mockitoForce | mockitoLock | null           | substituteWith | 'substitute-dynamic-force-lock'
        plugins       | mockito | null          | mockitoDynamic | null       | null         | mockitoLock | null           | substituteWith | 'substitute-dynamic-lock'
//        substitution - with recommendation
        plugins       | mockito | null          | null           | mockitoRec | null         | null        | null           | substituteWith | 'substitute-rec'
        plugins       | mockito | null          | null           | mockitoRec | mockitoForce | null        | null           | substituteWith | 'substitute-rec-force'
        plugins       | mockito | null          | null           | mockitoRec | mockitoForce | mockitoLock | null           | substituteWith | 'substitute-rec-force-lock'
        plugins       | mockito | null          | null           | mockitoRec | null         | mockitoLock | null           | substituteWith | 'substitute-rec-lock'

//        ===core insight===
//        static
        core          | guava   | guavaStatic   | null           | null       | null         | null        | null           | null           | 'static'
        core          | guava   | guavaStatic   | null           | null       | guavaForce   | null        | null           | null           | 'static-force'
        core          | guava   | guavaStatic   | null           | null       | guavaForce   | guavaLock   | null           | null           | 'static-force-lock'
        core          | guava   | guavaStatic   | null           | null       | null         | guavaLock   | null           | null           | 'static-lock'
//         dynamic
        core          | guava   | null          | guavaDynamic   | null       | null         | null        | null           | null           | 'dynamic'
        core          | guava   | null          | guavaDynamic   | null       | guavaForce   | null        | null           | null           | 'dynamic-force'
        core          | guava   | null          | guavaDynamic   | null       | guavaForce   | guavaLock   | null           | null           | 'dynamic-force-lock'
        core          | guava   | null          | guavaDynamic   | null       | null         | guavaLock   | null           | null           | 'dynamic-lock'
//         recommendation
        core          | guava   | null          | null           | guavaRec   | null         | null        | null           | null           | 'rec'
        core          | guava   | null          | null           | guavaRec   | guavaForce   | null        | null           | null           | 'rec-force'
        core          | guava   | null          | null           | guavaRec   | guavaForce   | guavaLock   | null           | null           | 'rec-force-lock'
        core          | guava   | null          | null           | guavaRec   | null         | guavaLock   | null           | null           | 'rec-lock'
//        replacement - static
        core          | guava   | guavaStatic   | null           | null       | null         | null        | replaceFromDep | null           | 'replacement-static'
        core          | guava   | guavaStatic   | null           | null       | guavaForce   | null        | replaceFromDep | null           | 'replacement-static-force'
        core          | guava   | guavaStatic   | null           | null       | guavaForce   | guavaLock   | replaceFromDep | null           | 'replacement-static-force-lock'
        core          | guava   | guavaStatic   | null           | null       | null         | guavaLock   | replaceFromDep | null           | 'replacement-static-lock'
//        replacement - dynamic
        core          | guava   | null          | guavaDynamic   | null       | null         | null        | replaceFromDep | null           | 'replacement-dynamic'
        core          | guava   | null          | guavaDynamic   | null       | guavaForce   | null        | replaceFromDep | null           | 'replacement-dynamic-force'
        core          | guava   | null          | guavaDynamic   | null       | guavaForce   | guavaLock   | replaceFromDep | null           | 'replacement-dynamic-force-lock'
        core          | guava   | null          | guavaDynamic   | null       | null         | guavaLock   | replaceFromDep | null           | 'replacement-dynamic-lock'
//        replacement - with recommendation
        core          | guava   | null          | null           | guavaRec   | null         | null        | replaceFromDep | null           | 'replacement-rec'
        core          | guava   | null          | null           | guavaRec   | guavaForce   | null        | replaceFromDep | null           | 'replacement-rec-force'
        core          | guava   | null          | null           | guavaRec   | guavaForce   | guavaLock   | replaceFromDep | null           | 'replacement-rec-force-lock'
        core          | guava   | null          | null           | guavaRec   | null         | guavaLock   | replaceFromDep | null           | 'replacement-rec-lock'
//        substitution - static
        core          | mockito | mockitoStatic | null           | null       | null         | null        | null           | substituteWith | 'substitute-static'
        core          | mockito | mockitoStatic | null           | null       | mockitoForce | null        | null           | substituteWith | 'substitute-static-force'
        core          | mockito | mockitoStatic | null           | null       | mockitoForce | mockitoLock | null           | substituteWith | 'substitute-static-force-lock'
        core          | mockito | mockitoStatic | null           | null       | null         | mockitoLock | null           | substituteWith | 'substitute-static-lock'
//        substitution - dynamic
        core          | mockito | null          | mockitoDynamic | null       | null         | null        | null           | substituteWith | 'substitute-dynamic'
        core          | mockito | null          | mockitoDynamic | null       | mockitoForce | null        | null           | substituteWith | 'substitute-dynamic-force'
        core          | mockito | null          | mockitoDynamic | null       | mockitoForce | mockitoLock | null           | substituteWith | 'substitute-dynamic-force-lock'
        core          | mockito | null          | mockitoDynamic | null       | null         | mockitoLock | null           | substituteWith | 'substitute-dynamic-lock'
//        substitution - with recommendation
        core          | mockito | null          | null           | mockitoRec | null         | null        | null           | substituteWith | 'substitute-rec'
        core          | mockito | null          | null           | mockitoRec | mockitoForce | null        | null           | substituteWith | 'substitute-rec-force'
        core          | mockito | null          | null           | mockitoRec | mockitoForce | mockitoLock | null           | substituteWith | 'substitute-rec-force-lock'
        core          | mockito | null          | null           | mockitoRec | null         | mockitoLock | null           | substituteWith | 'substitute-rec-lock'
    }

    private static void verifyOutput(String output, DependencyHelper dh, String dep, DocWriter w) {
        def expected = dh.results()
        def requestedVersion = dh.findRequestedVersion()

        assert expected.version != null || expected.moduleIdentifierWithVersion != null

        if (dh.substituteWith != null) {
            if (dh.staticVersion != null) {
                def expectedOutput = "${lookupRequestedModuleIdentifier[dep]}:${dh.staticVersion} -> ${expected}"
                w.addAssertionToDoc("contains $expectedOutput [substitute & static]")
                assert output.contains(expectedOutput)

            } else if (dh.recommendedVersion != null) {
                def expectedOutput = "${lookupRequestedModuleIdentifier[dep]} -> ${expected}"
                w.addAssertionToDoc("contains $expectedOutput [substitute & recommended]")
                assert output.contains(expectedOutput)

            } else {
                def expectedOutput = "${lookupRequestedModuleIdentifier[dep]}:${dh.dynamicVersion} -> ${expected}"
                w.addAssertionToDoc("contains $expectedOutput [substitute & dynamic]")
                assert output.contains(expectedOutput)
            }

            if (output.contains('locked')) { // whole substitutions supercede locks of substituted dependency
                w.addAssertionToDoc("contains 'nebula-dependency-lock tag line if \"locked\" is in output' [substitute]")
                assert output.contains('nebula.dependency-lock locked with: dependencies.lock')
            } else {
                w.addAssertionToDoc("does not contain 'locked' [substitute]")
            }

            w.addAssertionToDoc("does not contain 'forced/Forced' [substitute]")
            assert !(output.contains('forced') || output.contains('Forced'))
            // whole substitutions supercede forces of substituted dependency

            def endResultRegex = "Task.*\n.*$expected"
            w.addAssertionToDoc("contains $endResultRegex [substitute end result]")
            assert output.findAll { endResultRegex }.size() > 0

            return // if substitution occurs, stop checking here
        }

        if (dh.lockVersion != null) {
            w.addAssertionToDoc("contains 'locked'")
            assert output.contains('locked')

            def expectedOutput = "${expected.moduleIdentifier}${requestedVersion} -> ${expected.version}"
            w.addAssertionToDoc("contains $expectedOutput [locked]")
            assert output.contains(expectedOutput)
        }

        if (dh.forceVersion != null) {
            // FIXME: currently locked and forced go only down the 'locked' route of assertions
            w.addAssertionToDoc("contains 'forced/Forced'")
            assert output.contains('forced') || output.contains('Forced')
        }

        if (dh.forceVersion != null && dh.lockVersion == null) {
            def expectedOutput = "${expected.moduleIdentifier}${requestedVersion} -> ${expected.version}"
            w.addAssertionToDoc("contains $expectedOutput [forced and not locked]")
            assert output.contains(expectedOutput)
        }

        if (dh.replaceFrom != null) {
            def expectedOutput = "${dh.replaceFrom} -> ${expected}"
            w.addAssertionToDoc("contains $expectedOutput [replaced from]")
            assert output.contains(expectedOutput)
        }

        if (dh.staticVersion != null) {
            def endResultRegex = "Task.*\n.*$expected"
            w.addAssertionToDoc("contains $endResultRegex [static version end result]")
            assert output.findAll { endResultRegex }.size() > 0
        }

        if (dh.dynamicVersion != null) {
            def expectedOutput = "${expected.moduleIdentifier}:$dh.dynamicVersion -> ${expected.version}"
            w.addAssertionToDoc("contains $expectedOutput [dynamic]")
            assert output.contains(expectedOutput)
//            assert output.contains('Was requested')

            def endResultRegex = "Task.*\n.*$expected"
            w.addAssertionToDoc("contains $endResultRegex [dynamic version end result]")
            assert output.findAll { endResultRegex }.size() > 0
        }

        if (dh.recommendedVersion != null) {
//                assert output.contains("Recommending version ${m.recommendedVersion} for dependency")
            def expectedOutput = "${expected.moduleIdentifier} -> ${expected.version}"
            w.addAssertionToDoc("contains $expectedOutput [recommended]")
            assert output.contains(expectedOutput)

            def endResultRegex = "Task.*\n.*$expected"
            w.addAssertionToDoc("contains $endResultRegex [recommended end result]")
            assert output.findAll { endResultRegex }.size() > 0
        }
    }

}