package ru.service;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Consumer;

public final class SafeStreamPublisher implements Flow.Publisher<ByteBuffer> {
    private final SubmissionPublisher<ByteBuffer> delegate = new SubmissionPublisher<>();
    private final Consumer<SafeStreamPublisher> cleanupCallback;

    public SafeStreamPublisher(Consumer<SafeStreamPublisher> cleanupCallback) {
        this.cleanupCallback = cleanupCallback;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        delegate.subscribe(new Flow.Subscriber<>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        subscription.request(n);
                    }

                    @Override
                    public void cancel() {
                        subscription.cancel();
                        cleanupCallback.accept(SafeStreamPublisher.this);
                    }
                });
            }

            @Override
            public void onNext(ByteBuffer item) {
                subscriber.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                subscriber.onError(throwable);
                cleanupCallback.accept(SafeStreamPublisher.this);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
                cleanupCallback.accept(SafeStreamPublisher.this);
            }
        });
    }

    public void submit(ByteBuffer item) {
        if (!delegate.isClosed()) {
            delegate.submit(item);
        }
    }

    public void close() {
        delegate.close();
    }
}
