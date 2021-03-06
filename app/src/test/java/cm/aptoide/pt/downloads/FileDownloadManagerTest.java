package cm.aptoide.pt.downloads;

import cm.aptoide.pt.download.FileDownloadManager;
import cm.aptoide.pt.download.FileDownloadTask;
import com.liulishuo.filedownloader.BaseDownloadTask;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by filipegoncalves on 8/1/18.
 */
public class FileDownloadManagerTest {

  @Mock private com.liulishuo.filedownloader.FileDownloader fileDownloader;
  @Mock private FileDownloadTask fileDownloadTask;
  @Mock private BaseDownloadTask mockBaseDownloadTask;
  private FileDownloadManager fileDownloaderManager;
  private FileDownloadManager fileDownloaderManagerEmptyDownloadLink;
  private TestSubscriber<Object> testSubscriber;

  @Before public void setupAppDownloaderTest() {
    MockitoAnnotations.initMocks(this);
    fileDownloaderManager =
        new FileDownloadManager(fileDownloader, fileDownloadTask, "randomDownloadsPath",
            "http://apkdownload.com/file/mainObb.apk", 0, "cm.aptoide.pt", 0, "filename");

    fileDownloaderManagerEmptyDownloadLink =
        new FileDownloadManager(fileDownloader, fileDownloadTask, "randomDownloadsPath", "", 0, "",
            0, "");

    testSubscriber = TestSubscriber.create();
  }

  @Test public void startFileDownload() throws Exception {
    when(fileDownloader.create(any())).thenReturn(mockBaseDownloadTask);
    when(mockBaseDownloadTask.asInQueueTask()).thenReturn(new MockInqueueTask());

    fileDownloaderManager.startFileDownload()
        .subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertCompleted();
    verify(fileDownloader).start(fileDownloadTask, true);
  }

  @Test public void startFileDownloadEmptyLink() throws Exception {

    fileDownloaderManagerEmptyDownloadLink.startFileDownload()
        .subscribe(testSubscriber);
    testSubscriber.assertError(IllegalArgumentException.class);
    verifyZeroInteractions(fileDownloader);
  }

  @Test public void pauseDownload() throws Exception {
    fileDownloaderManager.pauseDownload()
        .subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertCompleted();
    verify(fileDownloader).pause(fileDownloadTask);
  }

  @Test public void removeDownloadFile() throws Exception {
    fileDownloaderManager.removeDownloadFile()
        .subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertCompleted();
    verify(fileDownloader).clear(0, "http://apkdownload.com/file/mainObb.apk");
  }
}