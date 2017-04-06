package com.a.eye.skywalking.collector.actor;

/**
 * @author pengys5
 */
public interface Provider {

    WorkerRef create(AbstractWorker workerOwner) throws IllegalArgumentException, ProviderNotFoundException;
}