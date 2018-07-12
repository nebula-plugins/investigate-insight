import org.gradle.api.GradleException

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

class DependencyHelper {
    String requestedModuleIdentifier
    String staticVersion
    String dynamicVersion
    String recommendedVersion
    String forceVersion
    String lockVersion
    String overrideLockFileVersion
    String overrideLockPropertyVersion
    String versionForDynamicToResolveTo

    String replaceFrom
    String substituteWith

    String resolvedModuleIdentifier

    Map lookupRequestedModuleIdentifier


    def results() {
        if (substituteWith != null) {
            return new Coordinate(substituteWith)
        }
        if (replaceFrom != null) {
            resolvedModuleIdentifier = lookupRequestedModuleIdentifier[requestedModuleIdentifier]
        } else {
            resolvedModuleIdentifier = requestedModuleIdentifier
        }
        if (lockVersion != null) {
            if (overrideLockFileVersion != null) {
                return new Coordinate(resolvedModuleIdentifier, overrideLockFileVersion)
            }
            if (overrideLockPropertyVersion != null) {
                return new Coordinate(resolvedModuleIdentifier, overrideLockPropertyVersion)
            } // this logic will have a problem if we test using both an override property & file
            return new Coordinate(resolvedModuleIdentifier, lockVersion)
        }
        if (forceVersion != null) {
            return new Coordinate(resolvedModuleIdentifier, forceVersion)
        }
        if (staticVersion != null) {
            return new Coordinate(resolvedModuleIdentifier, staticVersion)
        }
        if (recommendedVersion != null) {
            return new Coordinate(resolvedModuleIdentifier, recommendedVersion)
        }
        return new Coordinate(resolvedModuleIdentifier, versionForDynamicToResolveTo)
    }

    String findRequestedVersion() {
        def listedVersion
        if (recommendedVersion != null) {
            listedVersion = ''
        } else {
            if (staticVersion != null) {
                listedVersion = ":${staticVersion}"
            } else if (dynamicVersion != null) {
                listedVersion = ":${dynamicVersion}"
            } else {
                throw new GradleException("must list a version")
            }
        }
        listedVersion
    }
}
