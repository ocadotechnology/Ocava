# Development Process

Please note that this library is primarily developed on an internal, private repository.  This means that issue 
numbers referenced in commit messages refer to the issue numbers in the internal repository.

# Contributing
Branch, edit, push, submit a merge request. Please squash commits on merging into master.

We would encourage users to contribute a variety of domain-independent utilities which fit into the broad theme of
creating, running and using simulations.

All new code added to this project, or changes made to existing code, should be committed with tests.

Effort should be made to ensure that code added to this project is in a state where it is easy to understand and use,
especially where it may use non-standard or complex patterns.
This may be achieved though example code (eg in unit tests) or javadocs. 

## Commit messages
You must follow commit message formats used for automated releases. They are enforced by push rules.
This does mean that commits visible on the GitHub repository will contain references to internal Ocado issue tracking systems.

This repository makes use of [Semantic-Release](https://github.com/semantic-release/semantic-release) and changes must follow
the following format (based loosely on [Angular](https://github.com/angular/angular.js/blob/master/DEVELOPERS.md#-git-commit-guidelines)):

A commit message consists of a **header**, **body** and **footer**. The header has a **type**, **scope** and **subject**

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The **header** is mandatory and the **scope** of the header is optional. 

### Examples

**Minor release**

```
feat: Add a new feature to project #xxxx

New feature is really useful for reason.
```

**Documentation update**

```
docs: Improve the javadoc on MyClass JIRA-XXXX

Add two @throws lines to the javadoc of myMethod
```

**Breaking Change**

```
feat: Delete a deprecated feature from project JIRA-XXXX

BREAKING CHANGE:  Delete some deprecated methods
```

### Type

If the prefix is `feat`, `fix` or `perf`, it will appear in the changelog. However if there is any BREAKING CHANGE, the commit will always appear in the changelog.

These types will trigger a release

* **feat** - A new feature (MINOR release)
* **fix** - A bug fix (PATCH release)
* **perf** - A performance improvement (PATCH release)

_note_: if marked as breaking change (by including the text "BREAKING_CHANGE" in the body) it will be a MAJOR release

These types will not trigger a release

* **build** - Changes to the build (updated dependencies for example)
* **ci** - Changes to the continuous integration pipeline
* **docs** - Additional documentation changes
* **refactor** - Refactoring of code (with no new features or bug fixes)
* **style** - Correction of code style
* **test** - Additional tests
* **chore**