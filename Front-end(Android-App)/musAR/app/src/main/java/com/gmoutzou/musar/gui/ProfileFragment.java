package com.gmoutzou.musar.gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.library.banner.BannerLayout;
import com.gmoutzou.musar.R;
import com.gmoutzou.musar.gui.adapter.BannerCallbackInterface;
import com.gmoutzou.musar.gui.adapter.LocalDataAdapter;
import com.special.ResideMenu.ResideMenu;

import prof.onto.ProfileVocabulary;

public class ProfileFragment extends Fragment implements ProfileVocabulary, BannerCallbackInterface {

    private View parentView;
    private ResideMenu resideMenu;
    private BannerLayout bannerVertical;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        parentView = inflater.inflate(R.layout.profile, container, false);
        setUpViews();
        return parentView;
    }

    private void setUpViews() {
        final MainActivity parentActivity = (MainActivity) getActivity();
        resideMenu = parentActivity.getResideMenu();
        bannerVertical =  parentView.findViewById(R.id.recycler_ver);
        bannerVertical.setAutoPlaying(false);
        LocalDataAdapter localDataAdapter = new LocalDataAdapter();
        localDataAdapter.registerCallbackInterface(this);
        bannerVertical.setAdapter(localDataAdapter);
    }

    @Override
    public void onBannerItemClick(String userChoice) {
        MainActivity parentActivity = (MainActivity) getActivity();
        if (!parentActivity.isProfileSet()) {
            parentActivity.makeNewOperation(CREATE_PROFILE, parentActivity.getPrimaryAccount(), userChoice);
        } else {
            parentActivity.makeNewOperation(UPDATE_PROFILE, parentActivity.getPrimaryAccount(), userChoice);
        }
    }
}
