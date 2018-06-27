# Testing `dependencyInsight` task

## Purpose

Nebula has been using [nebula-plugins/nebula-dependency-base-plugin](https://github.com/nebula-plugins/nebula-dependency-base-plugin/releases) to generate a custom `dependencyInsight` task. 

Gradle has changed their core `dependencyInsight` task, and this is some investigation on what occurs in situations where we move from Nebula's custom to Gradle's core features. 

We are testing the insight for dependencies with versions from... 

- Static configuration
- Dynamic configuration
- A lockfile
- A forced override
- A BOM recommendation
- A replacement rule
- A substitution rule
- An alignment rule

#### Under test
See the following configurations:
- With [guava](guava.md)
- With [mockito](mockito.md)
- With [slf4j](slf4j.README.md)

## Steps

`./gradlew test`

View the differences at `docs/<taskName>/<insight-from>.txt`

When all the tests succeed, the files will end with the line `completed assertions`

#### Viewing the differences, hints!

- See all files printed, with headers: 
```
find docs -type f | xargs tail -n +1
```

- View side-by-side in an IDE

## Results

#### Ordering
- Substitution, replacement
- Lock
- Alignment
- Force
- Static, dynamic
- Recommendation

#### More information wanted...
- When forced or locked are not the last step, they should still be listed in the Selection Reasons. Want: `Forced to 'x' version`
    - `static-force-lock`
    - `dynamic-force-lock`
    - `rec-force-lock`
    - `replacement-static-force-lock` 
    - `replacement-dynamic-force-lock` 
    - `replacement-rec-force-lock`
    - `alignment-static-force-lock`
    - `alignment-rec-force-lock`
        
#### Unexpected results
- Forces & locks have different priority order
  - `alignment-static-force-lock` and `alignment-static-force` (force wins) vs `dynamic-force-lock` and `dynamic-force` (lock wins)
- Not listing the recommended version
  - `alignment-rec`
  - `alignment-rec-force`
  - `alignment-rec-force-lock`
  - `alignment-rec-lock`
