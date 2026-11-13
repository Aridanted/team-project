## What does this PR do?
Closes #<issue number>

## Layer(s) touched
- [ ] entity
- [ ] use_case
- [ ] interface_adapter
- [ ] view
- [ ] data_access
- [ ] app / docs / infra

## SOLID checklist
- [ ] **S**: each new class has one reason to change
- [ ] **O**: new behaviour was added with new classes, not by editing `if`/`switch` chains
- [ ] **L**: every implementation can replace its interface without surprising callers
- [ ] **I**: interfaces are small and owned by the use case that needs them
- [ ] **D**: dependencies come in through constructors as interfaces; only `app` uses `new` on concrete outer classes

## General checklist
- [ ] `mvn verify` passes locally (Checkstyle, the dependency rule test, and every other test)
- [ ] New logic has unit tests (fakes, no network, no real data files)
- [ ] Public classes and methods have Javadoc
- [ ] README updated if a use case, entity, API or how-to-run changed
- [ ] No API keys, personal paths, or graded lab files committed
- [ ] Screenshot attached if the UI changed

## Notes for the reviewer
