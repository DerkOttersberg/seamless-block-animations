# Contributing

Use Java 25 and keep changes loader-neutral whenever Minecraft itself exposes the required API. Loader imports belong only in their loader module.

Before submitting a change, run:

```text
./gradlew clean check build
./gradlew :fabric:runClientGameTest
```

For rendering changes, inspect the generated closed, opening, reversing, and final screenshots. Also boot the affected Forge and NeoForge development clients. Do not commit `build`, `run`, IDE, crash-report, or generated files.

Bug reports should include the loader/version, graphics backend, resource packs, relevant log excerpt, and a minimal reproduction sequence.
