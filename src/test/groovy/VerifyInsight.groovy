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
    static def guavaReplaceFrom = 'com.google.collections:google-collections:1.0'

    static def mockito = 'mockito'
    static def mockitoDependency = 'org.mockito:mockito-all'
    static def mockitoStatic = '1.8.0'
    static def mockitoDynamic = '1.8.+'
    static def mockitoForce = '1.10.17'
    static def mockitoLock = '2.1.0'
    static def mockitoRec = '1.9.5'
    static def mockitoSubTo = 'org.mockito:mockito-core:1.10.19'

    static def netty = 'netty'
    static def nettyDependency = 'io.netty:netty-all'
    static def nettyStatic = '4.1.20.FINAL'
    static def nettyDynamic = '4.1.+'
    static def nettyForce = '4.1.10.FINAL'
    static def nettyLock = '4.1.15.FINAL'
    static def nettyRec = '4.1.22.FINAL'
    static def nettySubTo = 'io.netty:netty-common:4.1.23.Final'

    public static
    def lookupRequestedModuleIdentifier = ImmutableMap.of(guava, guavaDependency, mockito, mockitoDependency, netty, nettyDependency)
    public static
    def lookupDynamicResolveVersion = ImmutableMap.of(guava, guavaStatic, mockito, mockitoStatic, netty, nettyStatic)

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
        dependencyHelper.exclude = exclude
        dependencyHelper.lookupRequestedModuleIdentifier = lookupRequestedModuleIdentifier

        def version = dependencyHelper.findRequestedVersion() // static, dynamic, or recommended

        boolean needsAdditionalResolutionRuleFile = needsAdditionalResolutionRuleFile(dep, dependencyHelper)
        if (needsAdditionalResolutionRuleFile) {
            createAdditionalResolutionRuleFile(dep, dependencyHelper)
        }

        buildFile << """
dependencies {
    compile 'org.slf4j:slf4j-api:1.7.25'
    compile 'org.slf4j:slf4j-simple:1.7.25'
    compile '${lookupRequestedModuleIdentifier[dep]}${version}'
    ${replaceFrom != null ? "compile '$replaceFrom'" : ''}
    ${needsAdditionalResolutionRuleFile ? "resolutionRules files('additional-rules.json')" : ''}
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
        insightSource | dep     | staticVersion | dynamicVersion | recVersion | forceVersion | lockVersion | replaceFrom      | substitute   | exclude | title
//        ===plugins insight===
//        static
        plugins       | guava   | guavaStatic   | null           | null       | null         | null        | null             | null         | null    | 'static'
        plugins       | guava   | guavaStatic   | null           | null       | guavaForce   | null        | null             | null         | null    | 'static-force'
        plugins       | guava   | guavaStatic   | null           | null       | guavaForce   | guavaLock   | null             | null         | null    | 'static-force-lock'
        plugins       | guava   | guavaStatic   | null           | null       | null         | guavaLock   | null             | null         | null    | 'static-lock'
//         dynamic
        plugins       | guava   | null          | guavaDynamic   | null       | null         | null        | null             | null         | null    | 'dynamic'
        plugins       | guava   | null          | guavaDynamic   | null       | guavaForce   | null        | null             | null         | null    | 'dynamic-force'
        plugins       | guava   | null          | guavaDynamic   | null       | guavaForce   | guavaLock   | null             | null         | null    | 'dynamic-force-lock'
        plugins       | guava   | null          | guavaDynamic   | null       | null         | guavaLock   | null             | null         | null    | 'dynamic-lock'
//         recommendation
        plugins       | guava   | null          | null           | guavaRec   | null         | null        | null             | null         | null    | 'rec'
        plugins       | guava   | null          | null           | guavaRec   | guavaForce   | null        | null             | null         | null    | 'rec-force'
        plugins       | guava   | null          | null           | guavaRec   | guavaForce   | guavaLock   | null             | null         | null    | 'rec-force-lock'
        plugins       | guava   | null          | null           | guavaRec   | null         | guavaLock   | null             | null         | null    | 'rec-lock'
//        replacement - static
        plugins       | guava   | guavaStatic   | null           | null       | null         | null        | guavaReplaceFrom | null         | null    | 'replacement-static'
        plugins       | guava   | guavaStatic   | null           | null       | guavaForce   | null        | guavaReplaceFrom | null         | null    | 'replacement-static-force'
        plugins       | guava   | guavaStatic   | null           | null       | guavaForce   | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-static-force-lock'
        plugins       | guava   | guavaStatic   | null           | null       | null         | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-static-lock'
//        replacement - dynamic
        plugins       | guava   | null          | guavaDynamic   | null       | null         | null        | guavaReplaceFrom | null         | null    | 'replacement-dynamic'
        plugins       | guava   | null          | guavaDynamic   | null       | guavaForce   | null        | guavaReplaceFrom | null         | null    | 'replacement-dynamic-force'
        plugins       | guava   | null          | guavaDynamic   | null       | guavaForce   | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-dynamic-force-lock'
        plugins       | guava   | null          | guavaDynamic   | null       | null         | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-dynamic-lock'
//        replacement - with recommendation
        plugins       | guava   | null          | null           | guavaRec   | null         | null        | guavaReplaceFrom | null         | null    | 'replacement-rec'
        plugins       | guava   | null          | null           | guavaRec   | guavaForce   | null        | guavaReplaceFrom | null         | null    | 'replacement-rec-force'
        plugins       | guava   | null          | null           | guavaRec   | guavaForce   | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-rec-force-lock'
        plugins       | guava   | null          | null           | guavaRec   | null         | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-rec-lock'
//        substitution - static
        plugins       | mockito | mockitoStatic | null           | null       | null         | null        | null             | mockitoSubTo | null    | 'substitute-static'
        plugins       | mockito | mockitoStatic | null           | null       | mockitoForce | null        | null             | mockitoSubTo | null    | 'substitute-static-force'
        plugins       | mockito | mockitoStatic | null           | null       | mockitoForce | mockitoLock | null             | mockitoSubTo | null    | 'substitute-static-force-lock'
        plugins       | mockito | mockitoStatic | null           | null       | null         | mockitoLock | null             | mockitoSubTo | null    | 'substitute-static-lock'
//        substitution - dynamic
        plugins       | mockito | null          | mockitoDynamic | null       | null         | null        | null             | mockitoSubTo | null    | 'substitute-dynamic'
        plugins       | mockito | null          | mockitoDynamic | null       | mockitoForce | null        | null             | mockitoSubTo | null    | 'substitute-dynamic-force'
        plugins       | mockito | null          | mockitoDynamic | null       | mockitoForce | mockitoLock | null             | mockitoSubTo | null    | 'substitute-dynamic-force-lock'
        plugins       | mockito | null          | mockitoDynamic | null       | null         | mockitoLock | null             | mockitoSubTo | null    | 'substitute-dynamic-lock'
//        substitution - with recommendation
        plugins       | mockito | null          | null           | mockitoRec | null         | null        | null             | mockitoSubTo | null    | 'substitute-rec'
        plugins       | mockito | null          | null           | mockitoRec | mockitoForce | null        | null             | mockitoSubTo | null    | 'substitute-rec-force'
        plugins       | mockito | null          | null           | mockitoRec | mockitoForce | mockitoLock | null             | mockitoSubTo | null    | 'substitute-rec-force-lock'
        plugins       | mockito | null          | null           | mockitoRec | null         | mockitoLock | null             | mockitoSubTo | null    | 'substitute-rec-lock'
//        exclude - static
        plugins       | netty   | nettyStatic   | null           | null       | null         | null        | null             | null         | true    | 'exclude-static'
        plugins       | netty   | nettyStatic   | null           | null       | nettyForce   | null        | null             | null         | true    | 'exclude-static-force'
        plugins       | netty   | nettyStatic   | null           | null       | nettyForce   | nettyLock   | null             | null         | true    | 'exclude-static-force-lock'
        plugins       | netty   | nettyStatic   | null           | null       | null         | nettyLock   | null             | null         | true    | 'exclude-static-lock'
//        exclude - dynamic
        plugins       | netty   | null          | nettyDynamic   | null       | null         | null        | null             | null         | true    | 'exclude-dynamic'
        plugins       | netty   | null          | nettyDynamic   | null       | nettyForce   | null        | null             | null         | true    | 'exclude-dynamic-force'
        plugins       | netty   | null          | nettyDynamic   | null       | nettyForce   | nettyLock   | null             | null         | true    | 'exclude-dynamic-force-lock'
        plugins       | netty   | null          | nettyDynamic   | null       | null         | nettyLock   | null             | null         | true    | 'exclude-dynamic-lock'
//        exclude - with recommendation
        plugins       | netty   | null          | null           | nettyRec   | null         | null        | null             | null         | true    | 'exclude-rec'
        plugins       | netty   | null          | null           | nettyRec   | nettyForce   | null        | null             | null         | true    | 'exclude-rec-force'
        plugins       | netty   | null          | null           | nettyRec   | nettyForce   | nettyLock   | null             | null         | true    | 'exclude-rec-force-lock'
        plugins       | netty   | null          | null           | nettyRec   | null         | nettyLock   | null             | null         | true    | 'exclude-rec-lock'
//        exclude - static & with substitution
        plugins       | netty   | nettyStatic   | null           | null       | null         | null        | null             | nettySubTo   | true    | 'exclude-substitute-static'
        plugins       | netty   | nettyStatic   | null           | null       | nettyForce   | null        | null             | nettySubTo   | true    | 'exclude-substitute-static-force'
        plugins       | netty   | nettyStatic   | null           | null       | nettyForce   | nettyLock   | null             | nettySubTo   | true    | 'exclude-substitute-static-force-lock'
        plugins       | netty   | nettyStatic   | null           | null       | null         | nettyLock   | null             | nettySubTo   | true    | 'exclude-substitute-static-lock'

//        ===core insight===
//        static
        core          | guava   | guavaStatic   | null           | null       | null         | null        | null             | null         | null    | 'static'
        core          | guava   | guavaStatic   | null           | null       | guavaForce   | null        | null             | null         | null    | 'static-force'
        core          | guava   | guavaStatic   | null           | null       | guavaForce   | guavaLock   | null             | null         | null    | 'static-force-lock'
        core          | guava   | guavaStatic   | null           | null       | null         | guavaLock   | null             | null         | null    | 'static-lock'
//         dynamic
        core          | guava   | null          | guavaDynamic   | null       | null         | null        | null             | null         | null    | 'dynamic'
        core          | guava   | null          | guavaDynamic   | null       | guavaForce   | null        | null             | null         | null    | 'dynamic-force'
        core          | guava   | null          | guavaDynamic   | null       | guavaForce   | guavaLock   | null             | null         | null    | 'dynamic-force-lock'
        core          | guava   | null          | guavaDynamic   | null       | null         | guavaLock   | null             | null         | null    | 'dynamic-lock'
//         recommendation
        core          | guava   | null          | null           | guavaRec   | null         | null        | null             | null         | null    | 'rec'
        core          | guava   | null          | null           | guavaRec   | guavaForce   | null        | null             | null         | null    | 'rec-force'
        core          | guava   | null          | null           | guavaRec   | guavaForce   | guavaLock   | null             | null         | null    | 'rec-force-lock'
        core          | guava   | null          | null           | guavaRec   | null         | guavaLock   | null             | null         | null    | 'rec-lock'
//        replacement - static
        core          | guava   | guavaStatic   | null           | null       | null         | null        | guavaReplaceFrom | null         | null    | 'replacement-static'
        core          | guava   | guavaStatic   | null           | null       | guavaForce   | null        | guavaReplaceFrom | null         | null    | 'replacement-static-force'
        core          | guava   | guavaStatic   | null           | null       | guavaForce   | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-static-force-lock'
        core          | guava   | guavaStatic   | null           | null       | null         | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-static-lock'
//        replacement - dynamic
        core          | guava   | null          | guavaDynamic   | null       | null         | null        | guavaReplaceFrom | null         | null    | 'replacement-dynamic'
        core          | guava   | null          | guavaDynamic   | null       | guavaForce   | null        | guavaReplaceFrom | null         | null    | 'replacement-dynamic-force'
        core          | guava   | null          | guavaDynamic   | null       | guavaForce   | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-dynamic-force-lock'
        core          | guava   | null          | guavaDynamic   | null       | null         | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-dynamic-lock'
//        replacement - with recommendation
        core          | guava   | null          | null           | guavaRec   | null         | null        | guavaReplaceFrom | null         | null    | 'replacement-rec'
        core          | guava   | null          | null           | guavaRec   | guavaForce   | null        | guavaReplaceFrom | null         | null    | 'replacement-rec-force'
        core          | guava   | null          | null           | guavaRec   | guavaForce   | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-rec-force-lock'
        core          | guava   | null          | null           | guavaRec   | null         | guavaLock   | guavaReplaceFrom | null         | null    | 'replacement-rec-lock'
//        substitution - static
        core          | mockito | mockitoStatic | null           | null       | null         | null        | null             | mockitoSubTo | null    | 'substitute-static'
        core          | mockito | mockitoStatic | null           | null       | mockitoForce | null        | null             | mockitoSubTo | null    | 'substitute-static-force'
        core          | mockito | mockitoStatic | null           | null       | mockitoForce | mockitoLock | null             | mockitoSubTo | null    | 'substitute-static-force-lock'
        core          | mockito | mockitoStatic | null           | null       | null         | mockitoLock | null             | mockitoSubTo | null    | 'substitute-static-lock'
//        substitution - dynamic
        core          | mockito | null          | mockitoDynamic | null       | null         | null        | null             | mockitoSubTo | null    | 'substitute-dynamic'
        core          | mockito | null          | mockitoDynamic | null       | mockitoForce | null        | null             | mockitoSubTo | null    | 'substitute-dynamic-force'
        core          | mockito | null          | mockitoDynamic | null       | mockitoForce | mockitoLock | null             | mockitoSubTo | null    | 'substitute-dynamic-force-lock'
        core          | mockito | null          | mockitoDynamic | null       | null         | mockitoLock | null             | mockitoSubTo | null    | 'substitute-dynamic-lock'
//        substitution - with recommendation
        core          | mockito | null          | null           | mockitoRec | null         | null        | null             | mockitoSubTo | null    | 'substitute-rec'
        core          | mockito | null          | null           | mockitoRec | mockitoForce | null        | null             | mockitoSubTo | null    | 'substitute-rec-force'
        core          | mockito | null          | null           | mockitoRec | mockitoForce | mockitoLock | null             | mockitoSubTo | null    | 'substitute-rec-force-lock'
        core          | mockito | null          | null           | mockitoRec | null         | mockitoLock | null             | mockitoSubTo | null    | 'substitute-rec-lock'
//        exclude - static
        core          | netty   | nettyStatic   | null           | null       | null         | null        | null             | null         | true    | 'exclude-static'
        core          | netty   | nettyStatic   | null           | null       | nettyForce   | null        | null             | null         | true    | 'exclude-static-force'
        core          | netty   | nettyStatic   | null           | null       | nettyForce   | nettyLock   | null             | null         | true    | 'exclude-static-force-lock'
        core          | netty   | nettyStatic   | null           | null       | null         | nettyLock   | null             | null         | true    | 'exclude-static-lock'
//        exclude - dynamic
        core          | netty   | null          | nettyDynamic   | null       | null         | null        | null             | null         | true    | 'exclude-dynamic'
        core          | netty   | null          | nettyDynamic   | null       | nettyForce   | null        | null             | null         | true    | 'exclude-dynamic-force'
        core          | netty   | null          | nettyDynamic   | null       | nettyForce   | nettyLock   | null             | null         | true    | 'exclude-dynamic-force-lock'
        core          | netty   | null          | nettyDynamic   | null       | null         | nettyLock   | null             | null         | true    | 'exclude-dynamic-lock'
//        exclude - with recommendation
        core          | netty   | null          | null           | nettyRec   | null         | null        | null             | null         | true    | 'exclude-rec'
        core          | netty   | null          | null           | nettyRec   | nettyForce   | null        | null             | null         | true    | 'exclude-rec-force'
        core          | netty   | null          | null           | nettyRec   | nettyForce   | nettyLock   | null             | null         | true    | 'exclude-rec-force-lock'
        core          | netty   | null          | null           | nettyRec   | null         | nettyLock   | null             | null         | true    | 'exclude-rec-lock'
//        exclude - static & with substitution
        core          | netty   | nettyStatic   | null           | null       | null         | null        | null             | nettySubTo   | true    | 'exclude-substitute-static'
        core          | netty   | nettyStatic   | null           | null       | nettyForce   | null        | null             | nettySubTo   | true    | 'exclude-substitute-static-force'
        core          | netty   | nettyStatic   | null           | null       | nettyForce   | nettyLock   | null             | nettySubTo   | true    | 'exclude-substitute-static-force-lock'
        core          | netty   | nettyStatic   | null           | null       | null         | nettyLock   | null             | nettySubTo   | true    | 'exclude-substitute-static-lock'

    }

    private static void verifyOutput(String output, DependencyHelper dh, String dep, DocWriter w) {
        def expected = dh.results()
        def requestedVersion = dh.findRequestedVersion()

        assert expected.version != null || expected.moduleIdentifierWithVersion != null

        if (dh.exclude != null) {
            def expectedOutput = 'No dependencies matching given input were found in configuration'
            w.addAssertionToDoc("$expectedOutput [exclude]")
            assert output.contains(expectedOutput)
            return // if exclude occurs, stop checking here
        }

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
