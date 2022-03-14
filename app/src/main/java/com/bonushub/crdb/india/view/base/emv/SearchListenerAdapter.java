package com.bonushub.crdb.india.view.base.emv;

import android.os.Bundle;
import android.os.RemoteException;

import com.usdk.apiservice.aidl.emv.SearchCardListener;

public class SearchListenerAdapter extends SearchCardListener.Stub {
    @Override
    public void onCardSwiped(Bundle bundle) throws RemoteException {

    }

    @Override
    public void onCardInsert() throws RemoteException {

    }

    @Override
    public void onCardPass(int i) throws RemoteException {

    }

    @Override
    public void onTimeout() throws RemoteException {

    }

    @Override
    public void onError(int i, String s) throws RemoteException {

    }
}
