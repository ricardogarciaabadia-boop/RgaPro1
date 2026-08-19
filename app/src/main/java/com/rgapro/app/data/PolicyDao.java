package com.rgapro.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PolicyDao {
    @Insert
    long insert(PolicyEntity policy);

    @Query("SELECT * FROM policies ORDER BY id DESC")
    List<PolicyEntity> getAll();

    @Query("SELECT * FROM policies WHERE policyNumber LIKE :query OR holder LIKE :query OR identityNumber LIKE :query ORDER BY id DESC")
    List<PolicyEntity> search(String query);
}
