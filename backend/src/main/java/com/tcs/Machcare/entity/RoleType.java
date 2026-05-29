package com.tcs.Machcare.entity;

public enum RoleType {
    Admin(1), 
    Maintenance_engineer(2), 
    Operator(3);

    private final int id;

    RoleType(int id) { this.id = id; }
    public int getId()  { return id; }
}