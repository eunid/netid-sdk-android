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
- Claims are now a string
- Reworked redirect handling, now working with beta id app
- Removed deprecated variable
- Do not display empty values in userinfo
- Remove debug flag to find id apps regardless if they are usable or not
- Implement ui feedback
- Refined JSON handling
- Changed some wording
- Renamed soft/hard to permission/login
- Make subclaims optional, too

### Fixed
- Fixed UserInfo json parser error
- Fixed a bug with the soft flow if only one app was installed
- Fixed a problem with finding the activities of a package
- Fixed wrong scopes for hard/soft flow
- Fixed a bug with the soft flow if only one app was installed
- Fixed a bug with cathcing the redirect
- Fixed a bug with too many scopes
- Fixed a bug with not setting the istandard claims
- Fixed a bug when some claims were missing
- Only set claims in login and login+permission flows
- Make get/update permission possible in permission flow
- Fixed possible exception during permission fetch
- Set correct value for app name
- Fixed a crash in the context of the bottom fragment
 
