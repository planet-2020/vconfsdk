package com.kedacom.vconf.sdk.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "members")
public final class Member {

    public static final String TABLE_NAME = "members";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public final String e164;

    public Member(String e164) {
        this.e164 = e164;
    }
}
