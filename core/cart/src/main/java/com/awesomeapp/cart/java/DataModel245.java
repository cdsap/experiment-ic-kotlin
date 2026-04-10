package com.awesomeapp.cart.java;

/**
 * Simple data model class 245.
 */
public class DataModel245 {
    private String id;
    private String name;
    private int value;
    private boolean active;

    public DataModel245() {}

    public DataModel245(String id, String name, int value, boolean active) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "DataModel245{id='" + id + "', name='" + name + "', value=" + value + ", active=" + active + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataModel245 that = (DataModel245) o;
        return value == that.value && active == that.active &&
            java.util.Objects.equals(id, that.id) && java.util.Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, value, active);
    }
}
