package com.easycache.core;

public class Foo implements Entity {

    private final long id;
    private String description;

    public Foo(long id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Foo [id=" + this.id + ", description=" + this.description + "]";
    }
}
