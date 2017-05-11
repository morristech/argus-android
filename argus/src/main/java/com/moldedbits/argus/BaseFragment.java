package com.moldedbits.argus;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moldedbits.argus.listener.ResultListener;
import com.moldedbits.argus.model.ArgusUser;
import com.moldedbits.argus.provider.BaseProvider;

import java.util.List;


public abstract class BaseFragment extends Fragment implements ResultListener {

    private ResultListener listener;

    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(getLayoutId(), container, false);
        setView(rootView, getProviders());
        return rootView;
    }

    protected void setView(View view, List<BaseProvider> providerList) {
        // First set result listener for all providers
        for (BaseProvider provider : providerList) {
            provider.setResultListener(this);
        }

        // If state is IN_PROGRESS, find the provider in progress and populate.
        if (Argus.getInstance().getState() == ArgusState.IN_PROGRESS) {
            BaseProvider provider = getProviderInProgress(providerList);
            // Exactly one provider should be in progress
            if (provider == null) {
                throw new RuntimeException("At least one provider should be in progress");
            }

            // Layout needs to define container id
            if (rootView.findViewById(getContainerId()) == null) {
                throw new RuntimeException("Did you forget to define container in your layout?");
            }

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(getContainerId(), provider.getProgressView())
                    .commit();
        } else { // Else populate all providers
            for (BaseProvider provider : providerList) {

                View containerView = view.findViewById(provider.getContainerId());
                if (containerView == null || !(containerView instanceof ViewGroup)) {
                    throw new RuntimeException("Did you forget to define container in your layout");
                }

                ((ViewGroup) containerView)
                        .addView(provider.loginView(this, (ViewGroup) containerView));
            }
        }
    }

    @Override
    public void onSuccess(ArgusUser user, ArgusState state) {
        listener.onSuccess(user, state);

        // If state was changed to IN_PROGRESS, then update the UI to show the progress view of the
        // in progress provider. If state was changed to SIGNED_OUT, then update the UI to show all
        // the providers
        setView(rootView, getProviders());
    }

    @Override
    public void onFailure(String message, ArgusState state) {
        listener.onFailure(message, state);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ResultListener) {
            listener = (ResultListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ResultListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        onProviderActivityResult(requestCode, resultCode, data, getProviders());
    }

    protected void onProviderActivityResult(int requestCode, int resultCode, Intent data,
                                            List<BaseProvider>
                                                    providerList) {
        for (BaseProvider provider : providerList) {
            provider.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected abstract int getLayoutId();

    protected abstract int getContainerId();

    protected abstract List<BaseProvider> getProviders();

    private static BaseProvider getProviderInProgress(List<BaseProvider> providers) {
        for (BaseProvider provider : providers) {
            if (provider.isInProgress()) {
                return provider;
            }
        }

        return null;
    }
}
