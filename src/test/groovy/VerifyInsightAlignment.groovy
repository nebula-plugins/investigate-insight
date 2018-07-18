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

class VerifyInsightAlignment extends AbstractVerifyInsight {
    static String slf4jApi = 'slf4jApi'
    static String slf4jSimple = 'slf4jSimple'
    static String slf4jApiDependency = 'org.slf4j:slf4j-api'
    static String slf4jSimpleDependency = 'org.slf4j:slf4j-simple'
    static String slf4jApiStatic = '1.6.0'
    static String slf4jSimpleStatic = '1.7.20'
    static String slf4jApiForce = '1.5.0'
    static String slf4jSimpleForce = '1.5.5'
    static String slf4jApiLock = '1.7.1'
    static String slf4jSimpleLock = '1.7.2'
    static String slf4jApiRec = '1.7.25'

    public static
    def lookup = ImmutableMap.of(slf4jApi, slf4jApiDependency, slf4jSimple, slf4jSimpleDependency)

    @Unroll
    def "#title - #insightSource"() {
        given:
        createSimpleBuildFile(insightSource)
        gradleVersion = insightSource

        createLockfileIfNeeded(first, firstLockVersion)

        def dependencyHelper = new DependencyHelper()
        dependencyHelper.staticVersion = firstStaticVersion
        dependencyHelper.recommendedVersion = firstRecVersion

        def firstRequestedVersion = dependencyHelper.findRequestedVersion() // static, dynamic, or recommended

        buildFile << """
dependencies {
    compile '${lookup[first]}${firstRequestedVersion}'
    compile '${lookup[second]}:${secondStaticVersion}'
    compile 'com.google.guava:guava:18.0'
}
${createForceConfigurationIfNeeded(first, firstForceVersion, lookup)}
${createForceConfigurationIfNeeded(second, secondForceVersion, lookup)}
""".stripIndent()

        createJavaSourceFile(projectDir, createMainFile())
        def tasks = tasksFor('slf4j')

        when:
        def result = runTasks(*tasks)
        def output = result.output

        then:
        DocWriter w = new DocWriter(title, insightSource, projectDir)

        w.writeCleanedUpBuildOutput('=== For the dependency under test ===\n' +
                "Tasks: ${tasks.join(' ')}\n\n" +
                output)
        w.writeProjectFiles()

        // assert on final version
        // FIXME: note: aligns use force > locks, but no other rules do this
        if (firstForceVersion != null) {
            def firstRegex = "${lookup[first]}.*-> ${firstForceVersion}"
            w.addAssertionToDoc("contains $firstRegex [align & force]")
            assert output.findAll(firstRegex).size() > 0

            def secondRegex = "${lookup[second]}.*-> ${firstForceVersion}"
            w.addAssertionToDoc("contains $secondRegex [align & force]")
            assert output.findAll(secondRegex).size() > 0 // aligned to first force
        } else if (firstLockVersion != null) {
            def firstRegex = "${lookup[first]}.*-> ${secondLockVersion}"
            w.addAssertionToDoc("contains $firstRegex [align & lock]")
            assert output.findAll(firstRegex).size() > 0 // aligned to higher lock version

            def secondRegex = "${lookup[second]}.*-> ${secondLockVersion}"
            w.addAssertionToDoc("contains $secondRegex [align & lock]")
            assert output.findAll(secondRegex).size() > 0
        } else if (firstStaticVersion != null) {
            def firstRegex = "${lookup[first]}:$firstStaticVersion -> $secondStaticVersion"
            w.addAssertionToDoc("contains $firstRegex [align & static]")
            assert output.findAll(firstRegex).size() > 0 // aligned to higher static

            def secondRegex = "${lookup[second]}:$secondStaticVersion"
            w.addAssertionToDoc("contains $secondRegex [align & static]")
            assert output.findAll(secondRegex).size() > 0
        } else {
            def firstExpected = "${lookup[first]} -> ${firstRecVersion}"
            w.addAssertionToDoc("contains $firstExpected [align & rec]")
            assert output.contains(firstExpected)

            def secondExpected = "${lookup[second]}:$secondStaticVersion -> ${firstRecVersion}"
            w.addAssertionToDoc("contains $secondExpected [align & rec]")
            assert output.contains(secondExpected) // aligned to recommended
        }

        // assert on supporting causes
        if (firstForceVersion == null) { // FIXME: should happen for all cases
            def orKeywords = ['aligned to', 'By conflict resolution']
            w.addAssertionToDoc("contains '${orKeywords[0]}' or '${orKeywords[1]}'")
            assert output.contains(orKeywords[0]) || output.contains(orKeywords[1])
        }

        if (firstLockVersion != null) {
            def expectedOutput = 'locked'
            w.addAssertionToDoc("contains $expectedOutput [align & lock]")
            assert output.contains(expectedOutput)
        }

        if (firstForceVersion != null) {
            def orKeywords = ['forced', 'Forced']
            w.addAssertionToDoc("contains '${orKeywords[0]}' or '${orKeywords[1]}'")
            assert output.contains(orKeywords[0]) || output.contains(orKeywords[1])
            // FIXME: this should happen
        }

        if (firstStaticVersion != null) {
            if (insightSource == core) {
                def expectedOutput = 'Was requested'
                w.addAssertionToDoc("contains $expectedOutput [align & static - core]")
                assert output.contains(expectedOutput)
            }
        }

        if (firstRecVersion != null) {
            // FIXME: this should happen. It does not for nebula plugins or core gradle.
            def orKeywords = ["Recommending version ${firstRecVersion} for dependency", 'recommend']
            w.addAssertionToDoc("contains '${orKeywords[0]}' or '${orKeywords[1]}' before footer [align & rec]")

            def split = output.split('nebula.dependency-recommender')
            def firstSection = split[0]
            assert firstSection.contains(orKeywords[0]) || firstSection.contains(orKeywords[1])
        }


        w.writeFooter('completed assertions')

        where:
        insightSource | first    | second      | firstStaticVersion | secondStaticVersion | firstRecVersion | firstForceVersion | secondForceVersion | firstLockVersion | secondLockVersion | title
//        alignment - static 
        plugins       | slf4jApi | slf4jSimple | slf4jApiStatic     | slf4jSimpleStatic   | null            | null              | null               | null             | null              | 'alignment-static'
        plugins       | slf4jApi | slf4jSimple | slf4jApiStatic     | slf4jSimpleStatic   | null            | slf4jApiForce     | slf4jSimpleForce   | null             | null              | 'alignment-static-force'
        plugins       | slf4jApi | slf4jSimple | slf4jApiStatic     | slf4jSimpleStatic   | null            | slf4jApiForce     | slf4jSimpleForce   | slf4jApiLock     | slf4jSimpleLock   | 'alignment-static-force-lock'
        plugins       | slf4jApi | slf4jSimple | slf4jApiStatic     | slf4jSimpleStatic   | null            | null              | null               | slf4jApiLock     | slf4jSimpleLock   | 'alignment-static-lock'
//        alignment - with recommendation
        plugins       | slf4jApi | slf4jSimple | null               | slf4jSimpleStatic   | slf4jApiRec     | null              | null               | null             | null              | 'alignment-rec'
        plugins       | slf4jApi | slf4jSimple | null               | slf4jSimpleStatic   | slf4jApiRec     | slf4jApiForce     | slf4jSimpleForce   | null             | null              | 'alignment-rec-force'
        plugins       | slf4jApi | slf4jSimple | null               | slf4jSimpleStatic   | slf4jApiRec     | slf4jApiForce     | slf4jSimpleForce   | slf4jApiLock     | slf4jSimpleLock   | 'alignment-rec-force-lock'
        plugins       | slf4jApi | slf4jSimple | null               | slf4jSimpleStatic   | slf4jApiRec     | null              | null               | slf4jApiLock     | slf4jSimpleLock   | 'alignment-rec-lock'

//        alignment - static
        core          | slf4jApi | slf4jSimple | slf4jApiStatic     | slf4jSimpleStatic   | null            | null              | null               | null             | null              | 'alignment-static'
        core          | slf4jApi | slf4jSimple | slf4jApiStatic     | slf4jSimpleStatic   | null            | slf4jApiForce     | slf4jSimpleForce   | null             | null              | 'alignment-static-force'
        core          | slf4jApi | slf4jSimple | slf4jApiStatic     | slf4jSimpleStatic   | null            | slf4jApiForce     | slf4jSimpleForce   | slf4jApiLock     | slf4jSimpleLock   | 'alignment-static-force-lock'
        core          | slf4jApi | slf4jSimple | slf4jApiStatic     | slf4jSimpleStatic   | null            | null              | null               | slf4jApiLock     | slf4jSimpleLock   | 'alignment-static-lock'
//        alignment - with recommendation
        core          | slf4jApi | slf4jSimple | null               | slf4jSimpleStatic   | slf4jApiRec     | null              | null               | null             | null              | 'alignment-rec'
        core          | slf4jApi | slf4jSimple | null               | slf4jSimpleStatic   | slf4jApiRec     | slf4jApiForce     | slf4jSimpleForce   | null             | null              | 'alignment-rec-force'
        core          | slf4jApi | slf4jSimple | null               | slf4jSimpleStatic   | slf4jApiRec     | slf4jApiForce     | slf4jSimpleForce   | slf4jApiLock     | slf4jSimpleLock   | 'alignment-rec-force-lock'
        core          | slf4jApi | slf4jSimple | null               | slf4jSimpleStatic   | slf4jApiRec     | null              | null               | slf4jApiLock     | slf4jSimpleLock   | 'alignment-rec-lock'
    }

}