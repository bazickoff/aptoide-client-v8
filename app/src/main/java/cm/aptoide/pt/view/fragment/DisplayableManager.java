package cm.aptoide.pt.view.fragment;

import cm.aptoide.pt.view.recycler.displayable.Displayable;
import java.util.List;

public interface DisplayableManager {

  DisplayableManager addDisplayable(int position, Displayable displayable, boolean finishedLoading);

  DisplayableManager addDisplayable(Displayable displayable, boolean finishedLoading);

  DisplayableManager addDisplayables(List<? extends Displayable> displayables,
      boolean finishedLoading);

  @Deprecated DisplayableManager addDisplayables(int position,
      List<? extends Displayable> displayables, boolean finishedLoading);

  DisplayableManager clearDisplayables();

  boolean hasDisplayables();

  int getDisplayablesSize();

  void removeDisplayables(int fromIndex, int toIndex);
}
