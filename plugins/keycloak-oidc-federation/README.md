This is a keycloak plugin to add support for OIDC federations. 

It implements the [OpenID Connect Federation 1.0.Specification (draft 13)](https://openid.net/specs/openid-connect-federation-1_0.html).

This plugin offers the OpenID Connect Relying Party implementation.

## Building

This component should be not built directly i.e. `mvn clean install` , because it has many dependencies to the keycloak core modules. Instead, issue a full server build i.e. with 

`mvn -Pdistribution -pl distribution/server-dist -am -Dmaven.test.skip clean install`

from the keycloak's base folder, as described [here](https://github.com/keycloak/keycloak/blob/master/docs/building.md) 

It should be installed as a keycloak extension, meaning that it should be deployed as a wildfly module, and also added as a dependency in `keycloak-services` module xml, by adding the entry 
`<module name="org.keycloak.keycloak-oidc-federation" services="import"/>` 
