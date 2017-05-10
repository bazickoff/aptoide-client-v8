/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 19/08/2016.
 */

package cm.aptoide.pt.v8engine.view.payment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.iab.ErrorCodeFactory;
import cm.aptoide.pt.imageloader.ImageLoader;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.payment.PaymentAnalytics;
import cm.aptoide.pt.v8engine.payment.Product;
import cm.aptoide.pt.v8engine.payment.Purchase;
import cm.aptoide.pt.v8engine.payment.products.InAppBillingProduct;
import cm.aptoide.pt.v8engine.payment.products.PaidAppProduct;
import cm.aptoide.pt.v8engine.payment.products.AbstractProduct;
import cm.aptoide.pt.v8engine.presenter.PaymentPresenter;
import cm.aptoide.pt.v8engine.presenter.PaymentSelector;
import cm.aptoide.pt.v8engine.presenter.PaymentView;
import cm.aptoide.pt.v8engine.view.BaseActivity;
import cm.aptoide.pt.v8engine.view.account.AccountNavigator;
import cm.aptoide.pt.v8engine.view.recycler.displayable.SpannableFactory;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxRadioGroup;
import java.util.List;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class PaymentActivity extends BaseActivity implements PaymentView {

  private static final String SELECTED_PAYMENT_ID = "selected_payment_id";
  private static final int PAYPAL_PAYMENT_ID = 17;
  private static final String EXTRA_APP_ID =
      "cm.aptoide.pt.v8engine.view.payment.intent.extra.APP_ID";
  private static final String EXTRA_STORE_NAME =
      "cm.aptoide.pt.v8engine.view.payment.intent.extra.STORE_NAME";
  private static final String EXTRA_SPONSORED =
      "cm.aptoide.pt.v8engine.view.payment.intent.extra.SPONSORED";
  private static final String EXTRA_API_VERSION =
      "cm.aptoide.pt.v8engine.view.payment.intent.extra.API_VERSION";
  private static final String EXTRA_PACKAGE_NAME =
      "cm.aptoide.pt.v8engine.view.payment.intent.extra.PACKAGE_NAME";
  private static final String EXTRA_SKU = "cm.aptoide.pt.v8engine.view.payment.intent.extra.SKU";
  private static final String EXTRA_TYPE = "cm.aptoide.pt.v8engine.view.payment.intent.extra.TYPE";
  private static final String EXTRA_DEVELOPER_PAYLOAD =
      "cm.aptoide.pt.v8engine.view.payment.intent.extra.DEVELOPER_PAYLOAD";

  private View overlay;
  private View body;
  private View progressView;
  private RadioGroup paymentRadioGroup;
  private ImageView productIcon;
  private TextView productName;
  private TextView productDescription;
  private TextView noPaymentsText;
  private Button cancelButton;
  private Button buyButton;
  private TextView productPrice;

  private PurchaseIntentFactory intentFactory;
  private AlertDialog networkErrorDialog;
  private AlertDialog unknownErrorDialog;
  private SparseArray<PaymentViewModel> paymentMap;
  private SpannableFactory spannableFactory;

  public static Intent getIntent(Context context, long appId, String storeName, boolean sponsored) {
    final Intent intent = new Intent(context, PaymentActivity.class);
    intent.putExtra(EXTRA_APP_ID, appId);
    intent.putExtra(EXTRA_STORE_NAME, storeName);
    intent.putExtra(EXTRA_SPONSORED, sponsored);
    return intent;
  }

  public static Intent getIntent(Context context, int apiVersion, String packageName, String sku,
      String type, String developerPayload) {
    final Intent intent = new Intent(context, PaymentActivity.class);
    intent.putExtra(EXTRA_API_VERSION, apiVersion);
    intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
    intent.putExtra(EXTRA_TYPE, type);
    intent.putExtra(EXTRA_SKU, sku);
    intent.putExtra(EXTRA_DEVELOPER_PAYLOAD, developerPayload);
    return intent;
  }

  @SuppressLint("UseSparseArrays") @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_payment);

    spannableFactory = new SpannableFactory();
    overlay = findViewById(R.id.payment_activity_overlay);
    progressView = findViewById(R.id.activity_payment_global_progress_bar);
    noPaymentsText = (TextView) findViewById(R.id.activity_payment_no_payments_text);

    productIcon = (ImageView) findViewById(R.id.activity_payment_product_icon);
    productName = (TextView) findViewById(R.id.activity_payment_product_name);
    productDescription = (TextView) findViewById(R.id.activity_payment_product_description);

    body = findViewById(R.id.activity_payment_body);
    productPrice = (TextView) findViewById(R.id.activity_product_price);
    paymentRadioGroup = (RadioGroup) findViewById(R.id.activity_payment_list);

    cancelButton = (Button) findViewById(R.id.activity_payment_cancel_button);
    buyButton = (Button) findViewById(R.id.activity_payment_buy_button);

    paymentMap = new SparseArray<>();
    intentFactory = new PurchaseIntentFactory(new ErrorCodeFactory());
    final ContextThemeWrapper dialogTheme =
        new ContextThemeWrapper(this, R.style.AptoideThemeDefault);

    networkErrorDialog = new AlertDialog.Builder(dialogTheme).setMessage(R.string.connection_error)
        .setPositiveButton(android.R.string.ok, null)
        .create();
    unknownErrorDialog =
        new AlertDialog.Builder(dialogTheme).setMessage(R.string.having_some_trouble)
            .setPositiveButton(android.R.string.ok, null)
            .create();

    final AbstractProduct product = getIntent().getParcelableExtra(EXTRA_APP_ID);
    final AptoideAccountManager accountManager =
        ((V8Engine) getApplicationContext()).getAccountManager();
    final PaymentAnalytics paymentAnalytics =
        ((V8Engine) getApplicationContext()).getPaymentAnalytics();

    attachPresenter(
        new PaymentPresenter(this, this, ((V8Engine) getApplicationContext()).getAptoideBilling(),
            accountManager, new PaymentSelector(PAYPAL_PAYMENT_ID,
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())),
            new AccountNavigator(getFragmentNavigator(), accountManager, getActivityNavigator()),
            paymentAnalytics, getIntent().getLongExtra(EXTRA_APP_ID, -1),
            getIntent().getStringExtra(EXTRA_STORE_NAME),
            getIntent().getBooleanExtra(EXTRA_SPONSORED, false),
            getIntent().getIntExtra(EXTRA_API_VERSION, -1), getIntent().getStringExtra(EXTRA_TYPE),
            getIntent().getStringExtra(EXTRA_SKU), getIntent().getStringExtra(EXTRA_PACKAGE_NAME),
            getIntent().getStringExtra(EXTRA_DEVELOPER_PAYLOAD)), savedInstanceState);
  }

  @Override public Observable<PaymentViewModel> paymentSelection() {
    return RxRadioGroup.checkedChanges(paymentRadioGroup)
        .map(paymentId -> paymentMap.get(paymentId))
        .filter(paymentViewModel -> paymentViewModel != null);
  }

  @Override public Observable<Void> cancellationSelection() {
    return RxView.clicks(cancelButton)
        .subscribeOn(AndroidSchedulers.mainThread())
        .unsubscribeOn(AndroidSchedulers.mainThread());
  }

  @Override public Observable<Void> tapOutsideSelection() {
    return RxView.clicks(overlay)
        .subscribeOn(AndroidSchedulers.mainThread())
        .unsubscribeOn(AndroidSchedulers.mainThread());
  }

  @Override public Observable<Void> buySelection() {
    return RxView.clicks(buyButton)
        .subscribeOn(AndroidSchedulers.mainThread())
        .unsubscribeOn(AndroidSchedulers.mainThread());
  }

  @Override public void showLoading() {
    progressView.setVisibility(View.VISIBLE);
  }

  @Override public void showPayments(List<PaymentViewModel> payments) {
    paymentRadioGroup.removeAllViews();
    noPaymentsText.setVisibility(View.GONE);
    body.setVisibility(View.VISIBLE);
    buyButton.setVisibility(View.VISIBLE);
    paymentMap.clear();

    RadioButton radioButton;
    CharSequence radioText;
    for (PaymentViewModel payment : payments) {
      radioButton =
          (RadioButton) getLayoutInflater().inflate(R.layout.payment_item, paymentRadioGroup,
              false);
      radioButton.setId(payment.getId());
      if (TextUtils.isEmpty(payment.getDescription())) {
        radioText = payment.getName();
      } else {
        radioText =
            spannableFactory.createTextAppearanceSpan(this, R.style.TextAppearance_Aptoide_Caption,
                payment.getName() + "\n" + payment.getDescription(), payment.getDescription());
      }
      radioButton.setText(radioText);
      radioButton.setChecked(payment.isSelected());

      paymentMap.append(payment.getId(), payment);
      paymentRadioGroup.addView(radioButton);
    }
  }

  @Override public void showProduct(Product product) {
    ImageLoader.with(this).load(product.getIcon(), productIcon);
    productName.setText(product.getTitle());
    productDescription.setText(product.getDescription());
    productPrice.setText(
        product.getPrice().getCurrencySymbol() + " " + product.getPrice().getAmount());
  }

  @Override public void hideLoading() {
    progressView.setVisibility(View.GONE);
  }

  @Override public void dismiss(Purchase purchase) {
    finish(RESULT_OK, intentFactory.create(purchase));
  }

  @Override public void dismiss(Throwable throwable) {
    finish(RESULT_CANCELED, intentFactory.create(throwable));
  }

  @Override public void dismiss() {
    finish(RESULT_CANCELED, intentFactory.createFromCancellation());
  }

  @Override public void navigateToAuthorizationView(int paymentId, Product product) {
    if (product instanceof InAppBillingProduct) {
      startActivity(WebAuthorizationActivity.getIntent(this, paymentId,
          ((InAppBillingProduct) product).getApiVersion(),
          ((InAppBillingProduct) product).getPackageName(),
          ((InAppBillingProduct) product).getType(), ((InAppBillingProduct) product).getSku(),
          ((InAppBillingProduct) product).getDeveloperPayload()));
    } else if (product instanceof PaidAppProduct) {
      startActivity(
          WebAuthorizationActivity.getIntent(this, paymentId, ((PaidAppProduct) product).getAppId(),
              ((PaidAppProduct) product).getStoreName(), ((PaidAppProduct) product).isSponsored()));
    } else {
      throw new IllegalArgumentException("Invalid product. Only in app and paid apps supported");
    }
  }

  @Override public void showPaymentsNotFoundMessage() {
    body.setVisibility(View.GONE);
    noPaymentsText.setVisibility(View.VISIBLE);
    buyButton.setVisibility(View.GONE);
  }

  @Override public void showNetworkError() {
    if (!networkErrorDialog.isShowing() && !unknownErrorDialog.isShowing()) {
      networkErrorDialog.show();
    }
  }

  @Override public void showUnknownError() {
    if (!networkErrorDialog.isShowing() && !unknownErrorDialog.isShowing()) {
      unknownErrorDialog.show();
    }
  }

  @Override public void hideAllErrors() {
    networkErrorDialog.dismiss();
    unknownErrorDialog.dismiss();
  }

  private void finish(int code, Intent intent) {
    setResult(code, intent);
    finish();
  }

  private void finish(int code) {
    setResult(code);
    finish();
  }
}
