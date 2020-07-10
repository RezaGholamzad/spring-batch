package com.toptal.springBatch.JobConfig.processor;

import com.toptal.springBatch.domain.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
import org.springframework.batch.item.validator.ValidationException;

/*
    Spring provides few standard processors,
    such as CompositeItemProcessor that passes the item through a sequence of injected
    ItemProcessors and a ValidatingItemProcessor that validates input.
 */
@Slf4j
public class TransactionValidatingProcessor extends ValidatingItemProcessor<Customer> {

    int counter = 0;

    public TransactionValidatingProcessor(final int limit) {
        super(item -> {
//          A customer must have less than five completed transactions
            if (item.getTransactions() >= limit){
                throw new ValidationException("Customer has less than " + limit + " transactions");
            }
        });
        log.info("TransactionValidatingProcessor " + counter++);
        setFilter(true);
    }
}
