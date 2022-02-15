# Changelog
All notable eosc-kc changes of Keycloak will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For Keycloak upstream changelog please see https://www.keycloak.org/docs/latest/release_notes/index.html.
[Keycloak announcement for version 16.1.0](https://www.keycloak.org/2021/12/keycloak-1610-released)
Full Keycloak upstream jira issue can be shown if filtered by Fix version. 

## [v16.1.0-rc1.0.1] - 2022-02-15
### Changed
- offline_access scope return always refresh_token [RCIAM-744](https://jira.argo.grnet.gr/browse/RCIAM-744)

## [v16.1.0-rc1.0.0] - 2022-02-14
### Added
- Support for SAML IdP Federation
- User reaccepting Terms and Conditions. [EOSC-KC-48](https://github.com/eosc-kc/keycloak/issues/48)
- Terms and Conditions - periodic reset for all realm users. [EOSC-KC-49](https://github.com/eosc-kc/keycloak/issues/49)
- Email notification for add/remove group. [EOSC-KC-75](https://github.com/eosc-kc/keycloak/issues/75)
- View groups from Account Console. [EOSC-KC-61](https://github.com/eosc-kc/keycloak/issues/61)
- Identity Providers pager in Admin Console. [EOSC-KC-73](https://github.com/eosc-kc/keycloak/issues/73)
- Identity Providers pager in Linked Accounts of Account Console. [EOSC-KC-50](https://github.com/eosc-kc/keycloak/issues/50)
- Javascript SAML identity provider mapper. [KEYCLOAK-17685](https://issues.redhat.com/browse/KEYCLOAK-17685)
- SAML/ OIDC Identity Provider AutoUpdate. [EOSC-KC-119](https://github.com/eosc-kc/keycloak/issues/119)
- New release created on tag
- The idpLoginFullUrl common attribute passed to the ftl templates for any theme except from the default
- Include claim in token introspection response only [RCIAM-742](https://jira.argo.grnet.gr/browse/RCIAM-742)
- Device Authorization Grant with PKCE [KEYCLOAK-9710](https://github.com/keycloak/keycloak/issues/9710)
- External introspection endpoint [EOSC-KC-140](https://github.com/eosc-kc/keycloak/issues/140)

### Changed
- Increase User Attribute Value length to 4000 [EOSC-KC-132](https://github.com/eosc-kc/keycloak/issues/132)
- change emailVerified User field with UserAttributeMappers (conditional trust email). [EOSC-KC-70](https://github.com/eosc-kc/keycloak/issues/70)
- FreeMarkerLoginFormsProvider now has an additional common attribute passed to the ftl templates, the "uriInfo"
- Consent extension [RCIAM-791](https://jira.argo.grnet.gr/browse/RCIAM-791)

### Fixed
- Configure attribute name format in SAML UserAttribute mapper [EOSC-KC-121](https://github.com/eosc-kc/keycloak/issues/121)
- Fix scope bug in device authorization request [RCIAM-783](https://jira.argo.grnet.gr/browse/RCIAM-783)
