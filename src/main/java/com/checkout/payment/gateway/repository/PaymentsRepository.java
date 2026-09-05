package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentsRepository {
  private final HashMap<String, UUID> idempotentKeysMap = new HashMap<>();
  private final HashMap<UUID, PostPaymentResponse> payments = new HashMap<>();

  public void add(PostPaymentResponse payment) {
    payments.put(payment.getId(), payment);
  }

  public Optional<PostPaymentResponse> get(UUID id) {
    return Optional.ofNullable(payments.get(id));
  }

  public void addIdempotentKey(String idempotentKeys, UUID id) {
    idempotentKeysMap.put(idempotentKeys, id);
  }

  public Optional<UUID> getIdByIdempotentKey(String idempotentKeys) {
    return Optional.ofNullable(idempotentKeysMap.get(idempotentKeys));
  }

  // For unit test only
  public void reset() {
    idempotentKeysMap.clear();
    payments.clear();
  }
}
