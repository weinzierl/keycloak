
var keycloak_module = angular.module('keycloak');

keycloak_module.config(function($routeProvider) {

    $routeProvider
        .when('/realms/:realm/client-registration/oidc-federation-settings', {
            templateUrl : resourceUrl + '/partials/realm-oidc-federation-settings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                configuration : function(ConfigurationLoader) {
                    return ConfigurationLoader();
                }
            },
            controller : 'RealmOidcFedSettingsCtrl'
        })
        
} );



keycloak_module.controller('RealmOidcFedSettingsCtrl', function($scope, $route, Realm, realm, configuration, Configuration, Dialog, Notifications,TimeUnit2) {
    $scope.realm = realm;
    $scope.realm.configuration = configuration;
    $scope.saved=true;
    if ($scope.realm.configuration.authorityHints === undefined) {
    	$scope.realm.configuration.authorityHints = [];    
    	$scope.saved=false;
    }
    if ($scope.realm.configuration.trustAnchors === undefined) {
    	$scope.realm.configuration.trustAnchors = [];    
    }
    if ($scope.realm.configuration.expirationTime === undefined) {
    	$scope.realm.configuration.expirationTime = 3600;    
    }
    $scope.realm.configuration.expirationTime = TimeUnit2.asUnit(realm.configuration.expirationTime);
    if ($scope.realm.configuration.registrationType === undefined) {
    	$scope.realm.configuration.registrationType = "both";    
    }
    
    $scope.registrationTypeList = [
        "both",
        "automatic",
        "explicit"
    ];
    var oldCopy = angular.copy($scope.realm.configuration);
    $scope.newAuthorityHint = "";
    $scope.newTrustAnchor= "";
    $scope.changed=false;
    
    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm.configuration, oldCopy)) {
            $scope.changed = true;
        }
    }, true);
    
    $scope.addAuthorityHint = function() {
    	$scope.realm.configuration.authorityHints.push($scope.newAuthorityHint);
        $scope.newAuthorityHint = "";
    }
    
    $scope.deleteAuthorityHint = function(index) {
    	$scope.realm.configuration.authorityHints.splice(index, 1);
    }
    
    $scope.addTrustAnchor = function() {
    	$scope.realm.configuration.trustAnchors.push($scope.newTrustAnchor);
        $scope.newTrustAnchor = "";
    }
    
    $scope.deleteTrustAnchor = function(index) {
    	$scope.realm.configuration.trustAnchors.splice(index, 1);
    }    
    
    $scope.save = function() {
    	if ($scope.newAuthorityHint && $scope.newAuthorityHint.length > 0) {
             $scope.addAuthorityHint();
        }
    	if ($scope.newTrustAnchor && $scope.newTrustAnchor.length > 0) {
            $scope.addTrustAnchor();
        }

    	if (!$scope.realm.configuration.authorityHints || $scope.realm.configuration.authorityHints.length == 0 || !$scope.realm.configuration.trustAnchors || $scope.realm.configuration.trustAnchors.length == 0) {
             Notifications.error("You must specify at least one authority hint and one trust anchor");
        } else {
        	$scope.realm.configuration.expirationTime = $scope.realm.configuration.expirationTime.toSeconds();
    	    Configuration.save({
                realm : realm.realm
            }, $scope.realm.configuration, function(data, headers) {
                $route.reload();
                Notifications.success("Your changes have been saved.");
           });
        }
    };
    
    $scope.reset = function() {
    	$scope.realm.configuration = angular.copy(oldCopy);
        $scope.changed = false;
    };
    
    $scope.deleteConfiguration = function () {
        Dialog.confirmDelete($scope.realm.realm, 'OIDC federation configuration of', function () {
        	Configuration.remove({
                realm: realm.realm
            }, function () {
            	 $route.reload();
            	 Notifications.success("OIDC federation configuration for this realm has been deleted.");
            });
        });
    };
});

