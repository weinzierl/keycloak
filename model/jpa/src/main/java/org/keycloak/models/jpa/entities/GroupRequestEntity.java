package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.Nationalized;

import javax.persistence.*;

@NamedQueries({})
@Entity
@Table(name="GROUP_REQUEST",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"GROUP_ID","USER_ID"})}
)
public class GroupRequestEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY)
    protected String id;

    @Column(name = "REASON")
    protected String reason;

    @Column(name = "GROUP_ID")
    protected String groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    protected UserEntity user;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof GroupRequestEntity)) return false;

        GroupRequestEntity that = (GroupRequestEntity) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
