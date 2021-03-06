package cm.aptoide.pt.account.view;

import android.os.Bundle;
import androidx.annotation.Nullable;
import cm.aptoide.pt.view.BackButtonActivity;
import com.jakewharton.rxrelay.BehaviorRelay;
import rx.Observable;

public abstract class LoginBottomSheetActivity extends BackButtonActivity
    implements LoginBottomSheet {

  private BehaviorRelay<State> stateSubject;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    stateSubject = BehaviorRelay.create();
  }

  @Override public void expand() {
    stateSubject.call(State.EXPANDED);
  }

  @Override public void collapse() {
    stateSubject.call(State.COLLAPSED);
  }

  @Override public Observable<State> state() {
    return stateSubject;
  }
}
