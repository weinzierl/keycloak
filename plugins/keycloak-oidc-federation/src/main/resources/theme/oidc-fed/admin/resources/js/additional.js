
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



keycloak_module.controller('RealmOidcFedSettingsCtrl', function($scope, $compile, realm, configuration, Configuration, Notifications) {
    
    $scope.realm = realm;
    $scope.realm.configuration = configuration;
    if ($scope.realm.configuration.authorityHints === undefined) {
    	$scope.realm.configuration.authorityHints = [];    
    }
    if ($scope.realm.configuration.trustAnchors === undefined) {
    	$scope.realm.configuration.trustAnchors = [];    
    }
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
    	Configuration.save({
            realm : realm.realm
        }, $scope.realm.configuration, function(data, headers) {
        	 $scope.changed = false;
        	 $scope.newAuthorityHint = "";
        	 $scope.newTrustAnchor= "";
        	 oldCopy = angular.copy($scope.realm.configuration);
             Notifications.success("Your changes have been saved.");
        });
    }
    
    $scope.reset = function() {
    	$scope.realm.configuration = angular.copy(oldCopy);
        $scope.changed = false;
    };
});



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


