Some configuration of the following:
```
dependencies {
    compile 'org.slf4j:slf4j-api:1.7.25'
    compile 'org.slf4j:slf4j-simple:1.7.25'
    compile 'com.google.guava:guava:18.0'
    compile 'org.mockito:mockito-all:1.8.0' // static mockito version: v1.8.0 or dynamic version v1.8.+ or bom recommendation
}

configurations.all {
    resolutionStrategy {
        force 'org.mockito:mockito-all:1.10.17' // force mockito version: v1.10.17
    }
}

// bom recommendations mockito-core version: v1.9.5

// lockfile mockito-core version: v2.1.0

// substitution rule mockito-core version: v1.10.19
```
