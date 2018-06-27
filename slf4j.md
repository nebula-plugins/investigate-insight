Some configuration of the following:
```
dependencies {
    compile 'org.slf4j:slf4j-api:1.6.0' // static version v1.6.0 or bom recommendations slf4j version v1.7.25
    compile 'org.slf4j:slf4j-simple:1.7.20'
    compile 'com.google.guava:guava:18.0'
}

configurations.all {
    resolutionStrategy {
        force 'org.slf4j:slf4j-api:1.5.0' // force slf4j-api version: v1.5.0
        force 'org.slf4j:slf4j-simple:1.5.5' // force slf4j-simple version: v1.5.5
    }
}

// bom recommendations slf4j version v1.7.25

// lockfile slf4j version: api - v1.7.1 & simple - v1.7.2
```
