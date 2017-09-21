package cm.aptoide.pt.spotandshare.socket.message.handlers.v1;

import cm.aptoide.pt.spotandshare.socket.entities.AndroidAppInfo;
import cm.aptoide.pt.spotandshare.socket.entities.FileInfo;
import cm.aptoide.pt.spotandshare.socket.entities.Host;
import cm.aptoide.pt.spotandshare.socket.file.ShareAppsFileClientSocket;
import cm.aptoide.pt.spotandshare.socket.file.ShareAppsFileServerSocket;
import cm.aptoide.pt.spotandshare.socket.interfaces.SocketBinder;
import cm.aptoide.pt.spotandshare.socket.interfaces.TransferLifecycle;
import cm.aptoide.pt.spotandshare.socket.interfaces.TransferLifecycleProvider;
import cm.aptoide.pt.spotandshare.socket.message.Message;
import cm.aptoide.pt.spotandshare.socket.message.MessageHandler;
import cm.aptoide.pt.spotandshare.socket.message.client.AptoideMessageClientSocket;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.Accepter;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.AndroidAppInfoAccepter;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.Sender;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.StorageCapacity;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.ExitMessage;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.HostLeftMessage;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.NewFriendMessage;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.ReceiveApk;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.RequestPermissionToSend;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.SendApk;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.ServerLeftMessage;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.WelcomeMessage;
import cm.aptoide.pt.spotandshare.socket.message.server.AptoideMessageServerSocket;
import java.io.File;

/**
 * Created by neuro on 02-02-2017.
 */

public class HandlersFactoryV1 {

  static class RequestPermissionToSendHandler extends MessageHandler<RequestPermissionToSend> {

    final AptoideMessageServerSocket messageServerSocket;

    public RequestPermissionToSendHandler(AptoideMessageServerSocket messageServerSocket) {
      super(RequestPermissionToSend.class);
      this.messageServerSocket = messageServerSocket;
    }

    @Override public void handleMessage(RequestPermissionToSend requestPermissionToSend,
        Sender<Message> messageSender) {
      messageServerSocket.requestPermissionToSendApk(requestPermissionToSend);
    }
  }

  static class SendApkHandler extends MessageHandler<SendApk> {

    private final TransferLifecycleProvider<AndroidAppInfo> transferLifecycleProvider;

    public SendApkHandler(TransferLifecycleProvider<AndroidAppInfo> transferLifecycleProvider) {
      super(SendApk.class);
      this.transferLifecycleProvider = transferLifecycleProvider;
    }

    @Override public void handleMessage(SendApk sendApkMessage, Sender<Message> messageSender) {
      ShareAppsFileServerSocket shareAppsFileServerSocket =
          new ShareAppsFileServerSocket(sendApkMessage.getServerPort(),
              sendApkMessage.getAndroidAppInfo());
      shareAppsFileServerSocket.setTransferLifecycle(sendApkMessage.getAndroidAppInfo(),
          transferLifecycleProvider.newTransferLifecycle());
      shareAppsFileServerSocket.startAsync();
      sendAck(messageSender);
      // TODO: 03-02-2017 neuro maybe a good ideia to stop the server somewhat :)
    }
  }

  static class ReceiveApkHandler extends MessageHandler<ReceiveApk> {

    private final String root;
    private final StorageCapacity storageCapacity;
    private final SocketBinder socketBinder;
    private final AndroidAppInfoAccepter androidAppInfoAccepter;

    public ReceiveApkHandler(String root, StorageCapacity storageCapacity,
        SocketBinder socketBinder,
        AndroidAppInfoAccepter androidAppInfoAccepter) {
      super(ReceiveApk.class);
      this.root = root;
      this.storageCapacity = storageCapacity;
      this.socketBinder = socketBinder;
      this.androidAppInfoAccepter = androidAppInfoAccepter;
    }

    String changeFilesRootDir(AndroidAppInfo androidAppInfo) {
      String packageName = androidAppInfo.getPackageName();
      String rootToFiles = root + File.separatorChar + packageName;

      for (FileInfo fileInfo : androidAppInfo.getFileInfos()) {
        fileInfo.setParentDirectory(rootToFiles);
      }

      return rootToFiles;
    }

