package cm.aptoide.pt.v8engine.social.data;

import cm.aptoide.pt.dataprovider.model.v7.Comment;
import cm.aptoide.pt.dataprovider.model.v7.timeline.AppUpdateTimelineItem;
import cm.aptoide.pt.dataprovider.model.v7.timeline.Article;
import cm.aptoide.pt.dataprovider.model.v7.timeline.ArticleTimelineItem;
import cm.aptoide.pt.dataprovider.model.v7.timeline.GetUserTimeline;
import cm.aptoide.pt.dataprovider.model.v7.timeline.PopularAppTimelineItem;
import cm.aptoide.pt.dataprovider.model.v7.timeline.RecommendationTimelineItem;
import cm.aptoide.pt.dataprovider.model.v7.timeline.SocialInstall;
import cm.aptoide.pt.dataprovider.model.v7.timeline.SocialInstallTimelineItem;
import cm.aptoide.pt.dataprovider.model.v7.timeline.SocialRecommendation;
import cm.aptoide.pt.dataprovider.model.v7.timeline.SocialRecommendationTimelineItem;
import cm.aptoide.pt.dataprovider.model.v7.timeline.StoreLatestAppsTimelineItem;
import cm.aptoide.pt.dataprovider.model.v7.timeline.TimelineCard;
import cm.aptoide.pt.dataprovider.model.v7.timeline.TimelineItem;
import cm.aptoide.pt.dataprovider.model.v7.timeline.Video;
import cm.aptoide.pt.dataprovider.model.v7.timeline.VideoTimelineItem;
import cm.aptoide.pt.v8engine.Progress;
import cm.aptoide.pt.v8engine.link.LinksHandlerFactory;
import cm.aptoide.pt.v8engine.social.data.publisher.AptoidePublisher;
import cm.aptoide.pt.v8engine.social.data.publisher.MediaPublisher;
import cm.aptoide.pt.v8engine.social.data.publisher.Poster;
import cm.aptoide.pt.v8engine.social.data.publisher.Publisher;
import cm.aptoide.pt.v8engine.social.data.publisher.PublisherAvatar;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdandrade on 31/05/2017.
 */

