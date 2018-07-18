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

class VerifyInsightOverrideLock extends AbstractVerifyInsight {
    static def guava = 'guava'
    static def guavaDependency = 'com.google.guava:guava'
    static def guavaStatic = '18.0'
    static def guavaLock = '13.0'
    static def guavaOverrideLockFromFile = '10.0'
    static def guavaOverrideLockFromProp = '11.0'

    static def control = 'commons-io'
    static def controlDependency = 'commons-io:commons-io'

    public static
    def lookupRequestedModuleIdentifier = ImmutableMap.of(guava, guavaDependency, control, controlDependency)
    public static def lookupDynamicResolveVersion = ImmutableMap.of(guava, guavaStatic)

    @Unroll
    def "#title - #insightSource"() {
        given:
        createSimpleBuildFile(insightSource)
        gradleVersion = insightSource

        createLockfileIfNeeded(dep, lockVersion)
        createOverrideLockfileIfNeeded(lookupRequestedModuleIdentifier[dep], overrideLockFileVersion)

        def dependencyHelper = new DependencyHelper()
        dependencyHelper.lockVersion = lockVersion
        dependencyHelper.staticVersion = staticVersion
        dependencyHelper.overrideLockFileVersion = overrideLockFileVersion
        dependencyHelper.overrideLockPropertyVersion = overrideLockPropertyVersion
        dependencyHelper.versionForDynamicToResolveTo = lookupDynamicResolveVersion[dep]

        dependencyHelper.requestedModuleIdentifier = dep
        dependencyHelper.lookupRequestedModuleIdentifier = lookupRequestedModuleIdentifier

        def version = dependencyHelper.findRequestedVersion() // static, dynamic, or recommended

        buildFile << """
            dependencies {
                compile 'org.slf4j:slf4j-api:1.7.25'
                compile 'org.slf4j:slf4j-simple:1.7.25'
                compile '${lookupRequestedModuleIdentifier[dep]}${version}'
                compile '${lookupRequestedModuleIdentifier[control]}:2.6' // the control dependency
            }
            """.stripIndent()

        createJavaSourceFile(projectDir, createMainFile())

        when:
        def tasksForOverrides = tasksFor(dep)
        if (overrideLockFileVersion != null) {
            tasksForOverrides.add('-PdependencyLock.overrideFile=override.lock')
        }
        if (overrideLockPropertyVersion != null) {
            tasksForOverrides.add("-PdependencyLock.override=${lookupRequestedModuleIdentifier[dep]}:$overrideLockPropertyVersion")
        }

        def resultForOverrides = runTasks(*tasksForOverrides)


        def tasksForControl = tasksFor(control)
        if (overrideLockFileVersion != null) {
            tasksForControl.add('-PdependencyLock.overrideFile=override.lock')
        }
        if (overrideLockPropertyVersion != null) {
            tasksForControl.add("-PdependencyLock.override=${lookupRequestedModuleIdentifier[dep]}:$overrideLockPropertyVersion")
        }

        def resultForControl = runTasks(*tasksForControl)


        then:
        DocWriter w = new DocWriter(title, insightSource, projectDir)
        w.writeCleanedUpBuildOutput(
                '=== For the overridden dependency ===\n' +
                        "Tasks: ${tasksForOverrides.join(' ')}\n\n" +
                        resultForOverrides.output +
                        '\n\n=== For the control dependency ===\n' +
                        "Tasks: ${tasksForControl.join(' ')}\n\n" +
                        resultForControl.output)
        w.writeProjectFiles()

        verifyOutputForControl(resultForControl.output)

        verifyOutput(resultForOverrides.output, dependencyHelper, dep, w)
        w.writeFooter('completed assertions')


        where:
        insightSource | dep   | staticVersion | lockVersion | overrideLockFileVersion   | overrideLockPropertyVersion | title
//        ===plugins insight===
//        override lock from file
        plugins       | guava | guavaStatic   | guavaLock   | guavaOverrideLockFromFile | null                        | 'override-lock-from-file'
        plugins       | guava | guavaStatic   | guavaLock   | null                      | guavaOverrideLockFromProp   | 'override-lock-from-property'

//        ===core insight===
//        override lock from file
        core          | guava | guavaStatic   | guavaLock   | guavaOverrideLockFromFile | null                        | 'override-lock-from-file'
        core          | guava | guavaStatic   | guavaLock   | null                      | guavaOverrideLockFromProp   | 'override-lock-from-property'
    }

    private static void verifyOutputForControl(String output) {
        def split = output.split('nebula.dependency-recommender')

        def firstSection = split[0]

        assert !firstSection.contains('locked')
        assert !firstSection.contains('nebula.dependency-lock')
        assert !firstSection.contains('override')
    }

    private static void verifyOutput(String output, DependencyHelper dh, String dep, DocWriter w) {
        def expected = dh.results()
        def requestedVersion = dh.findRequestedVersion()

        assert expected.version != null || expected.moduleIdentifierWithVersion != null

        if (dh.lockVersion != null) {
            w.addAssertionToDoc("contains 'locked'")
            assert output.contains('locked')

            def expectedReason = 'nebula.dependency-lock locked with: dependencies.lock'
            w.addAssertionToDoc("contains '$expectedReason'")
            assert output.contains(expectedReason)

            def expectedOutput = "${expected.moduleIdentifier}${requestedVersion} -> ${expected.version}"
            w.addAssertionToDoc("contains $expectedOutput [locked]")
            assert output.contains(expectedOutput)
        }

        if (dh.overrideLockPropertyVersion != null) {
            def expectedReason = "nebula.dependency-lock using override: ${lookupRequestedModuleIdentifier[dep]}:${dh.overrideLockPropertyVersion}"
            w.addAssertionToDoc("contains '$expectedReason'")
            assert output.contains(expectedReason)
        }

        if (dh.overrideLockFileVersion != null) {
            def expectedReason = 'nebula.dependency-lock using override file: override.lock'
            w.addAssertionToDoc("contains '$expectedReason'")
            assert output.contains(expectedReason)
        }

        if (dh.staticVersion != null) {
            def endResultRegex = "Task.*\n.*$expected"
            w.addAssertionToDoc("contains $endResultRegex [static version end result]")
            assert output.findAll { endResultRegex }.size() > 0
        }
    }

}