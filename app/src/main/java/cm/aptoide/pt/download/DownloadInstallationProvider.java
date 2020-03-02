/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 25/07/2016.
 */

package cm.aptoide.pt.download;

import androidx.annotation.NonNull;
import cm.aptoide.pt.ads.MinimalAdMapper;
import cm.aptoide.pt.database.RoomStoredMinimalAdPersistence;
import cm.aptoide.pt.database.accessors.DownloadAccessor;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.database.room.RoomStoredMinimalAd;
import cm.aptoide.pt.dataprovider.ads.AdNetworkUtils;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.install.InstalledRepository;
import cm.aptoide.pt.install.exception.InstallationException;
import cm.aptoide.pt.install.installer.DownloadInstallationAdapter;
import cm.aptoide.pt.install.installer.Installation;
import cm.aptoide.pt.install.installer.InstallationProvider;
import cm.aptoide.pt.logger.Logger;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by marcelobenites on 7/25/16.
 */
public class DownloadInstallationProvider implements InstallationProvider {

  private static final String TAG = "DownloadInstallationPro";
  private final AptoideDownloadManager downloadManager;
  private final DownloadAccessor downloadAccessor;
  private final MinimalAdMapper adMapper;
  private final InstalledRepository installedRepository;
  private final RoomStoredMinimalAdPersistence roomStoredMinimalAdPersistence;

  public DownloadInstallationProvider(AptoideDownloadManager downloadManager,
      DownloadAccessor downloadAccessor, InstalledRepository installedRepository,
      MinimalAdMapper adMapper, RoomStoredMinimalAdPersistence roomStoredMinimalAdPersistence) {
    this.downloadManager = downloadManager;
    this.downloadAccessor = downloadAccessor;
    this.adMapper = adMapper;
    this.installedRepository = installedRepository;
    this.roomStoredMinimalAdPersistence = roomStoredMinimalAdPersistence;
  }

  @Override public Observable<Installation> getInstallation(String md5) {
    Logger.getInstance()
        .d(TAG, "Getting the installation " + md5);
    return downloadManager.getDownload(md5)
        .first()
        .flatMap(download -> {
          if (download.getOverallDownloadStatus() == Download.COMPLETED) {
            return installedRepository.get(download.getPackageName(), download.getVersionCode())
                .map(installed -> {
                  if (installed == null) {
                    installed = convertDownloadToInstalled(download);
                  }
                  return new DownloadInstallationAdapter(download, downloadAccessor,
                      installedRepository, installed);
                })
                .doOnNext(downloadInstallationAdapter -> {
                  roomStoredMinimalAdPersistence.get(downloadInstallationAdapter.getPackageName())
                      .doOnNext(handleCpd())
                      .subscribeOn(Schedulers.io())
                      .subscribe(storedMinimalAd -> {
                      }, Throwable::printStackTrace);
                });
          }
          return Observable.error(new InstallationException(
              "Installation file not available. download is "
                  + download.getMd5()
                  + " and the state is : "
                  + download.getOverallDownloadStatus()));
        });
  }

  @NonNull private Installed convertDownloadToInstalled(Download download) {
    Installed installed = new Installed();
    installed.setPackageAndVersionCode(download.getPackageName() + download.getVersionCode());
    installed.setVersionCode(download.getVersionCode());
    installed.setVersionName(download.getVersionName());
    installed.setStatus(Installed.STATUS_UNINSTALLED);
    installed.setType(Installed.TYPE_UNKNOWN);
    installed.setPackageName(download.getPackageName());
    return installed;
  }

  @NonNull private Action1<RoomStoredMinimalAd> handleCpd() {
    return storedMinimalAd -> {
      if (storedMinimalAd != null && storedMinimalAd.getCpdUrl() != null) {
        AdNetworkUtils.knockCpd(adMapper.map(storedMinimalAd));
        storedMinimalAd.setCpdUrl(null);
        roomStoredMinimalAdPersistence.insert(storedMinimalAd);
      }
    };
  }
}
