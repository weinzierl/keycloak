package org.keycloak.protocol.oidc.federation.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Policy<T> {

    private List<T> subset_of;
    private List<T> one_of;
    private List<T> superset_of;
    private T add;
    private T value;
    @JsonProperty("default")
    private T defaultValue;
    private T essential;

    public static <T> PolicyBuilder<T> builder() {
        return new PolicyBuilder<T>();
    }
    
    protected Policy(){
        
    }

    protected Policy(PolicyBuilder<T> builder) {
        this.subset_of = builder.subset_of;
        this.one_of = builder.one_of;
        this.superset_of = builder.superset_of;
        this.add = builder.add;
        this.value = builder.value;
        this.defaultValue = builder.defaultValue;
        this.essential = builder.essential;
    }

    public List<T> getSubset_of() {
        return subset_of;
    }

    public void setSubset_of(List<T> subset_of) {
        this.subset_of = subset_of;
    }

    public List<T> getOne_of() {
        return one_of;
    }

    public void setOne_of(List<T> one_of) {
        this.one_of = one_of;
    }

    public List<T> getSuperset_of() {
        return superset_of;
    }

    public void setSuperset_of(List<T> superset_of) {
        this.superset_of = superset_of;
    }

    public T getAdd() {
        return add;
    }

    public void setAdd(T add) {
        this.add = add;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public T getEssential() {
        return essential;
    }

    public void setEssential(T essential) {
        this.essential = essential;
    }

    public static class PolicyBuilder<T> {
        private List<T> subset_of = new ArrayList<>();
        private List<T> one_of = new ArrayList<>();
        private List<T> superset_of = new ArrayList<>();
        private T add;
        private T value;
        private T defaultValue;
        private T essential;

        
        public PolicyBuilder<T> subsetOf(List<T> subsetOfList) {
            this.subset_of= subsetOfList;
            return this;
        }
        
        public PolicyBuilder<T> addSubsetOf(T subsetOf) {
            this.subset_of.add(subsetOf);
            return this;
        }

        public PolicyBuilder<T> addOneOf(T oneOf) {
            this.one_of.add(oneOf);
            return this;
        }

        public PolicyBuilder<T> addSupersetOf(T supersetOf) {
            this.superset_of.add(supersetOf);
            return this;
        }

        public PolicyBuilder<T> add(T add) {
            this.add = add;
            return this;
        }

        public PolicyBuilder<T> value(T value) {
            this.value = value;
            return this;
        }

        public PolicyBuilder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public PolicyBuilder<T> essential(T essential) {
            this.essential = essential;
            return this;
        }

        public Policy<T> build() {
            Policy<T> policy = new Policy<T>(this);
            return policy;

        }

    }

}
