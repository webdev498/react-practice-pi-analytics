package pi.analytics.admin.serviceaddress.service.helpers;

import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.ThreadSafeMockingProgress;

/**
 * @author shane.xie
 */
public class GrpcTestHelper {

  public static StreamObserverStubber stubber() {
    MockingProgress mockingProgress = ThreadSafeMockingProgress.mockingProgress();
    mockingProgress.stubbingStarted();
    mockingProgress.resetOngoingStubbing();
    return new StreamObserverStubber();
  }

  public static <T> StreamObserverStubber replyWith(final T... responses) {
    return stubber().replyWith(responses);
  }

  public static <T> StreamObserverStubber replyWithError(final Throwable throwable) {
    return stubber().replyWithError(throwable);
  }
}
