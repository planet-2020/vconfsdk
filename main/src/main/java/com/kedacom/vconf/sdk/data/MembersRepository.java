package com.kedacom.vconf.sdk.data;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.base.IResponseListener;
import com.kedacom.vconf.sdk.base.CommonResultCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static androidx.core.util.Preconditions.checkNotNull;

public class MembersRepository implements IMembersDataSource {

    private static MembersRepository instance;

    private final IMembersDataSource membersRemoteDataSource;

    private final IMembersDataSource membersLocalDataSource;

    Map<String, Member> cachedMembers;

    boolean cacheIsDirty = false;


    private MembersRepository(@NonNull IMembersDataSource remoteDataSource,
                              @NonNull IMembersDataSource localDataSource) {
        membersRemoteDataSource = null;//checkNotNull(remoteDataSource);
        membersLocalDataSource = null;//checkNotNull(localDataSource);
    }

    public static MembersRepository getInstance(IMembersDataSource remoteDataSource,
                                                IMembersDataSource localDataSource){
        if (null == instance){
            instance = new MembersRepository(remoteDataSource, localDataSource);
        }

        return instance;
    }


    @Override
    public void getMembers(IResponseListener resultListener) {
//        checkNotNull(resultListener);

        if (cachedMembers != null && !cacheIsDirty) {
            resultListener.onResponse(CommonResultCode.SUCCESS, new ArrayList<>(cachedMembers.values()));
            return;
        }

        if (cacheIsDirty){

        }
    }

    private void getMembersFromRemoteDataSource(@NonNull final IResponseListener callback) {
        membersRemoteDataSource.getMembers(new IResponseListener(){
            @Override
            public void onResponse(int resultCode, Object response) {
                if (resultCode == CommonResultCode.SUCCESS){
                    refreshCache((List<Member>)response);
                }

                callback.onResponse(resultCode, response);
            }
        });
    }


    private void refreshCache(List<Member> members){
        if (cachedMembers == null) {
            cachedMembers = new LinkedHashMap<>();
        }
        cachedMembers.clear();
        for (Member member : members) {
            cachedMembers.put(member.e164, member);
        }
        cacheIsDirty = false;
    }
}
