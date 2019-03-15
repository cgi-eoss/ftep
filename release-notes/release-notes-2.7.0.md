# Release notes for F-TEP 2.7.0

This release of F-TEP exposes Batch Processing functionality to users,
and resolves several issues with the 2.6.x releases.

## Improvements, Changes &amp; Fixes

* Improve UI for multiple job configruation input values
* Parallel processing:
  * Remove unused 'parallel_processor' service type
  * Allow service developers to mark input parameters as 'parallel'
  * Allow users to launch an appropriately-configured service with
    parallel inputs expanding the configuration to create a batch of
    jobs
* Several fixes for sharing and permissions evaluation
* Improve logging of job execution
* Improve CREODIAS data resolver reliability

For a comprehensive log of all changes, please visit the F-TEP source
repository.

