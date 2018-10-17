package com.sissi.vconfsdk.data;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface MembersDao {
    @Query("SELECT * FROM " + Member.TABLE_NAME)
    List<Member> getMembers();
}
