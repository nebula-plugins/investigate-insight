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


import com.google.common.io.Resources

import java.nio.charset.StandardCharsets

abstract class AbstractVerifyInsight extends TestKitSpecification {
    static def plugins = '4.7' // gradle v
    static def core = '4.9-rc-1' // gradle v

    def setup() {
        def localRules = new File(projectDir, 'local-rules.json')
        localRules << readFileFromClasspath('local-rules.json')

        def dependencyRecommendations = new File(projectDir, 'dependency-recommendations.properties')
        dependencyRecommendations << readFileFromClasspath('dependency-recommendations.properties')
    }

    def cleanup() {
    }

    def tasks(String dependencyName) {
        ['dependencyInsight', '--dependency', "${dependencyName}"]
    }

    File createSimpleBuildFile(String insightSource) {
        assert insightSource != null
        def pluginClasspaths
        if (insightSource == plugins) {
            pluginClasspaths = """
                classpath 'com.netflix.nebula:nebula-dependency-recommender:5.1.0'
                classpath "com.netflix.nebula:gradle-resolution-rules-plugin:5.2.2"
                classpath "com.netflix.nebula:gradle-dependency-lock-plugin:5.0.6"
                """.stripIndent()
        } else {
            pluginClasspaths = """
            classpath ('com.netflix.nebula:nebula-dependency-base-plugin:1.0.0-rc.1.dev.0.uncommitted+2998f3d') {
                force = true
            }
            classpath ('com.netflix.nebula:gradle-resolution-rules-plugin:5.3.0-dev.0.uncommitted+5ee5f2c') { // FIXME: update with RC-version
                force = true
            }
            classpath ('com.netflix.nebula:nebula-dependency-recommender:5.2.0-dev.1.uncommitted+74e1bfa') {
                force = true
            }
            classpath ('com.netflix.nebula:gradle-dependency-lock-plugin:5.1.0-dev.7.uncommitted+cf08279') {
                force = true
            }
            """.stripIndent()
        }

        buildFile << """
buildscript {
    repositories {
        mavenLocal()
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        ${pluginClasspaths}
    }
}

apply plugin: 'java'
apply plugin: 'nebula.dependency-lock'
apply plugin: 'nebula.resolution-rules'
apply plugin: 'nebula.dependency-recommender'

repositories {
    jcenter()
}

dependencyRecommendations {
    propertiesFile file: file(getRootDir().getPath() + File.separator + 'dependency-recommendations.properties')
}

dependencies {
    resolutionRules 'com.netflix.nebula:gradle-resolution-rules:latest.release'
    resolutionRules files('local-rules.json') // mostly from https://github.com/nebula-plugins/gradle-resolution-rules-plugin/wiki/Example-Rules-JSON
}
        """.stripIndent()

    }

    String createMainFile() {
        """
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MainDynamic {
    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(MainDynamic.class);
        log.info("Hello, " + ImmutableList.of("friend").get(0));
    }
}
""".stripIndent()
    }

    void createLockfileIfNeeded(String dep, String lockVersion) {
        if (lockVersion != null) {
            createLockfile(dep)
        }
    }

    static def createForceConfigurationIfNeeded(String dep, String forceVersion, Map lookupRequestedModuleIdentifier) {
        def forceConfiguration = ''
        if (forceVersion != null) {
            forceConfiguration = """
                configurations.all {
                    resolutionStrategy {
                        force '${"${lookupRequestedModuleIdentifier[dep]}:${forceVersion}"}'
                    }
                }
                """.stripIndent()
        }
        forceConfiguration
    }

    def createLockfile(String dependencyName) {
        def lockFile = new File(projectDir, "dependencies.lock")
        lockFile << readFileFromClasspath("${dependencyName}-dependencies.lock")
    }

    private static String readFileFromClasspath(String filename) {
        Resources.toString(Resources.getResource(filename), StandardCharsets.UTF_8)
    }
}