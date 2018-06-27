import nebula.test.functional.internal.classpath.ClasspathAddingInitScriptBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

/**
 * Things that really belong in the unfinished Gradle Testkit
 */
abstract class TestKitSpecification extends Specification {
    @Rule
    final TestName testName = new TestName()

    File projectDir
    File buildFile
//    List<File> pluginClasspath
    File settingsFile
    File propertiesFile
    Boolean debug = true
    String moduleName
    String gradleVersion

    /**
     * Automatic addition of `GradleRunner.withPluginClasspath()` _only_ works if the plugin under test is applied using the plugins DSL
     * This enables us to add the plugin-under-test classpath via an init script
     * https://docs.gradle.org/4.6/userguide/test_kit.html#sub:test-kit-automatic-classpath-injection
     */
    boolean definePluginOutsideOfPluginBlock = false

    def setup() {
        projectDir = new File("build/nebulatest/${this.class.canonicalName}/${testName.methodName.replaceAll(/\W+/, '-')}").absoluteFile
        if (projectDir.exists()) {
            projectDir.deleteDir()
        }
        projectDir.mkdirs()

        buildFile = new File(projectDir, 'build.gradle')
        settingsFile = new File(projectDir, 'settings.gradle')
        propertiesFile = new File(projectDir, 'gradle.properties')

//        def pluginClasspathResource = getClass().classLoader.findResource('plugin-classpath.txt')
//        if (pluginClasspathResource == null) {
//            throw new IllegalStateException('Did not find plugin classpath resource, run `cCM` build task.')
//        }

        moduleName = findModuleName()
//        pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
    }

    def runTasksSuccessfully(String... tasks) {
//        def pluginArgs = definePluginOutsideOfPluginBlock ? createGradleTestKitInitArgs() : new ArrayList<>()
        def pluginArgs = new ArrayList<>()

        def gradleRunnerBuilder = GradleRunner.create()
                .withDebug(debug)
                .withProjectDir(projectDir)
                .withArguments(*(tasks + pluginArgs + '--stacktrace'))
//                .withPluginClasspath(pluginClasspath)

        if (gradleVersion != null) {
            gradleRunnerBuilder.withGradleVersion(gradleVersion)
        }

        def result = gradleRunnerBuilder.build()

        tasks.each { task ->
            if (!task.contains('-P') && !task.contains('--')) {
                def modTask = task.startsWith(':') ? task : ":$task"
                def outcome = result.task(modTask).outcome
                assert outcome == SUCCESS || outcome == UP_TO_DATE
            }
        }

        result
    }

    def runTasks(String... tasks) {
//        def pluginArgs = definePluginOutsideOfPluginBlock ? createGradleTestKitInitArgs() : new ArrayList<>()
        def pluginArgs = new ArrayList<>()

        def gradleRunnerBuilder = GradleRunner.create()
                .withDebug(debug)
                .withProjectDir(projectDir)
                .withArguments(*(tasks + pluginArgs + '--stacktrace'))
//                .withPluginClasspath(pluginClasspath)

        if (gradleVersion != null) {
            gradleRunnerBuilder.withGradleVersion(gradleVersion)
        }

        gradleRunnerBuilder.build()
    }

    def tasksWereSuccessful(BuildResult result, String... tasks) {
        tasks.each { task ->
            if (!task.contains('-P') && !task.contains('--')) {
                def modTask = task.startsWith(':') ? task : ":$task"
                def outcome = result.task(modTask).outcome
                assert outcome == SUCCESS || outcome == UP_TO_DATE
            }
        }
    }

    def runTasksFail(String... tasks) {
//        def pluginArgs = definePluginOutsideOfPluginBlock ? createGradleTestKitInitArgs() : new ArrayList<>()
        def pluginArgs = new ArrayList<>()

        def gradleRunnerBuilder = GradleRunner.create()
                .withDebug(debug)
                .withProjectDir(projectDir)
                .withArguments(*(tasks + pluginArgs))
//                .withPluginClasspath(pluginClasspath)

        if (gradleVersion != null) {
            gradleRunnerBuilder.withGradleVersion(gradleVersion)
        }

        return gradleRunnerBuilder.buildAndFail()
    }

    File addSubproject(String name) {
        File subprojectDir = new File(projectDir, name)
        subprojectDir.mkdirs()
        settingsFile << "include '$name'\n"
        return subprojectDir
    }

    File addSubproject(String name, String buildGradleContents) {
        def subprojectDir = addSubproject(name)
        new File(subprojectDir, 'build.gradle').text = buildGradleContents
        return subprojectDir
    }

    def dependencies(File _buildFile, String... confs = ['compile', 'testCompile']) {
        _buildFile.text.readLines()
                .collect { it.trim() }
                .findAll { line -> confs.any { c -> line.startsWith(c) } }
                .collect { it.split(/\s+/)[1].replaceAll(/['"]/, '') }
                .sort()
    }

    def createJavaSourceFile() {
        createJavaSourceFile('public class Main {}')
    }

    def createJavaSourceFile(String source) {
        createJavaSourceFile(projectDir, source)
    }

    def createJavaSourceFile(File projectDir) {
        createJavaSourceFile(projectDir, 'public class Main {}')
    }

    def createJavaSourceFile(File projectDir, String source) {
        createJavaFile(projectDir, source, 'src/main/java')
    }

    def createJavaTestFile(File projectDir, String source) {
        createJavaFile(projectDir, source, 'src/test/java')
    }

    def createJavaTestFile(String source) {
        createJavaTestFile(projectDir, source)
    }

    def createJavaTestFile(File projectDir) {
        createJavaTestFile(projectDir, '''
            import org.junit.Test;
            public class Test1 {
                @Test
                public void test() {}
            }
        '''.stripIndent())
    }

    def createJavaFile(File projectDir, String source, String sourceFolderPath) {
        def sourceFolder = new File(projectDir, sourceFolderPath)
        sourceFolder.mkdirs()
        new File(sourceFolder, fullyQualifiedName(source).replaceAll(/\./, '/') + '.java').text = source
    }

    static String fullyQualifiedName(String sourceStr) {
        def pkgMatcher = sourceStr =~ /\s*package\s+([\w\.]+)/
        def pkg = pkgMatcher.find() ? pkgMatcher[0][1] + '.' : ''

        def classMatcher = sourceStr =~ /\s*(class|interface)\s+(\w+)/
        return classMatcher.find() ? pkg + classMatcher[0][2] : null
    }

    private String findModuleName() {
        getProjectDir().getName().replaceAll(/_\d+/, '')
    }

    private List<String> createGradleTestKitInitArgs() {
        File testKitDir = new File(projectDir, ".gradle-test-kit")
        if (!testKitDir.exists()) {
            GFileUtils.mkdirs(testKitDir)
        }

        File initScript = new File(testKitDir, "init.gradle")
        ClassLoader classLoader = this.getClass().getClassLoader()
        def classpathFilter = nebula.test.functional.GradleRunner.CLASSPATH_DEFAULT
        ClasspathAddingInitScriptBuilder.build(initScript, classLoader, classpathFilter)

        return Arrays.asList("--init-script", initScript.getAbsolutePath())
    }

}
