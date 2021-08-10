package com.onthegomap.flatmap.stats;

import static io.prometheus.client.Collector.NANOSECONDS_PER_SECOND;

import com.onthegomap.flatmap.util.LogUtil;
import com.onthegomap.flatmap.util.MemoryEstimator;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

public interface Stats extends AutoCloseable {

  default void printSummary() {
    timers().printSummary();
  }

  default Timers.Finishable startStage(String name) {
    LogUtil.setStage(name);
    var timer = timers().startTimer(name);
    return () -> {
      timer.stop();
      LogUtil.clearStage();
    };
  }

  default void gauge(String name, Number value) {
    gauge(name, () -> value);
  }

  void gauge(String name, Supplier<Number> value);

  void emittedFeatures(int z, String layer, int coveringTiles);

  void wroteTile(int zoom, int bytes);

  Timers timers();

  void monitorFile(String features, Path featureDbPath);

  void monitorInMemoryObject(String name, MemoryEstimator.HasEstimate heapObject);

  void counter(String name, Supplier<Number> supplier);

  default Counter.MultiThreadCounter longCounter(String name) {
    Counter.MultiThreadCounter counter = Counter.newMultiThreadCounter();
    counter(name, counter::get);
    return counter;
  }

  default Counter.MultiThreadCounter nanoCounter(String name) {
    Counter.MultiThreadCounter counter = Counter.newMultiThreadCounter();
    counter(name, () -> counter.get() / NANOSECONDS_PER_SECOND);
    return counter;
  }

  void counter(String name, String label, Supplier<Map<String, Counter.Readable>> values);

  void processedElement(String elemType, String layer);

  void dataError(String stat);

  static Stats inMemory() {
    return new InMemory();
  }

  static Stats prometheusPushGateway(String destination, String job, Duration interval) {
    return new PrometheusStats(destination, job, interval);
  }

  class InMemory implements Stats {

    private final Timers timers = new Timers();

    @Override
    public void wroteTile(int zoom, int bytes) {
    }

    @Override
    public Timers timers() {
      return timers;
    }

    @Override
    public void monitorFile(String features, Path featureDbPath) {
    }

    @Override
    public void monitorInMemoryObject(String name, MemoryEstimator.HasEstimate heapObject) {
    }

    @Override
    public void counter(String name, Supplier<Number> supplier) {
    }

    @Override
    public Counter.MultiThreadCounter longCounter(String name) {
      return Counter.newMultiThreadCounter();
    }

    @Override
    public Counter.MultiThreadCounter nanoCounter(String name) {
      return Counter.newMultiThreadCounter();
    }

    @Override
    public void counter(String name, String label, Supplier<Map<String, Counter.Readable>> values) {
    }

    @Override
    public void processedElement(String elemType, String layer) {
    }

    @Override
    public void dataError(String stat) {
    }

    @Override
    public void gauge(String name, Supplier<Number> value) {
    }

    @Override
    public void emittedFeatures(int z, String layer, int coveringTiles) {
    }

    @Override
    public void close() throws Exception {

    }
  }
}
