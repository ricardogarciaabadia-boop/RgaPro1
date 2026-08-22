package com.rgapro1.ocaso.domain.repository;

import com.rgapro1.ocaso.domain.model.Policy;
import java.util.List;

/** Domain boundary for policy/client persistence. UI code must depend on this contract, not storage details. */
public interface PolicyRepository {
    List<Policy> getAll();
    void save(Policy policy);
    void deleteLast();
}
