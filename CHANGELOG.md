Change Log
==========

All notable changes to Fili will be documented here. Changes are accumulated as new paragraphs at the top of the current 
major version. Each change has a link to the issue that triggered the change, and to the pull request that makes the
change.

Current
-------

### Added:


### Deprecated:


### Removed:


### Changed:

[Reorganizes asynchronous package structure](https://github.com/yahoo/fili/pull/19)
    * The `jobs` package is renamed to `async` and split into the following subpackages:
        - `broadcastchannels` - Everything dealing with broadcast channels
        - `jobs` - Everything related to `jobs`, broken into subpackages
            * `jobrows` - Everything related to the content of the job metadata
            * `payloads` - Everything related to building the version of the job metadata to send to the user
            * `stores` - Everything related to the databases for job data
        - `preresponses` - Everything related to `PreResponses`, broken into subpackages
            * `stores` - Everything related to the the databases for PreResponse data
        - `workflows` - Everything related to the asynchronous workflow
### Fixed:


### Known Issues:

