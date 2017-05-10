/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 12/08/2016.
 */

package cm.aptoide.pt.v8engine.payment;

import android.content.Context;
import cm.aptoide.pt.v8engine.payment.exception.PaymentFailureException;
import cm.aptoide.pt.v8engine.payment.repository.PaymentRepositoryFactory;
import cm.aptoide.pt.v8engine.payment.repository.ProductRepositoryFactory;
import java.util.List;
import rx.Observable;
import rx.Single;

public class AptoideBilling {

  private final ProductRepositoryFactory productRepositoryFactory;
  private final PaymentRepositoryFactory paymentRepositoryFactory;

  public AptoideBilling(ProductRepositoryFactory productRepositoryFactory,
      PaymentRepositoryFactory paymentRepositoryFactory) {
    this.productRepositoryFactory = productRepositoryFactory;
    this.paymentRepositoryFactory = paymentRepositoryFactory;
  }

  public Single<Product> getPaidAppProduct(long appId, String storeName, boolean sponsored) {
    return productRepositoryFactory.getPaidAppProductRepository()
        .getProduct(appId, sponsored, storeName);
  }

  public Single<Product> getInAppProduct(int apiVersion, String packageName, String sku,
      String type, String developerPayload) {
    return productRepositoryFactory.getInAppProductRepository()
        .getProduct(apiVersion, packageName, sku, type, developerPayload);
  }

  public Single<List<Payment>> getPayments(Context context, Product product) {
    return productRepositoryFactory.getProductRepository(product).getPayments(context, product);
  }

  public Single<Payment> getPayment(Context context, int paymentId, Product product) {
    return getPayments(context, product).flatMapObservable(payments -> Observable.from(payments)
        .filter(payment -> payment.getId() == paymentId)
        .switchIfEmpty(Observable.error(
            new PaymentFailureException("Payment " + paymentId + "not available")))).toSingle();
  }

  public Observable<PaymentConfirmation> getConfirmation(Product product) {
    return paymentRepositoryFactory.getPaymentConfirmationRepository(product)
        .getPaymentConfirmation(product)
        .distinctUntilChanged(confirmation -> confirmation.getStatus());
  }

  public Single<Purchase> getPurchase(Product product) {
    return productRepositoryFactory.getProductRepository(product).getPurchase(product);
  }
}