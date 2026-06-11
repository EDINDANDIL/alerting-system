package ru.services;

import ru.tinkoff.kora.common.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public final class DBExecutor implements AutoCloseable {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ExecutorService executor() {return executor;}

    @Override
    public void close() {executor.close();}
}