public class TimelineResponseCardMapper {
  public List<Post> map(GetUserTimeline timelineResponse, LinksHandlerFactory linksFactory) {
    final List<Post> cards = new ArrayList();

    for (TimelineItem<TimelineCard> item : timelineResponse.getDatalist()
        .getList()) {
      String abUrl = null;
      if (item.getAb() != null
          && item.getAb()
          .getConversion() != null) {
        abUrl = item.getAb()
            .getConversion()
            .getUrl();
      }
      if (item instanceof ArticleTimelineItem) {
        final Article article = ((ArticleTimelineItem) item).getData();
        cards.add(new Media(article.getCardId(), article.getTitle(), article.getThumbnailUrl(),
            article.getDate(), article.getApps()
            .get(0), abUrl, new MediaPublisher(article.getPublisher()
            .getName(), new PublisherAvatar(article.getPublisher()
            .getLogoUrl())), linksFactory.get(LinksHandlerFactory.CUSTOM_TABS_LINK_TYPE,
            article.getPublisher()
                .getBaseUrl()),
            linksFactory.get(LinksHandlerFactory.CUSTOM_TABS_LINK_TYPE, article.getUrl()),
            CardType.ARTICLE));
      } else if (item instanceof VideoTimelineItem) {
        final Video video = ((VideoTimelineItem) item).getData();
        cards.add(
            new Media(video.getCardId(), video.getTitle(), video.getThumbnailUrl(), video.getDate(),
                video.getApps()
                    .get(0), abUrl, new MediaPublisher(video.getPublisher()
                .getName(), new PublisherAvatar(video.getPublisher()
                .getLogoUrl())), linksFactory.get(LinksHandlerFactory.CUSTOM_TABS_LINK_TYPE,
                video.getPublisher()
                    .getBaseUrl()),
                linksFactory.get(LinksHandlerFactory.CUSTOM_TABS_LINK_TYPE, video.getUrl()),
                CardType.VIDEO));
      } else if (item instanceof RecommendationTimelineItem) {
        final cm.aptoide.pt.dataprovider.model.v7.timeline.Recommendation recommendation =
            ((RecommendationTimelineItem) item).getData();
        cards.add(new Recommendation(recommendation.getCardId(), recommendation.getRecommendedApp()
            .getId(), recommendation.getRecommendedApp()
            .getPackageName(), recommendation.getRecommendedApp()
            .getName(), recommendation.getRecommendedApp()
            .getIcon(), recommendation.getSimilarApps()
            .get(0)
            .getName(), new AptoidePublisher(), recommendation.getTimestamp(), abUrl,
            CardType.RECOMMENDATION));
      } else if (item instanceof StoreLatestAppsTimelineItem) {
        final cm.aptoide.pt.dataprovider.model.v7.timeline.StoreLatestApps store =
            ((StoreLatestAppsTimelineItem) item).getData();
        cards.add(new StoreLatestApps(store.getCardId(), store.getStore()
            .getName(), store.getStore()
            .getAvatar(), store.getStore()
            .getStats()
            .getSubscribers(), store.getStore()
            .getStats()
            .getApps(), store.getLatestUpdate(), store.getApps(), abUrl, CardType.STORE));
      } else if (item instanceof AppUpdateTimelineItem) {
        final cm.aptoide.pt.dataprovider.model.v7.timeline.AppUpdate appUpdate =
            ((AppUpdateTimelineItem) item).getData();
        cards.add(new AppUpdate(appUpdate.getCardId(), appUpdate.getStore()
            .getName(), appUpdate.getStore()
            .getAvatar(), appUpdate.getIcon(), appUpdate.getName(), appUpdate.getPackageName(),
            appUpdate.getAdded(), abUrl, CardType.UPDATE, appUpdate.getFile(), appUpdate.getObb(),
            // TODO: 26/06/2017 probably should get progress state someway because the download might be happening already.
            Progress.INACTIVE));
      } else if (item instanceof PopularAppTimelineItem) {
        final cm.aptoide.pt.dataprovider.model.v7.timeline.PopularApp popularApp =
            ((PopularAppTimelineItem) item).getData();
        List<Publisher> publishers = new ArrayList<>();
        for (Comment.User user : popularApp.getUsers()) {
          publishers.add(new MediaPublisher(user.getName(), new PublisherAvatar(user.getAvatar())));
        }
        cards.add(new PopularApp(popularApp.getCardId(), popularApp.getPopularApplication()
            .getId(), popularApp.getPopularApplication()
            .getPackageName(), popularApp.getPopularApplication()
            .getName(), popularApp.getPopularApplication()
            .getIcon(), popularApp.getPopularApplication()
            .getStats()
            .getRating()
            .getAvg(), publishers, popularApp.getDate(), abUrl, CardType.POPULAR_APP));
      } else if (item instanceof SocialRecommendationTimelineItem) {
        final SocialRecommendation socialRecommendation =
            ((SocialRecommendationTimelineItem) item).getData();
        cards.add(new RatedRecommendation(socialRecommendation.getCardId(),
            new Poster(socialRecommendation.getUser(), socialRecommendation.getStore()), socialRecommendation.getApp()
            .getId(), socialRecommendation.getApp()
            .getPackageName(), socialRecommendation.getApp()
            .getName(), socialRecommendation.getApp()
            .getIcon(), socialRecommendation.getApp()
            .getStats()
            .getRating()
            .getAvg(), socialRecommendation.getDate(), abUrl, CardType.SOCIAL_RECOMMENDATION));
      } else if (item instanceof SocialInstallTimelineItem) {
        final SocialInstall socialInstall = ((SocialInstallTimelineItem) item).getData();
        cards.add(new RatedRecommendation(socialInstall.getCardId(),
            new Poster(socialInstall.getUser(), socialInstall.getStore()),
            socialInstall.getApp()
                .getId(), socialInstall.getApp()
            .getPackageName(), socialInstall.getApp()
            .getName(), socialInstall.getApp()
            .getIcon(), socialInstall.getApp()
            .getStats()
            .getRating()
            .getAvg(), socialInstall.getDate(), abUrl, CardType.SOCIAL_INSTALL));
      }
    }

    return cards;
  }
}
