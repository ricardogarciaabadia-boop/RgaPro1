package com.rgapro1.ocaso.domain.usecase;

import static org.junit.Assert.assertEquals;

import com.rgapro1.ocaso.domain.model.Policy;
import com.rgapro1.ocaso.domain.repository.PolicyRepository;

import org.junit.Test;

public class SavePolicyUseCaseTest {
    @Test
    public void execute_delegatesToRepository() {
        FakeRepository repository = new FakeRepository();
        Policy policy = new Policy("Ana", "Auto", "P-1", "123", "2030-01-01", "OCR", 1L);

        new SavePolicyUseCase(repository).execute(policy);

        assertEquals(policy, repository.saved);
    }

    private static final class FakeRepository implements PolicyRepository {
        private Policy saved;

        @Override
        public void save(Policy policy) {
            saved = policy;
        }
    }
}
