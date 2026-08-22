package com.rgapro1.ocaso.domain.usecase;

import com.rgapro1.ocaso.domain.model.Policy;
import com.rgapro1.ocaso.domain.repository.PolicyRepository;

/** Domain operation for persisting a policy without exposing storage details to UI. */
public final class SavePolicyUseCase {
    private final PolicyRepository repository;

    public SavePolicyUseCase(PolicyRepository repository) {
        this.repository = repository;
    }

    public void execute(Policy policy) {
        repository.save(policy);
    }
}