/*
keycloak_module.controller('ClientRegPoliciesCtrl', function($scope, $compile, realm) {

    $scope.realm = realm;
    var divElement = angular.element(document.querySelector(".nav.nav-tabs.nav-tabs-pf"));
    var appendHtml = $compile("<li><a href='#/realms/{{realm.realm}}/client-registration/oidc-federation-settings'>{{:: 'realm-tab-oidc-federation' | translate}}</a></li>")($scope);
    divElement.append(appendHtml);

});

keycloak_module.controller('ClientInitialAccessCtrl', function($scope, $compile, realm) {

	$scope.realm = realm;
    var divElement = angular.element(document.querySelector(".nav.nav-tabs.nav-tabs-pf"));
    var appendHtml = $compile("<li><a href='#/realms/{{realm.realm}}/client-registration/oidc-federation-settings'>{{:: 'realm-tab-oidc-federation' | translate}}</a></li>")($scope);
    divElement.append(appendHtml);
    
});

*/



keycloak_module.controller('ClientRegPoliciesCtrl', function($scope, realm, clientRegistrationPolicyProviders, policies, Dialog, Notifications, Components, $route, $location, $compile) {
    $scope.realm = realm;
    $scope.providers = clientRegistrationPolicyProviders;
    $scope.anonPolicies = [];
    $scope.authPolicies = [];
    for (var i=0 ; i<policies.length ; i++) {
        var policy = policies[i];
        if (policy.subType === 'anonymous') {
            $scope.anonPolicies.push(policy);
        } else if (policy.subType === 'authenticated') {
            $scope.authPolicies.push(policy);
        } else {
            throw 'subType is required for clientRegistration policy component!';
        }
    }

    $scope.addProvider = function(authType, provider) {
        console.log('Add provider: authType ' + authType + ', providerId: ' + provider.id);
        $location.url("/realms/" + realm.realm + "/client-registration/client-reg-policies/create/" + authType + '/' + provider.id);
    };

    $scope.getInstanceLink = function(instance) {
        return "/realms/" + realm.realm + "/client-registration/client-reg-policies/" + instance.providerId + "/" + instance.id;
    }

    $scope.removeInstance = function(instance) {
        Dialog.confirmDelete(instance.name, 'client registration policy', function() {
            Components.remove({
                realm : realm.realm,
                componentId : instance.id
            }, function() {
                $route.reload();
                Notifications.success("The policy has been deleted.");
            });
        });
    };
    
    $scope.realm = realm;
    var divElement = angular.element(document.querySelector(".nav.nav-tabs.nav-tabs-pf"));
    var appendHtml = $compile("<li><a href='#/realms/{{realm.realm}}/client-registration/oidc-federation-settings'>{{:: 'realm-tab-oidc-federation' | translate}}</a></li>")($scope);
    divElement.append(appendHtml);

});





keycloak_module.controller('ClientInitialAccessCtrl', function($scope, realm, clientInitialAccess, ClientInitialAccess, Dialog, Notifications, $route, $location, $compile) {
    $scope.realm = realm;
    $scope.clientInitialAccess = clientInitialAccess;

    $scope.remove = function(id) {
        Dialog.confirmDelete(id, 'initial access token', function() {
            ClientInitialAccess.remove({ realm: realm.realm, id: id }, function() {
                Notifications.success("The initial access token was deleted.");
                $route.reload();
            });
        });
    }
    
    $scope.realm = realm;
    var divElement = angular.element(document.querySelector(".nav.nav-tabs.nav-tabs-pf"));
    var appendHtml = $compile("<li><a href='#/realms/{{realm.realm}}/client-registration/oidc-federation-settings'>{{:: 'realm-tab-oidc-federation' | translate}}</a></li>")($scope);
    divElement.append(appendHtml);
    
    
});






var keycloak_loaders = angular.module('keycloak.loaders');

keycloak_loaders.factory('ConfigurationLoader', function(Loader, Configuration, $route) {
    return Loader.get(Configuration, function() {
        return {
            realm: $route.current.params.realm
        }
    });
});



var keycloak_services = angular.module('keycloak.services');

keycloak_services.factory('Configuration', function($resource) {
    return $resource(authUrl + '/realms/:realm/oidc-federation/configuration', {
        realm : '@realm',
    });
});


