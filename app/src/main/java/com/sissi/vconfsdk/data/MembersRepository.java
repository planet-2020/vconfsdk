package com.sissi.vconfsdk.data;

import android.support.annotation.NonNull;

import java.util.Map;

import static androidx.core.util.Preconditions.checkNotNull;

public class MembersRepository implements IMembersDataSource {

    private static MembersRepository instance;

    private final IMembersDataSource membersRemoteDataSource;

    private final IMembersDataSource membersLocalDataSource;

    Map<String, Member> mCachedMembers;

    boolean mCacheIsDirty = false;


    private MembersRepository(@NonNull IMembersDataSource remoteDataSource,
                              @NonNull IMembersDataSource localDataSource) {
        membersRemoteDataSource = checkNotNull(remoteDataSource);
        membersLocalDataSource = checkNotNull(localDataSource);
    }

    public static MembersRepository getInstance(IMembersDataSource remoteDataSource,
                                                IMembersDataSource localDataSource){
        if (null == instance){
            instance = new MembersRepository(remoteDataSource, localDataSource);
        }

        return instance;
    }

    @Override
    public void getMembers() {

    }
}
