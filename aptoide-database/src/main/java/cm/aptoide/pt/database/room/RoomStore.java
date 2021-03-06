package cm.aptoide.pt.database.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "store") public class RoomStore {
  public static final String STORE_ID = "storeId";
  public static final String ICON_PATH = "iconPath";
  public static final String THEME = "theme";
  public static final String DOWNLOADS = "downloads";
  public static final String STORE_NAME = "storeName";
  public static final String USERNAME = "username";
  public static final String PASSWORD_SHA1 = "passwordSha1";

  @PrimaryKey @NonNull private long storeId;
  private String iconPath;
  private String theme;
  private long downloads;
  private String storeName;
  private String username;
  private String passwordSha1;

  public RoomStore() {
  }

  public RoomStore(long storeId, String iconPath, String theme, long downloads, String storeName,
      String username, String passwordSha1) {
    this.storeId = storeId;
    this.iconPath = iconPath;
    this.theme = theme;
    this.downloads = downloads;
    this.storeName = storeName;
    this.username = username;
    this.passwordSha1 = passwordSha1;
  }

  public long getStoreId() {
    return storeId;
  }

  public void setStoreId(long storeId) {
    this.storeId = storeId;
  }

  public String getIconPath() {
    return iconPath;
  }

  public void setIconPath(String iconPath) {
    this.iconPath = iconPath;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public long getDownloads() {
    return downloads;
  }

  public void setDownloads(long downloads) {
    this.downloads = downloads;
  }

  public String getStoreName() {
    return storeName;
  }

  public void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordSha1() {
    return passwordSha1;
  }

  public void setPasswordSha1(String passwordSha1) {
    this.passwordSha1 = passwordSha1;
  }
}
