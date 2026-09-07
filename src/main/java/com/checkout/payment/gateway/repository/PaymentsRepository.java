package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentsRepository {
  private final MeterRegistry meterRegistry;
  private final HashMap<String, UUID> idempotentKeysMap = new HashMap<>();
  private final HashMap<UUID, PostPaymentResponse> payments = new HashMap<>();

  public PaymentsRepository(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void add(PostPaymentResponse payment) {
    payments.put(payment.getId(), payment);
  }

  public Optional<PostPaymentResponse> get(UUID id) {
    Timer.Sample executionTimer = Timer.start(meterRegistry);
    Optional<PostPaymentResponse> result = Optional.ofNullable(payments.get(id));
    executionTimer.stop(meterRegistry.timer("db.repository.operations", "operation", "get"));
    return result;
  }

  public void addIdempotentKey(String idempotentKeys, UUID id) {
    idempotentKeysMap.put(idempotentKeys, id);
  }

  public Optional<UUID> getIdByIdempotentKey(String idempotentKeys) {
    Timer.Sample executionTimer = Timer.start(meterRegistry);
    Optional<UUID> result = Optional.ofNullable(idempotentKeysMap.get(idempotentKeys));
    executionTimer.stop(meterRegistry.timer("db.repository.operations", "operation", "getIdByIdempotentKey"));
    return result;
  }

  // For unit test only
  public void reset() {
    idempotentKeysMap.clear();
    payments.clear();
  }
}
