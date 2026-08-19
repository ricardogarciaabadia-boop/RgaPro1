package com.rgapro.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "policies")
public class PolicyEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String policyNumber;
    public String company;
    public String holder;
    public String identityNumber;
    public String identityType;
    public String cif;
    public String phone;
    public String email;
    public String address;
    public String birthDate;
    public String nationality;
    public String expiry;
    public String rawJson;
}
