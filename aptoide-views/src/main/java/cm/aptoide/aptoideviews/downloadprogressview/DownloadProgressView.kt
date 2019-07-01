package cm.aptoide.aptoideviews.downloadprogressview

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.CheckResult
import android.support.annotation.VisibleForTesting
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import cm.aptoide.aptoideviews.R
import cm.aptoide.aptoideviews.common.Debouncer
import com.tinder.StateMachine
import kotlinx.android.synthetic.main.download_progress_view.view.*
import rx.Observable


/**
 * This view is responsible for handling the display of download progress
 */
class DownloadProgressView : FrameLayout {
  private var isPausable: Boolean = true
  private var payload: Any? = null
  private var eventListener: DownloadEventListener? = null

  private var debouncer = Debouncer(750)

  private var currentProgress: Int = 0

  private var stateMachine = StateMachine.create<State, Event, ViewSideEffects> {
    initialState(State.Indeterminate)
    state<State.Indeterminate> {
      on<Event.DownloadStart> {
        debouncer.reset()
        transitionTo(State.InProgress, ViewSideEffects.ShowInProgressView)
      }
    }
    state<State.InProgress> {
      on<Event.PauseClick> {
        eventListener?.onActionClick(
            DownloadEventListener.Action(DownloadEventListener.Action.Type.PAUSE, payload))
        transitionTo(State.Paused, ViewSideEffects.ShowPausedView)
      }
      on<Event.InstallStart> {
        transitionTo(State.Indeterminate, ViewSideEffects.ShowInstallingView)
      }
    }
    state<State.Paused> {
      on<Event.ResumeClick> {
        eventListener?.onActionClick(
            DownloadEventListener.Action(DownloadEventListener.Action.Type.RESUME, payload))
        transitionTo(State.InProgress, ViewSideEffects.ShowInProgressView)
      }
      on<Event.CancelClick> {
        eventListener?.onActionClick(
            DownloadEventListener.Action(DownloadEventListener.Action.Type.CANCEL, payload))
        transitionTo(State.Indeterminate, ViewSideEffects.ShowCanceledView)
      }
    }
    onTransition { transition ->
      val validTransition = transition as? StateMachine.Transition.Valid ?: return@onTransition
      when (validTransition.sideEffect) {
        ViewSideEffects.ShowCanceledView -> {
          resetProgress()
          progressBar.isIndeterminate = true
          cancelButton.visibility = View.GONE
          if (isPausable) resumePauseButton.visibility = View.VISIBLE
          downloadProgressNumber.visibility = View.VISIBLE
          downloadState.setText(R.string.appview_short_downloading)
        }
        ViewSideEffects.ShowInProgressView -> {
          setProgress(currentProgress)
          progressBar.isIndeterminate = false
          cancelButton.visibility = View.GONE
          if (isPausable) resumePauseButton.visibility = View.VISIBLE
          resumePauseButton.play()
          downloadProgressNumber.visibility = View.VISIBLE
          downloadState.setText(R.string.appview_short_downloading)
        }
        ViewSideEffects.ShowPausedView -> {
          progressBar.isIndeterminate = false
          cancelButton.visibility = View.VISIBLE
          resumePauseButton.visibility = View.VISIBLE
          resumePauseButton.play()
          downloadProgressNumber.visibility = View.VISIBLE
          downloadState.setText(R.string.appview_short_downloading)
        }
        ViewSideEffects.ShowInstallingView -> {
          progressBar.isIndeterminate = true
          cancelButton.visibility = View.VISIBLE
          resumePauseButton.visibility = View.GONE
          downloadProgressNumber.visibility = View.GONE
          downloadState.setText(R.string.appview_short_installing)
        }
      }
    }
  }

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
      defStyleAttr) {
    inflate(context, R.layout.download_progress_view, this)
    retrievePreferences(attrs, defStyleAttr)
    setupClickListeners()
  }

  private fun retrievePreferences(attrs: AttributeSet?, defStyleAttr: Int) {
    val typedArray =
        context.obtainStyledAttributes(attrs, R.styleable.DownloadProgressView, defStyleAttr, 0)

    setProgressDrawable(typedArray.getDrawable(R.styleable.DownloadProgressView_progressDrawable))
    setEnableAnimations(
        typedArray.getBoolean(R.styleable.DownloadProgressView_enableAnimations, false))
    isPausable = typedArray.getBoolean(R.styleable.DownloadProgressView_isPausable, true)
    typedArray.recycle()
  }

  private fun setupClickListeners() {
    cancelButton.setOnClickListener {
      debouncer.execute {
        stateMachine.transition(Event.CancelClick)
      }
    }
    resumePauseButton.setOnClickListener {
      debouncer.execute {
        if (isPausable) {
          if (stateMachine.state == State.InProgress)
            stateMachine.transition(Event.PauseClick)
          else
            stateMachine.transition(Event.ResumeClick)
        }
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    setEventListener(null)
    payload = null
  }

  internal fun setEventListener(eventListener: DownloadEventListener?) {
    this.eventListener = eventListener
    if (eventListener == null) {
      cancelButton.setOnClickListener(null)
      resumePauseButton.setOnClickListener(null)
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun resetProgress() {
    currentProgress = 0
    progressBar.progress = currentProgress
    val progressPercent = "$currentProgress%"
    downloadProgressNumber.text = progressPercent
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  internal fun setState(state: State) {
    stateMachine = stateMachine.with { initialState(state) }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  internal fun consumeEvent(event: Event) {
    stateMachine.transition(event)
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  internal fun getState(): State {
    return stateMachine.state
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  internal fun setDebounceTime(time: Long) {
    debouncer = Debouncer(time)
  }

  /**
   * Sets if animations should be enabled
   * @param enableAnimations true to enable animations, false to disable animations
   */
  fun setEnableAnimations(enableAnimations: Boolean) {
    resumePauseButton.isAnimationsEnabled = enableAnimations
    rootLayout.layoutTransition = if (enableAnimations) LayoutTransition() else null
  }

  /**
   * Sets a specific drawable for progress
   * @param progressDrawable Progress drawable
   */
  fun setProgressDrawable(progressDrawable: Drawable?) {
    progressDrawable?.let { drawable ->
      progressBar.progressDrawable = drawable
    }
  }

  /**
   * Retrieves the events stream for this view.
   *
   * @return Observable<DownloadEventListener.Action>
   */
  @CheckResult
  fun events(): Observable<DownloadEventListener.Action> {
    return Observable.create(DownloadProgressViewEventOnSubscribe(this))
  }

  /**
   * Sets an optional payload to be retrieved with the event listener
   * E.g. Attaching an object identifying/describing the download
   *
   * @param payload
   */
  fun setPayload(payload: Any?) {
    this.payload = payload
  }

  /**
   * Sets the download progress. Note that it clips to 0-100.
   * If the view is in a paused state, it caches the value and sets it when it's in the resume state.
   *
   * @param progress, 0-100
   */
  fun setProgress(progress: Int) {
    if (stateMachine.state == State.Indeterminate) return
    currentProgress = Math.min(Math.max(progress, 0), 100)
    if (stateMachine.state == State.InProgress) {
      progressBar.progress = currentProgress
      val progressPercent = "$currentProgress%"
      downloadProgressNumber.text = progressPercent
    }
  }

  /**
   * Notifies the view that downloading will now begin.
   * It changes the view to a InProgress state.
   */
  fun startDownload() {
    stateMachine.transition(Event.DownloadStart)
  }

  /**
   * Notifies the view that installation will now begin. This implies that the download has ended.
   * It changes the view to an Indeterminate state.
   */
  fun startInstallation() {
    stateMachine.transition(Event.InstallStart)
  }

}