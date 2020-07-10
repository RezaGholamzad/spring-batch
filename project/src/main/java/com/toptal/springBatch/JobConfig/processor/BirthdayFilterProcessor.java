package com.toptal.springBatch.JobConfig.processor;

import com.toptal.springBatch.domain.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.Calendar;
import java.util.GregorianCalendar;

/*
    ItemProcessors transform input items and introduce business logic
    in an item-oriented processing scenario.
 */
@Slf4j
public class BirthdayFilterProcessor implements ItemProcessor<Customer, Customer> {

    int counter = 0;

    /*
        The method process() accepts one instance of the I class and may or may not return
        an instance of the same type. Returning null indicates that the item should
        not continue to be processed.
     */
    @Override
    public Customer process(final Customer item) throws Exception {
        log.info("BirthdayFilterProcessor " + counter++);
//        A customer must be born in the current month
        if (new GregorianCalendar().get(Calendar.MONTH) == item.getBirthday().get(Calendar.MONTH)){
            return item;
        }
        return null;
    }
}
