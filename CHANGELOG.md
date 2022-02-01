# Changelog
All notable eosc-kc changes of Keycloak will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For Keycloak upstream changelog please see https://www.keycloak.org/docs/latest/release_notes/index.html.
Full Keycloak upstream jira issue can be shown if filtered by Fix version. For example [Keycloak jira issue for 15.0.2 version](https://issues.redhat.com/browse/KEYCLOAK-19161?jql=project%20%3D%20keycloak%20and%20fixVersion%20%3D%2015.0.2)

## [v15.0.2-rc1.1.0] - 2022-02-01
### Added
- Device Authorization Grant with PKCE [KEYCLOAK-9710](https://github.com/keycloak/keycloak/issues/9710)
- external introspection endpoint [EOSC-KC-140](https://github.com/eosc-kc/keycloak/issues/140)
### Fixed
- Fix scope bug in device authorization request [RCIAM-783](https://jira.argo.grnet.gr/browse/RCIAM-783)
- Fix SAML federation enable save button bug [RCIAM-796](https://jira.argo.grnet.gr/browse/RCIAM-796)


## [v15.0.2-rc1.0.9] - 2022-01-28
### Added
- Added an xslt overriding mechanism on the export function of the federation's SP description xml. [EOSC-KC-147](https://github.com/eosc-kc/keycloak/issues/147)
### Fixed
- Fixed a possible db connection leak while updating federation's IdPs.

## [v15.0.2-rc1.0.8] - 2022-01-05
### Added
- Include claim in token introspection response only [RCIAM-742](https://jira.argo.grnet.gr/browse/RCIAM-742)

## [v15.0.2-r1.0.7] - 2021-12-23
### Fixed
- Verify fine-grained admin permissions feature is enabled before checking fine-grained permissions when creating users. [KEYCLOAK CVE-2021-4133](https://www.keycloak.org/2021/12/cve.html)

## [v15.0.2-r1.0.6] - 2021-12-10
### Added
- SAML Federation sync mode [EOSC-KC-144](https://github.com/eosc-kc/keycloak/issues/144)

### Changed
- Increase User Attribute Value length to 4000 [EOSC-KC-132](https://github.com/eosc-kc/keycloak/issues/132)
- Improve implementation in attribute Identity Provider mapper taking into account emailVerified User field. [EOSC-KC-70](https://github.com/eosc-kc/keycloak/issues/70)
- FreeMarkerLoginFormsProvider now has an additional common attribute passed to the ftl templates, the "uriInfo"

### Fixed
- Fixed SAML principals tooltip

## [v15.0.2-r1.0.5] - 2021-11-16
### Added
-the idpLoginFullUrl common attribute passed to the ftl templates for any theme except from the default

## [v15.0.2-r1.0.4] - 2021-11-04
### Added
- Attribute Identity Provider mapper taking into account emailVerified User field. [EOSC-KC-70](https://github.com/eosc-kc/keycloak/issues/70)

### Fixed
- Fix a bug for Identity Providers in ModelToRepresentation

## [v15.0.2-r1.0.3] - 2021-10-18
### Added
- new release created on tag

## [v15.0.2-r1.0.2] - 2021-10-05
### Fixed
- Brokered SAML logins fail due to wrong InResponseTo in 15.x. [KEYCLOAK-19143](https://issues.redhat.com/browse/KEYCLOAK-19143)

## [v15.0.2-r1.0.1] - 2021-09-29
### Added
- Support for SAML IdP Federation
- User reaccepting Terms and Conditions. [EOSC-KC-48](https://github.com/eosc-kc/keycloak/issues/48)
- Terms and Conditions - periodic reset for all realm users. [EOSC-KC-49](https://github.com/eosc-kc/keycloak/issues/49)
- Email notification for add/remove group. [EOSC-KC-75](https://github.com/eosc-kc/keycloak/issues/75)
- View groups from Account Console. [EOSC-KC-61](https://github.com/eosc-kc/keycloak/issues/61)
- Show additional SAML & OIDC client metadata in consent screen. [EOSC-KC-87](https://github.com/eosc-kc/keycloak/issues/87)
- Create a minimal realm representation and use it in Admin Console. [EOSC-KC-93](https://github.com/eosc-kc/keycloak/issues/93)
- Identity Providers pager in Admin Console. [EOSC-KC-73](https://github.com/eosc-kc/keycloak/issues/73)
- Identity Providers pager in Linked Accounts of Account Console. [EOSC-KC-50](https://github.com/eosc-kc/keycloak/issues/50)
- Javascript SAML identity provider mapper. [KEYCLOAK-17685](https://issues.redhat.com/browse/KEYCLOAK-17685)
- Conditional trust email for OIDC, Google and GitLab IdP. [EOSC-KC-70](https://github.com/eosc-kc/keycloak/issues/70)
- SAML/ OIDC Identity Provider AutoUpdate. [EOSC-KC-119](https://github.com/eosc-kc/keycloak/issues/119)

### Fixed
- Imported RSA keys and Java keystore keys are always of type "sig". [KEYCLOAK-18933](https://issues.redhat.com/browse/KEYCLOAK-18933)
- Configure attribute name format in SAML UserAttribute mapper. [EOSC-KC-121](https://github.com/eosc-kc/keycloak/issues/121)
- Fix AttributeConsumingService element in SAML SP metadata. [EOSC-KC-122](https://github.com/eosc-kc/keycloak/issues/122)
