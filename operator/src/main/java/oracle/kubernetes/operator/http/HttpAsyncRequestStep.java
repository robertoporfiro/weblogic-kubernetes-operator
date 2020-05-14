// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import oracle.kubernetes.operator.work.AsyncFiber;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;

/**
 * An asynchronous step to handle http requests.
 */
public class HttpAsyncRequestStep extends Step {

  interface FutureFactory {
    CompletableFuture<HttpResponse<String>> createFuture(HttpRequest request);
  }

  private static final long DEFAULT_TIMEOUT_SECONDS = 5;

  private final FutureFactory factory;
  private final HttpRequest request;
  private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

  HttpAsyncRequestStep(HttpRequest request, HttpResponseStep responseStep, FutureFactory factory) {
    super(responseStep);
    this.request = request;
    this.factory = factory;
  }

  private HttpAsyncRequestStep(HttpRequest request, HttpResponseStep responseStep) {
    this(request, responseStep, DEFAULT_FACTORY);
  }

  /**
   * Creates a step to send a GET request to a server. If a response is received, processing
   * continues with the response step. If none is received within the timeout, the fiber is terminated.
   * @param url the URL of the targeted server
   * @param responseStep the step to handle the response
   * @return a new step to run as part of a fiber, linked to the response step
   */
  public static HttpAsyncRequestStep createGetRequest(String url, HttpResponseStep responseStep) {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
    return new HttpAsyncRequestStep(request, responseStep);
  }

  /**
   * Overrides the default timeout for this request.
   * @param timeoutSeconds the new timeout, in seconds
   * @return this step
   */
  HttpAsyncRequestStep withTimeoutSeconds(long timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
    return this;
  }

  @Override
  public NextAction apply(Packet packet) {
    AsyncProcessing processing = new AsyncProcessing(packet);
    return doSuspend(processing::process);
  }

  class AsyncProcessing {
    private Packet packet;
    private CompletableFuture<HttpResponse<String>> future;

    AsyncProcessing(Packet packet) {
      this.packet = packet;
    }

    void process(AsyncFiber fiber) {
      future = factory.createFuture(request);
      future.thenAccept(response -> resume(fiber, response));
      fiber.scheduleOnce(timeoutSeconds, TimeUnit.SECONDS, () -> checkTimeout(fiber));
    }

    private void checkTimeout(AsyncFiber fiber) {
      if (!future.isDone()) {
        fiber.terminate(new RuntimeException("timeout"), packet);
      }
    }

    private void resume(AsyncFiber fiber, HttpResponse<String> response) {
      HttpResponseStep.addToPacket(packet, response);
      fiber.resume(packet);
    }
  }


  private static FutureFactory DEFAULT_FACTORY
        = request -> takeClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());

  private static HttpClient takeClient() {
    return HttpClient.newHttpClient();
  }
}
