/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.jpa;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.ClientInitialAccessEntity;
import org.keycloak.models.jpa.entities.ClientScopeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.IdentityProviderEntity;
import org.keycloak.models.jpa.entities.IdentityProviderMapperEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import com.google.common.collect.ImmutableSet;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProvider implements RealmProvider {
    protected static final Logger logger = Logger.getLogger(JpaRealmProvider.class);
    private final KeycloakSession session;
    protected EntityManager em;

    public JpaRealmProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public MigrationModel getMigrationModel() {
        return new MigrationModelAdapter(em);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmEntity realm = new RealmEntity();
        realm.setName(name);
        realm.setId(id);
        em.persist(realm);
        em.flush();
        final RealmModel adapter = new RealmAdapter(session, em, realm);
        session.getKeycloakSessionFactory().publish(new RealmModel.RealmCreationEvent() {
            @Override
            public RealmModel getCreatedRealm() {
                return adapter;
            }
            @Override
            public KeycloakSession getKeycloakSession() {
            	return session;
            }
        });
        return adapter;
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        RealmAdapter adapter = new RealmAdapter(session, em, realm);
        return adapter;
    }
    
    @Override
    public List<RealmModel> getRealmsWithProviderType(Class<?> providerType) {
        TypedQuery<String> query = em.createNamedQuery("getRealmIdsWithProviderType", String.class);
        query.setParameter("providerType", providerType.getName());
        return getRealms(query);
    }

    @Override
    public List<RealmModel> getRealms() {
        TypedQuery<String> query = em.createNamedQuery("getAllRealmIds", String.class);
        return getRealms(query);
    }

    private List<RealmModel> getRealms(TypedQuery<String> query) {
        List<String> entities = query.getResultList();
        List<RealmModel> realms = new ArrayList<RealmModel>();
        for (String id : entities) {
            RealmModel realm = session.realms().getRealm(id);
            if (realm != null) realms.add(realm);
            em.flush();
            em.clear();
        }
        return realms;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        TypedQuery<String> query = em.createNamedQuery("getRealmIdByName", String.class);
        query.setParameter("name", name);
        List<String> entities = query.getResultList();
        if (entities.isEmpty()) return null;
        if (entities.size() > 1) throw new IllegalStateException("Should not be more than one realm with same name");
        String id = query.getResultList().get(0);

        return session.realms().getRealm(id);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (realm == null) {
            return false;
        }
        em.refresh(realm);
        final RealmAdapter adapter = new RealmAdapter(session, em, realm);
        session.users().preRemove(adapter);

        realm.getDefaultGroups().clear();
        em.flush();

        int num = em.createNamedQuery("deleteGroupRoleMappingsByRealm")
                .setParameter("realm", realm).executeUpdate();

        TypedQuery<String> query = em.createNamedQuery("getClientIdsByRealm", String.class);
        query.setParameter("realm", realm.getId());
        List<String> clients = query.getResultList();
        for (String client : clients) {
            // No need to go through cache. Clients were already invalidated
            removeClient(client, adapter);
        }

        num = em.createNamedQuery("deleteDefaultClientScopeRealmMappingByRealm")
                .setParameter("realm", realm).executeUpdate();

        for (ClientScopeEntity a : new LinkedList<>(realm.getClientScopes())) {
            adapter.removeClientScope(a.getId());
        }

        for (RoleModel role : adapter.getRoles()) {
            // No need to go through cache. Roles were already invalidated
            removeRole(adapter, role);
        }

        for (GroupModel group : adapter.getGroups()) {
            session.realms().removeGroup(adapter, group);
        }
        
        for(IdentityProviderModel identityProviderModel : getIdentityProviders(adapter))
        	session.realms().removeIdentityProviderByAlias(adapter, identityProviderModel.getAlias());
        
        
        num = em.createNamedQuery("removeClientInitialAccessByRealm")
                .setParameter("realm", realm).executeUpdate();

        em.remove(realm);

        em.flush();
        em.clear();

        session.getKeycloakSessionFactory().publish(new RealmModel.RealmRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return adapter;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
       return addRealmRole(realm, KeycloakModelUtils.generateId(), name);

    }
    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        if (getRealmRole(realm, name) != null) {
            throw new ModelDuplicateException();
        }
        RoleEntity entity = new RoleEntity();
        entity.setId(id);
        entity.setName(name);
        RealmEntity ref = em.getReference(RealmEntity.class, realm.getId());
        entity.setRealm(ref);
        entity.setRealmId(realm.getId());
        em.persist(entity);
        em.flush();
        RoleAdapter adapter = new RoleAdapter(session, realm, em, entity);
        return adapter;

    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        TypedQuery<String> query = em.createNamedQuery("getRealmRoleIdByName", String.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm.getId());
        List<String> roles = query.getResultList();
        if (roles.isEmpty()) return null;
        return session.realms().getRoleById(roles.get(0), realm);
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String name) {
        return addClientRole(realm, client, KeycloakModelUtils.generateId(), name);
    }
    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name) {
        if (getClientRole(realm, client, name) != null) {
            throw new ModelDuplicateException();
        }
        ClientEntity clientEntity = em.getReference(ClientEntity.class, client.getId());
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setClient(clientEntity);
        roleEntity.setClientRole(true);
        roleEntity.setRealmId(realm.getId());
        em.persist(roleEntity);
        RoleAdapter adapter = new RoleAdapter(session, realm, em, roleEntity);
        return adapter;
    }

    @Override
    public Set<RoleModel> getRealmRoles(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getRealmRoleIds", String.class);
        query.setParameter("realm", realm.getId());
        List<String> roles = query.getResultList();

        if (roles.isEmpty()) return Collections.EMPTY_SET;
        Set<RoleModel> list = new HashSet<>();
        for (String id : roles) {
            list.add(session.realms().getRoleById(id, realm));
        }
        return Collections.unmodifiableSet(list);
    }

    @Override
    public RoleModel getClientRole(RealmModel realm, ClientModel client, String name) {
        TypedQuery<String> query = em.createNamedQuery("getClientRoleIdByName", String.class);
        query.setParameter("name", name);
        query.setParameter("client", client.getId());
        List<String> roles = query.getResultList();
        if (roles.isEmpty()) return null;
        return session.realms().getRoleById(roles.get(0), realm);
    }


    @Override
    public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client) {
        Set<RoleModel> list = new HashSet<>();
        TypedQuery<String> query = em.createNamedQuery("getClientRoleIds", String.class);
        query.setParameter("client", client.getId());
        List<String> roles = query.getResultList();
        for (String id : roles) {
            list.add(session.realms().getRoleById(id, realm));
        }
        return list;
    }
    
    @Override
    public Set<RoleModel> getRealmRoles(RealmModel realm, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getRealmRoles", RoleEntity.class);
        query.setParameter("realm", realm.getId());
        
        return getRoles(query, realm, first, max);
    }

    @Override
    public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getClientRoles", RoleEntity.class);
        query.setParameter("client", client.getId());
        
        return getRoles(query, realm, first, max);
    }
    
    protected Set<RoleModel> getRoles(TypedQuery<RoleEntity> query, RealmModel realm, Integer first, Integer max) {
        if(Objects.nonNull(first) && Objects.nonNull(max)
                && first >= 0 && max >= 0) {
            query= query.setFirstResult(first).setMaxResults(max);
        }

        List<RoleEntity> results = query.getResultList();
        
        return results.stream()
                .map(role -> new RoleAdapter(session, realm, em, role))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
    }
    
    @Override
    public Set<RoleModel> searchForClientRoles(RealmModel realm, ClientModel client, String search, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("searchForClientRoles", RoleEntity.class);
        query.setParameter("client", client.getId());
        return searchForRoles(query, realm, search, first, max);
    }
    
    @Override
    public Set<RoleModel> searchForRoles(RealmModel realm, String search, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("searchForRealmRoles", RoleEntity.class);
        query.setParameter("realm", realm.getId());
        
        return searchForRoles(query, realm, search, first, max);
    }
    
    protected Set<RoleModel> searchForRoles(TypedQuery<RoleEntity> query, RealmModel realm, String search, Integer first, Integer max) {

        query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        if(Objects.nonNull(first) && Objects.nonNull(max)
                && first >= 0 && max >= 0) {
            query= query.setFirstResult(first).setMaxResults(max);
        }
        
        List<RoleEntity> results = query.getResultList();
        
        return results.stream()
                .map(role -> new RoleAdapter(session, realm, em, role))
                .collect(Collectors.collectingAndThen(
                        Collectors.toSet(), Collections::unmodifiableSet));
    }

    @Override
    public boolean removeRole(RealmModel realm, RoleModel role) {
        session.users().preRemove(realm, role);
        RoleContainerModel container = role.getContainer();
        if (container.getDefaultRoles().contains(role.getName())) {
            container.removeDefaultRoles(role.getName());
        }
        RoleEntity roleEntity = em.getReference(RoleEntity.class, role.getId());
        String compositeRoleTable = JpaUtils.getTableNameForNativeQuery("COMPOSITE_ROLE", em);
        em.createNativeQuery("delete from " + compositeRoleTable + " where CHILD_ROLE = :role").setParameter("role", roleEntity).executeUpdate();
        realm.getClients().forEach(c -> c.deleteScopeMapping(role));
        em.createNamedQuery("deleteClientScopeRoleMappingByRole").setParameter("role", roleEntity).executeUpdate();
        int val = em.createNamedQuery("deleteGroupRoleMappingsByRole").setParameter("roleId", roleEntity.getId()).executeUpdate();

        em.flush();
        em.remove(roleEntity);

        session.getKeycloakSessionFactory().publish(new RoleContainerModel.RoleRemovedEvent() {
            @Override
            public RoleModel getRole() {
                return role;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        em.flush();
        return true;

    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (!realm.getId().equals(entity.getRealmId())) return null;
        RoleAdapter adapter = new RoleAdapter(session, realm, em, entity);
        return adapter;
    }

    @Override
    public GroupModel getGroupById(String id, RealmModel realm) {
        GroupEntity groupEntity = em.find(GroupEntity.class, id);
        if (groupEntity == null) return null;
        if (!groupEntity.getRealm().getId().equals(realm.getId())) return null;
        GroupAdapter adapter =  new GroupAdapter(realm, em, groupEntity);
        return adapter;
    }

    @Override
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        if (toParent != null && group.getId().equals(toParent.getId())) {
            return;
        }
        if (group.getParentId() != null) {
            group.getParent().removeChild(group);
        }
        group.setParent(toParent);
        if (toParent != null) toParent.addChild(group);
        else session.realms().addTopLevelGroup(realm, group);
    }

    @Override
    public List<GroupModel> getGroups(RealmModel realm) {
        RealmEntity ref = em.getReference(RealmEntity.class, realm.getId());

        return ref.getGroups().stream()
                .map(g -> session.realms().getGroupById(g.getId(), realm))
                .sorted(Comparator.comparing(GroupModel::getName))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        String query = "getGroupCount";
        if(Objects.equals(onlyTopGroups, Boolean.TRUE)) {
            query = "getTopLevelGroupCount";
        }
        Long count = em.createNamedQuery(query, Long.class)
                .setParameter("realm", realm.getId())
                .getSingleResult();

        return count;
    }

    @Override
    public Long getClientsCount(RealmModel realm) {
        return em.createNamedQuery("getRealmClientsCount", Long.class)
                .setParameter("realm", realm.getId())
                .getSingleResult();
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        return (long) searchForGroupByName(realm, search, null, null).size();
    }
    
    @Override
    public List<GroupModel> getGroupsByRole(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        TypedQuery<GroupEntity> query = em.createNamedQuery("groupsInRole", GroupEntity.class);
        query.setParameter("roleId", role.getId());
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<GroupEntity> results = query.getResultList();

        return results.stream()
        		.map(g -> new GroupAdapter(realm, em, g))
                .sorted(Comparator.comparing(GroupModel::getName))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<GroupModel> getTopLevelGroups(RealmModel realm) {
        RealmEntity ref = em.getReference(RealmEntity.class, realm.getId());

        return ref.getGroups().stream()
                .filter(g -> g.getParent() == null)
                .map(g -> session.realms().getGroupById(g.getId(), realm))
                .sorted(Comparator.comparing(GroupModel::getName))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<GroupModel> getTopLevelGroups(RealmModel realm, Integer first, Integer max) {
        List<String> groupIds =  em.createNamedQuery("getTopLevelGroupIds", String.class)
                .setParameter("realm", realm.getId())
                .setFirstResult(first)
                    .setMaxResults(max)
                    .getResultList();
        List<GroupModel> list = new ArrayList<>();
        if(Objects.nonNull(groupIds) && !groupIds.isEmpty()) {
            for (String id : groupIds) {
                GroupModel group = getGroupById(id, realm);
                list.add(group);
            }
        }

        list.sort(Comparator.comparing(GroupModel::getName));

        return Collections.unmodifiableList(list);
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        if (group == null) {
            return false;
        }

        GroupModel.GroupRemovedEvent event = new GroupModel.GroupRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public GroupModel getGroup() {
                return group;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
        session.getKeycloakSessionFactory().publish(event);

        session.users().preRemove(realm, group);

        realm.removeDefaultGroup(group);
        for (GroupModel subGroup : group.getSubGroups()) {
            session.realms().removeGroup(realm, subGroup);
        }
        GroupEntity groupEntity = em.find(GroupEntity.class, group.getId(), LockModeType.PESSIMISTIC_WRITE);
        if ((groupEntity == null) || (!groupEntity.getRealm().getId().equals(realm.getId()))) {
            return false;
        }
        em.createNamedQuery("deleteGroupRoleMappingsByGroup").setParameter("group", groupEntity).executeUpdate();

        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        realmEntity.getGroups().remove(groupEntity);

        em.remove(groupEntity);
        return true;


    }

    @Override
    public GroupModel createGroup(RealmModel realm, String name) {
        String id = KeycloakModelUtils.generateId();
        return createGroup(realm, id, name);
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name) {
        if (id == null) id = KeycloakModelUtils.generateId();
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setName(name);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        groupEntity.setRealm(realmEntity);
        em.persist(groupEntity);
        em.flush();
        realmEntity.getGroups().add(groupEntity);

        GroupAdapter adapter = new GroupAdapter(realm, em, groupEntity);
        return adapter;
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        subGroup.setParent(null);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String clientId) {
        return addClient(realm, KeycloakModelUtils.generateId(), clientId);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        if (clientId == null) {
            clientId = id;
        }
        ClientEntity entity = new ClientEntity();
        entity.setId(id);
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setStandardFlowEnabled(true);
        RealmEntity realmRef = em.getReference(RealmEntity.class, realm.getId());
        entity.setRealm(realmRef);
        em.persist(entity);
        em.flush();
        final ClientModel resource = new ClientAdapter(realm, em, session, entity);

        em.flush();
        session.getKeycloakSessionFactory().publish(new RealmModel.ClientCreationEvent() {
            @Override
            public ClientModel getCreatedClient() {
                return resource;
            }
        });
        return resource;
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm, Integer firstResult, Integer maxResults) {
        TypedQuery<String> query = em.createNamedQuery("getClientIdsByRealm", String.class);
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        query.setParameter("realm", realm.getId());
        List<String> clients = query.getResultList();
        if (clients.isEmpty()) return Collections.EMPTY_LIST;
        List<ClientModel> list = new LinkedList<>();
        for (String id : clients) {
            ClientModel client = session.realms().getClientById(id, realm);
            if (client != null) list.add(client);
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm) {
        return this.getClients(realm, null, null);
    }

    @Override
    public List<ClientModel> getAlwaysDisplayInConsoleClients(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getAlwaysDisplayInConsoleClients", String.class);
        query.setParameter("realm", realm.getId());
        List<String> clients = query.getResultList();
        if (clients.isEmpty()) return Collections.EMPTY_LIST;
        List<ClientModel> list = new LinkedList<>();
        for (String id : clients) {
            ClientModel client = session.realms().getClientById(id, realm);
            if (client != null) list.add(client);
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        ClientEntity app = em.find(ClientEntity.class, id);
        // Check if application belongs to this realm
        if (app == null || !realm.getId().equals(app.getRealm().getId())) return null;
        ClientAdapter client = new ClientAdapter(realm, em, session, app);
        return client;

    }

    @Override
    public ClientModel getClientByClientId(String clientId, RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("findClientIdByClientId", String.class);
        query.setParameter("clientId", clientId);
        query.setParameter("realm", realm.getId());
        List<String> results = query.getResultList();
        if (results.isEmpty()) return null;
        String id = results.get(0);
        return session.realms().getClientById(id, realm);
    }

    @Override
    public List<ClientModel> searchClientsByClientId(String clientId, Integer firstResult, Integer maxResults, RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("searchClientsByClientId", String.class);
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        query.setParameter("clientId", clientId);
        query.setParameter("realm", realm.getId());
        List<String> results = query.getResultList();
        if (results.isEmpty()) return Collections.EMPTY_LIST;
        return results.stream().map(id -> session.realms().getClientById(id, realm)).collect(Collectors.toList());
    }

    @Override
    public boolean removeClient(String id, RealmModel realm) {
        final ClientModel client = getClientById(id, realm);
        if (client == null) return false;

        session.users().preRemove(realm, client);

        for (RoleModel role : client.getRoles()) {
            // No need to go through cache. Roles were already invalidated
            removeRole(realm, role);
        }

        ClientEntity clientEntity = em.find(ClientEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        session.getKeycloakSessionFactory().publish(new RealmModel.ClientRemovedEvent() {
            @Override
            public ClientModel getClient() {
                return client;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        int countRemoved = em.createNamedQuery("deleteClientScopeClientMappingByClient")
                .setParameter("client", clientEntity)
                .executeUpdate();
        em.remove(clientEntity);  // i have no idea why, but this needs to come before deleteScopeMapping

        try {
            em.flush();
        } catch (RuntimeException e) {
            logger.errorv("Unable to delete client entity: {0} from realm {1}", client.getClientId(), realm.getName());
            throw e;
        }

        return true;
    }

    @Override
    public ClientScopeModel getClientScopeById(String id, RealmModel realm) {
        ClientScopeEntity app = em.find(ClientScopeEntity.class, id);

        // Check if application belongs to this realm
        if (app == null || !realm.getId().equals(app.getRealm().getId())) return null;
        ClientScopeAdapter adapter = new ClientScopeAdapter(realm, em, session, app);
        return adapter;
    }

    @Override
    public List<GroupModel> searchForGroupByName(RealmModel realm, String search, Integer first, Integer max) {
        TypedQuery<String> query = em.createNamedQuery("getGroupIdsByNameContaining", String.class)
                .setParameter("realm", realm.getId())
                .setParameter("search", search);
        if(Objects.nonNull(first) && Objects.nonNull(max)) {
            query= query.setFirstResult(first).setMaxResults(max);
        }
        List<String> groups =  query.getResultList();
        if (Objects.isNull(groups)) return Collections.EMPTY_LIST;
        List<GroupModel> list = new ArrayList<>();
        for (String id : groups) {
            GroupModel groupById = session.realms().getGroupById(id, realm);
            while(Objects.nonNull(groupById.getParentId())) {
                groupById = session.realms().getGroupById(groupById.getParentId(), realm);
            }
            if(!list.contains(groupById)) {
                list.add(groupById);
            }
        }
        list.sort(Comparator.comparing(GroupModel::getName));

        return Collections.unmodifiableList(list);
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(RealmModel realm, int expiration, int count) {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId());

        ClientInitialAccessEntity entity = new ClientInitialAccessEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealm(realmEntity);

        entity.setCount(count);
        entity.setRemainingCount(count);

        int currentTime = Time.currentTime();
        entity.setTimestamp(currentTime);
        entity.setExpiration(expiration);

        em.persist(entity);

        return entityToModel(entity);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(RealmModel realm, String id) {
        ClientInitialAccessEntity entity = em.find(ClientInitialAccessEntity.class, id);
        if (entity == null) {
            return null;
        } else {
            return entityToModel(entity);
        }
    }

    @Override
    public void removeClientInitialAccessModel(RealmModel realm, String id) {
        ClientInitialAccessEntity entity = em.find(ClientInitialAccessEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity != null) {
            em.remove(entity);
            em.flush();
        }
    }

    @Override
    public List<ClientInitialAccessModel> listClientInitialAccess(RealmModel realm) {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId());

        TypedQuery<ClientInitialAccessEntity> query = em.createNamedQuery("findClientInitialAccessByRealm", ClientInitialAccessEntity.class);
        query.setParameter("realm", realmEntity);
        List<ClientInitialAccessEntity> entities = query.getResultList();

        return entities.stream()
                .map(this::entityToModel)
                .collect(Collectors.toList());
    }

    @Override
    public void removeExpiredClientInitialAccess() {
        int currentTime = Time.currentTime();

        em.createNamedQuery("removeExpiredClientInitialAccess")
                .setParameter("currentTime", currentTime)
                .executeUpdate();
    }

    @Override
    public void decreaseRemainingCount(RealmModel realm, ClientInitialAccessModel clientInitialAccess) {
        em.createNamedQuery("decreaseClientInitialAccessRemainingCount")
                .setParameter("id", clientInitialAccess.getId())
                .executeUpdate();
    }

    private ClientInitialAccessModel entityToModel(ClientInitialAccessEntity entity) {
        ClientInitialAccessModel model = new ClientInitialAccessModel();
        model.setId(entity.getId());
        model.setCount(entity.getCount());
        model.setRemainingCount(entity.getRemainingCount());
        model.setExpiration(entity.getExpiration());
        model.setTimestamp(entity.getTimestamp());
        return model;
    }
    
    
    
    @Override
	public List<String> getUsedIdentityProviderIdTypes(RealmModel realm){
    	TypedQuery<String> query = em.createNamedQuery("findUtilizedIdentityProviderTypesOfRealm", String.class);
    	query.setParameter("realmId", realm.getId());
    	return query.getResultList();
	}
    
    @Override
	public Long countIdentityProviders(RealmModel realm) {
    	TypedQuery<Long> query = em.createNamedQuery("countIdentityProvidersOfRealm", Long.class);
    	query.setParameter("realmId", realm.getId());
    	return query.getSingleResult();
    }
    
    
	@Override
	public List<IdentityProviderModel> getIdentityProviders(RealmModel realm) {
		TypedQuery<IdentityProviderEntity> query = em.createNamedQuery("findIdentityProviderByRealm", IdentityProviderEntity.class);
		query.setParameter("realmId", realm.getId());
		return query.getResultList().stream().map(entity -> entityToModel(entity)).collect(Collectors.toList());
	}
  
  
	@Override
	public List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults) {
		TypedQuery<IdentityProviderEntity> query = (keyword!=null && !keyword.isEmpty()) ? 
				em.createNamedQuery("findIdentityProviderByRealmAndKeyword", IdentityProviderEntity.class) :
				em.createNamedQuery("findIdentityProviderByRealm", IdentityProviderEntity.class);
				
		query.setParameter("realmId", realm.getId());
		if (firstResult != null && firstResult >= 0)
			query.setFirstResult(firstResult);
		if (maxResults != null && maxResults > 0)
			query.setMaxResults(maxResults);
		
		if(keyword!=null && !keyword.isEmpty())
			query.setParameter("keyword", "%"+keyword.toLowerCase()+"%");
		
		return query.getResultList().stream().map(entity -> entityToModel(entity)).collect(Collectors.toList());
	}
  
  
  
	private IdentityProviderModel entityToModel(IdentityProviderEntity entity) {
		IdentityProviderModel identityProviderModel = new IdentityProviderModel();
		identityProviderModel.setProviderId(entity.getProviderId());
		identityProviderModel.setAlias(entity.getAlias());
		identityProviderModel.setDisplayName(entity.getDisplayName());

		identityProviderModel.setInternalId(entity.getInternalId());
		Map<String, String> config = entity.getConfig();
		Map<String, String> copy = new HashMap<>();
		copy.putAll(config);
		identityProviderModel.setConfig(copy);
		identityProviderModel.setEnabled(entity.isEnabled());
		identityProviderModel.setLinkOnly(entity.isLinkOnly());
		identityProviderModel.setTrustEmail(entity.isTrustEmail());
		identityProviderModel.setAuthenticateByDefault(entity.isAuthenticateByDefault());
		identityProviderModel.setFirstBrokerLoginFlowId(entity.getFirstBrokerLoginFlowId());
		identityProviderModel.setPostBrokerLoginFlowId(entity.getPostBrokerLoginFlowId());
		identityProviderModel.setStoreToken(entity.isStoreToken());
		identityProviderModel.setAddReadTokenRoleOnCreate(entity.isAddReadTokenRoleOnCreate());
		return identityProviderModel;
	}
  
	private IdentityProviderEntity modelToEntity(IdentityProviderModel identityProvider) {
		IdentityProviderEntity entity = new IdentityProviderEntity();

		if (identityProvider.getInternalId() == null) {
			entity.setInternalId(KeycloakModelUtils.generateId());
		} else {
			entity.setInternalId(identityProvider.getInternalId());
		}
		entity.setAlias(identityProvider.getAlias());
		entity.setDisplayName(identityProvider.getDisplayName());
		entity.setProviderId(identityProvider.getProviderId());
		entity.setEnabled(identityProvider.isEnabled());
		entity.setStoreToken(identityProvider.isStoreToken());
		entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
		entity.setTrustEmail(identityProvider.isTrustEmail());
		entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
		entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
		entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
		entity.setConfig(identityProvider.getConfig());
		entity.setLinkOnly(identityProvider.isLinkOnly());

		return entity;
	}
  
	@Override
	public IdentityProviderModel getIdentityProviderById(String internalId) {
		IdentityProviderEntity identityProvider = em.find(IdentityProviderEntity.class, internalId);
		return entityToModel(identityProvider);
	}
  
	private IdentityProviderEntity getIdentityProviderEntityByAlias(RealmModel realmModel, String alias) {
		TypedQuery<IdentityProviderEntity> query = em.createNamedQuery("findIdentityProviderByRealmAndAlias", IdentityProviderEntity.class);
      query.setParameter("alias", alias);
      query.setParameter("realmId", realmModel.getId());
      try {
      	return query.getSingleResult();
      }
      catch(NoResultException | NonUniqueResultException ex) {
      	return null;
      }
	}
  
	@Override
	public IdentityProviderModel getIdentityProviderByAlias(RealmModel realmModel, String alias) {
		IdentityProviderEntity identityProvider = getIdentityProviderEntityByAlias(realmModel, alias);
		return entityToModel(identityProvider);
	}

	@Override
	public void addIdentityProvider(RealmModel realmModel, IdentityProviderModel identityProvider) {
		IdentityProviderEntity entity = modelToEntity(identityProvider);
		
		RealmEntity realm = new RealmEntity();
		realm.setId(realmModel.getId());
		
		entity.setRealm(realm);

		em.persist(entity);
		em.flush();

		identityProvider.setInternalId(entity.getInternalId());

		session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderCreationEvent() {
			@Override
			public IdentityProviderModel getCreatedIdentityProvider() {
				return identityProvider;
			}
		});

	}

	@Override
	public void removeIdentityProviderByAlias(RealmModel realmModel, String alias) {
		TypedQuery<IdentityProviderEntity> query = em.createNamedQuery("findIdentityProviderByRealmAndAlias", IdentityProviderEntity.class);
		query.setParameter("alias", alias);
		query.setParameter("realmId", realmModel.getId());
		IdentityProviderEntity identityProvider = query.getSingleResult();
		
		getIdentityProviderMappersByAlias(realmModel, identityProvider.getAlias()).stream()
			.forEach(identityProviderMapper -> removeIdentityProviderMapper(realmModel, identityProviderMapper));
		
		
		em.remove(identityProvider);
		em.flush();

		IdentityProviderModel model = entityToModel(identityProvider);
		
		session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderRemovedEvent() {

			@Override
			public RealmModel getRealm() {
				return realmModel;
			}

			@Override
			public IdentityProviderModel getRemovedIdentityProvider() {
				return model;
			}

			@Override
			public KeycloakSession getKeycloakSession() {
				return session;
			}
		});

	}

	@Override
	public void updateIdentityProvider(RealmModel realmModel, IdentityProviderModel identityProvider) {

		IdentityProviderEntity entity = em.find(IdentityProviderEntity.class, identityProvider.getInternalId());
		if (entity != null) {
			entity.setAlias(identityProvider.getAlias());
			entity.setDisplayName(identityProvider.getDisplayName());
			entity.setEnabled(identityProvider.isEnabled());
			entity.setTrustEmail(identityProvider.isTrustEmail());
			entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
			entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
			entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
			entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
			entity.setStoreToken(identityProvider.isStoreToken());
			entity.setConfig(identityProvider.getConfig());
			entity.setLinkOnly(identityProvider.isLinkOnly());
		}

		em.flush();

		session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderUpdatedEvent() {

			@Override
			public RealmModel getRealm() {
				return realmModel;
			}

			@Override
			public IdentityProviderModel getUpdatedIdentityProvider() {
				return identityProvider;
			}

			@Override
			public KeycloakSession getKeycloakSession() {
				return session;
			}
		});
		
	}



	@Override
	public boolean isIdentityFederationEnabled(RealmModel realmModel) {
		return countIdentityProviders(realmModel) > 0;
	}
	
	
	@Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel) {
		TypedQuery<IdentityProviderMapperEntity> query = em.createNamedQuery("findIdentityProviderMappersByRealm", IdentityProviderMapperEntity.class);
		query.setParameter("realmId", realmModel.getId());
        return query.getResultList().stream().map(entity -> entityToModel(entity)).collect(Collectors.collectingAndThen(Collectors.toSet(), ImmutableSet::copyOf));
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias) {
    	TypedQuery<IdentityProviderMapperEntity> query = em.createNamedQuery("findIdentityProviderMappersByRealmAndAlias", IdentityProviderMapperEntity.class);
    	query.setParameter("alias", brokerAlias);
    	query.setParameter("realmId", realmModel.getId());
    	return query.getResultList().stream().map(entity -> entityToModel(entity)).collect(Collectors.collectingAndThen(Collectors.toSet(), ImmutableSet::copyOf));
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model) {
    	RealmEntity realm = em.find(RealmEntity.class, realmModel.getId());
        if (getIdentityProviderMapperByName(realmModel, model.getIdentityProviderAlias(), model.getName()) != null) {
            throw new RuntimeException("identity provider mapper name must be unique per identity provider");
        }
        String id = KeycloakModelUtils.generateId();
        IdentityProviderMapperEntity entity = new IdentityProviderMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setRealm(realm);
        entity.setConfig(model.getConfig());
        
        em.persist(entity);
        
        return entityToModel(entity);
    }


    protected IdentityProviderMapperEntity getIdentityProviderMapperEntityByName(RealmModel realmModel, String alias, String name) {
    	TypedQuery<IdentityProviderMapperEntity> query = em.createNamedQuery("findIdentityProviderMappersByRealmAndAliasAndName", IdentityProviderMapperEntity.class);
    	query.setParameter("realmId", realmModel.getId());
    	query.setParameter("alias", alias);
    	query.setParameter("name", name);
    	try {
    		return query.getSingleResult();
    	}
    	catch(NoResultException | NonUniqueResultException e) {
    		return null;
    	}
    }

    @Override
    public void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
        IdentityProviderMapperEntity toDelete = em.find(IdentityProviderMapperEntity.class, mapping.getId());
        if (toDelete != null)
        	em.remove(toDelete);
    }

    @Override
    public void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
    	IdentityProviderMapperEntity entity = em.find(IdentityProviderMapperEntity.class, mapping.getId());
        entity.setIdentityProviderAlias(mapping.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(mapping.getIdentityProviderMapper());
        if (entity.getConfig() == null) {
            entity.setConfig(mapping.getConfig());
        } else {
            entity.getConfig().clear();
            if (mapping.getConfig() != null) {
                entity.getConfig().putAll(mapping.getConfig());
            }
        }
        em.flush();

    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(RealmModel realmModel, String id) {
    	IdentityProviderMapperEntity entity = em.find(IdentityProviderMapperEntity.class, id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntityByName(realmModel, alias, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    
    protected IdentityProviderMapperModel entityToModel(IdentityProviderMapperEntity entity) {
        IdentityProviderMapperModel mapping = new IdentityProviderMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setIdentityProviderAlias(entity.getIdentityProviderAlias());
        mapping.setIdentityProviderMapper(entity.getIdentityProviderMapper());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }
	
	
    
}
