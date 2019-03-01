package cm.aptoide.pt.home;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import cm.aptoide.pt.app.view.MoreBundleFragment;
import cm.aptoide.pt.navigator.FragmentNavigator;
import cm.aptoide.pt.store.view.StoreTabGridRecyclerFragment;

public class HomeContainerNavigator {

  private FragmentNavigator fragmentNavigator;

  public HomeContainerNavigator(FragmentNavigator fragmentNavigator) {
    this.fragmentNavigator = fragmentNavigator;
  }

  public void loadMainHomeContent() {
    fragmentNavigator.navigateToWithoutBackSave(new HomeFragment(), true);
  }

  public void loadGamesHomeContent() {
    Fragment fragment = new MoreBundleFragment();
    Bundle args = new Bundle();
    args.putString(StoreTabGridRecyclerFragment.BundleCons.TITLE, "Top Games");
    args.putString(StoreTabGridRecyclerFragment.BundleCons.ACTION,
        "https://ws75.aptoide.com/api/7/getStoreWidgets/store_id=15/context=home/widget=apps_list%3A0%262%3Adownloads7d");
    args.putBoolean(StoreTabGridRecyclerFragment.BundleCons.TOOLBAR, false);
    fragment.setArguments(args);
    fragmentNavigator.navigateToWithoutBackSave(fragment, true);
  }

  public void loadAppsHomeContent() {
    Fragment fragment = new MoreBundleFragment();
    Bundle args = new Bundle();
    args.putString(StoreTabGridRecyclerFragment.BundleCons.TITLE, "Applications");
    args.putString(StoreTabGridRecyclerFragment.BundleCons.ACTION,
        "https://ws75.aptoide.com/api/7/getStoreWidgets/store_id=15/context=home/widget=apps_list%3A15%267%3Atrending30d");
    args.putBoolean(StoreTabGridRecyclerFragment.BundleCons.TOOLBAR, false);
    fragment.setArguments(args);
    fragmentNavigator.navigateToWithoutBackSave(fragment, true);
  }
}
