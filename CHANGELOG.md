# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Inital commit
- Permission management webservices and API functions
- AuthorizationView screen design alignment
- API function to transmit a id token
- Hard-Login flow implementation
- Copyright 
- Possibility to transfer claims via the sdk interface
- Save states when switching orientation
- Enabled app2app workflow
- Enabled handling of redirect uri
- Added third flow option login + permission
- Added parameter to forece app2app flow

### Changed
- Renamed NetID to netID
- Changed call for opening id app
- Added parameters to verified app link
- Renamed hard and soft flow to login and permission
- Implemented new design

### Fixed
- Fixed UserInfo json parser error
- Fixed a bug with the soft flow if only one app was installed
- Fixed a problem with finding the activities of a package
- Fixed wrong scopes for hard/soft flow
- Fixed a bug with the soft flow if only one app was installed

