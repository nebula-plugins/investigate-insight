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


class DocWriter {
    String title
    String insightSource
    File projectDir
    File docs

    DocWriter(String title, String insightSource, File projectDir) {
        this.title = title
        this.insightSource = insightSource
        this.projectDir = projectDir

        def up = File.separator + '..'
        def topLevel = new File(projectDir.getPath() + up + up + up + up)
        docs = new File(topLevel, "docs")
        docs.mkdirs()
    }

    def writeCleanedUpBuildOutput(String output) {
        def depFolder = new File(docs, title)
        depFolder.mkdirs()

        def file = new File(depFolder, "${insightSource}.txt")
        file.delete()
        file.createNewFile()

        file << output.replaceAll("nebula.dependency-recommender uses a properties file: .*/investigate-insight",
                "nebula.dependency-recommender uses a properties file: ./investigate-insight")
                .replaceAll('BUILD SUCCESSFUL in .*s', 'BUILD SUCCESSFUL')

        file << """
=== Asserting on... ===
""".stripIndent()
    }

    def addAssertionToDoc(String message) {
        def depFolder = new File(docs, title)
        def file = new File(depFolder, "${insightSource}.txt")

        file << "- $message\n"
    }

    def writeFooter(String first) {
        def depFolder = new File(docs, title)
        def file = new File(depFolder, "${insightSource}.txt")

        file << "\n$first\n"
    }
}
