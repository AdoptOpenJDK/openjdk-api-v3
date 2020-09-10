# 2. Branching & Release Strategy

Date: 2020-08-29

## Status

Accepted

## Context

There are two main source code branches in the API project, `staging` and `master`.

The `staging` branch is built and deployed to the pre-production staging environment when new changes are merged.
The deployment is hosted on OpenShift and available at <https://staging-api.adoptopenjdk.net/>. 
The head of the `staging` branch represents the source code version for the current pre-production "staging" environment.

The `master` branch is built and deployed to the production environment when new changes are merged.
The deployment is hosted on OpenShift and available at <https://api.adoptopenjdk.net/>. 
The head of the `master` branch represents the source code version for the current production deployment.

The [instructions for contributing](https://github.com/AdoptOpenJDK/openjdk-api-v3/blob/4a122a05a904e851083643648f005556d30e9271/CONTRIBUTING.md#source-code-management-and-branching) to 
the project suggest creating a Pull Request (PR) for any significant change against the `staging` branch.
This allows those changes to be trialled in the production-like staging environment before being merged to `master`.
The definition of a significant change however is ambiguous.

Various non-significant changes have PRs raised directly against `master`.

This results in a drift between `master` and `staging`, sometimes with conflicting source trees between the 2 branches. 
This leads to a relatively manual and complex process to release new changes to production. 
The process includes:

- choosing the commits to merge from `staging` to `master`
- creating a PR to merge `staging` commits to `master`
- re-synchronising `staging` with `master`

As well as being a sub-optimal process, the drift between `staing` and `master` introduces some risk that changes are developed against a version of the code that does not represent the production state.

For new contributors to the project it makes it more complicated as they need to raise PRs against the `staging` branch rather than the default of `master`.

The purpose of the `staging` branch however does offer some value for truly experimental changes that we are unsure how they will work in a real environment, or unsure whether we actually want to include them.
These experimental changes are in fact fairly uncommon and therefore potentially don't warrant a dedicated branch and environment for this purpose.

## Decision

We will:

- instruct contributors to raise all PRs against the `master` branch (via [CONTRIBUTING.md](../../CONTRIBUTING.md))
- automatically build and deploy the `master` branch to the pre-production staging environment (<https://staging-api.adoptopenjdk.net/>)
- create a new `production`/`release` branch that will be automatically build and deployed to the production environment (<https://api.adoptopenjdk.net/>)
- perform a production release by raising a PR to synchronise the `master` branch with the `production`/`release` branch

## Consequences

The consequences of this are:

- the current OpenShift deployment process will need to be updated to pull from the `master` branch for staging and `production`/`release` branch for production
- the pre-production staging will not longer be used for experimental changes
- experimental changes will be handled on an ad-hoc basis
- if deemed worthy of a deployment to a production-like environment a decision will need to be made whether the change should be merged to `master` and deployed to the pre-production staging environment or not  