    @Override public void handleMessage(ReceiveApk receiveApk, Sender<Message> messageSender) {
      AndroidAppInfo androidAppInfo = receiveApk.getAndroidAppInfo();

      androidAppInfoAccepter.call(new Accepter<AndroidAppInfo>() {
        @Override public AndroidAppInfo getMeta() {
          return androidAppInfo;
        }

        @Override public void accept(TransferLifecycle<AndroidAppInfo> fileClientLifecycle) {
          if (storageCapacity.hasCapacity(androidAppInfo.getFilesSize())) {
            Host receiveApkServerHost = receiveApk.getServerHost();

            String generatedRoot = ReceiveApkHandler.this.changeFilesRootDir(androidAppInfo);
            boolean mkdirs = new File(generatedRoot).mkdirs();

            ShareAppsFileClientSocket shareAppsFileClientSocket =
                new ShareAppsFileClientSocket(receiveApkServerHost.getIp(),
                    receiveApkServerHost.getPort(), androidAppInfo.getFileInfos());

            shareAppsFileClientSocket.setTransferLifecycle(androidAppInfo, fileClientLifecycle);
            shareAppsFileClientSocket.setSocketBinder(socketBinder);
            shareAppsFileClientSocket.startAsync();
          }
        }
      });

      sendAck(messageSender, false);
    }
  }

  static class ExitMessageHandler extends MessageHandler<ExitMessage> {

    final AptoideMessageServerSocket aptoideMessageServerSocket;

    public ExitMessageHandler(AptoideMessageServerSocket aptoideMessageServerSocket) {
      super(ExitMessage.class);
      this.aptoideMessageServerSocket = aptoideMessageServerSocket;
    }

    @Override public void handleMessage(ExitMessage message, Sender<Message> messageSender) {
      aptoideMessageServerSocket.removeHost(message.getLocalHost());
      sendAck(messageSender);
    }
  }

  static class HostLeftMessageHandler extends MessageHandler<HostLeftMessage> {

    final AptoideMessageClientSocket aptoideMessageClientSocket;

    public HostLeftMessageHandler(AptoideMessageClientSocket aptoideMessageClientSocket) {
      super(HostLeftMessage.class);
      this.aptoideMessageClientSocket = aptoideMessageClientSocket;
    }

    @Override public void handleMessage(HostLeftMessage message, Sender<Message> messageSender) {
      sendAck(messageSender);
      aptoideMessageClientSocket.onFriendLeft(message.getHostThatLeft());
    }
  }

  static class ServerLeftHandler extends MessageHandler<ServerLeftMessage> {

    private final AptoideMessageClientSocket aptoideMessageClientSocket;

    public ServerLeftHandler(AptoideMessageClientSocket aptoideMessageClientSocket) {
      super(ServerLeftMessage.class);
      this.aptoideMessageClientSocket = aptoideMessageClientSocket;
    }

    @Override public void handleMessage(ServerLeftMessage message, Sender<Message> messageSender) {
      aptoideMessageClientSocket.serverLeft();
      sendAck(messageSender);
      aptoideMessageClientSocket.shutdown();
    }
  }

  static class WelcomeMessageHandler extends MessageHandler<WelcomeMessage> {

    private final AptoideMessageServerSocket aptoideMessageServerSocket;

    public WelcomeMessageHandler(AptoideMessageServerSocket aptoideMessageServerSocket) {
      super(WelcomeMessage.class);
      this.aptoideMessageServerSocket = aptoideMessageServerSocket;
    }

    @Override public void handleMessage(WelcomeMessage message, Sender<Message> messageSender) {
      sendAck(messageSender);
      aptoideMessageServerSocket.onNewFriend(message.getFriend(), message.getLocalHost());
    }
  }

  static class NewFriendMessageHandler extends MessageHandler<NewFriendMessage> {

    private final AptoideMessageClientSocket aptoideMessageClientSocket;

    public NewFriendMessageHandler(AptoideMessageClientSocket aptoideMessageClientSocket) {
      super(NewFriendMessage.class);
      this.aptoideMessageClientSocket = aptoideMessageClientSocket;
    }

    @Override public void handleMessage(NewFriendMessage message, Sender<Message> messageSender) {
      sendAck(messageSender);
      aptoideMessageClientSocket.onNewFriend(message.getFriend(), message.getLocalHost());
    }
  }
}