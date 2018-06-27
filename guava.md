Some configuration of the following:
```
dependencies {
    compile 'org.slf4j:slf4j-api:1.7.25'
    compile 'org.slf4j:slf4j-simple:1.7.25'
    compile 'com.google.guava:guava:18.0' // static guava version: v18.0 or dynamic version v18.+ or resolution away from google-collections
}

configurations.all {
    resolutionStrategy {
        force 'com.google.guava:guava:14.0.1' // force guava version: v14.0.1
    }
}

// bom recommendations guava version: v19.0

// lockfile guava version: v13.0
```