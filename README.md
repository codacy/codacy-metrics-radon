# codacy-metrics-radon

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7d43dedda0df444c99c7b0421f1099f4)](https://www.codacy.com/gh/codacy/codacy-metrics-radon?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-metrics-radon&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/7d43dedda0df444c99c7b0421f1099f4)](https://www.codacy.com/gh/codacy/codacy-metrics-radon?utm_source=github.com&utm_medium=referral&utm_content=codacy/codacy-metrics-radon&utm_campaign=Badge_Coverage)
[![CircleCI](https://circleci.com/gh/codacy/codacy-metrics-radon.svg?style=svg)](https://circleci.com/gh/codacy/codacy-metrics-radon)
[![Docker Version](https://images.microbadger.com/badges/version/codacy/codacy-metrics-radon.svg)](https://microbadger.com/images/codacy/codacy-metrics-radon "Get your own version badge on microbadger.com")

This is the docker engine we use at Codacy to have [Radon](https://github.com/rubik/radon) support.

## Requirements

* Python
* Radon
* Java 8+

## Usage

You can create the docker by doing:

```bash
sbt docker:publishLocal
```

The docker is ran with the following command:

```bash
docker run --user=docker --rm=true -v <Source_Directory>:/src codacy-metrics-radon:<docker version>
```

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
