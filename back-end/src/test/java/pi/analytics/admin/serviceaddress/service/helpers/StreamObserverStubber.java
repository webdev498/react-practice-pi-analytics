package pi.analytics.admin.serviceaddress.service.helpers;

import com.google.common.collect.Lists;

import org.mockito.internal.stubbing.StubberImpl;

import io.grpc.stub.StreamObserver;

/**
 * @author shane.xie@practiceinsight.io
 */
public class StreamObserverStubber extends StubberImpl {

  public <T> StreamObserverStubber replyWith(final T... responses) {
    return (StreamObserverStubber) doAnswer(invocation -> {
      StreamObserver<T> responseObserver = (StreamObserver<T>) invocation.getArguments()[1];
      Lists.newArrayList(responses).forEach(response -> responseObserver.onNext(response));
      responseObserver.onCompleted();
      return null;
    });
  }

  public <T> StreamObserverStubber replyWithError(final Throwable throwable) {
    return (StreamObserverStubber) doAnswer(invocation -> {
      StreamObserver<T> responseObserver = (StreamObserver<T>) invocation.getArguments()[1];
      responseObserver.onError(throwable);
      return null;
    });
  }
}
