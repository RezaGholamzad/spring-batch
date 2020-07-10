package com.toptal.springBatch.JobConfig;

import com.toptal.springBatch.domain.Customer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.IteratorItemReader;

import lombok.extern.slf4j.Slf4j;

import java.beans.XMLDecoder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/*
    An ItemReader provides the data and is expected to be stateful.
    It is typically called multiple times for each batch, with each call to read()
    returning the next value and finally returning null when all input data has been exhausted.
 */
@Slf4j
public class CustomerItemReader implements ItemReader<Customer> {

    private final String fileName;

    int counter = 0;

    private ItemReader<Customer> itemReader;

    public CustomerItemReader(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public Customer read() throws Exception, UnexpectedInputException,
            ParseException, NonTransientResourceException {

        if (itemReader == null){
            log.info("Creating iterator item reader");
            itemReader = new IteratorItemReader<>(customers());
        }

        log.info("Reading next customer " + counter++);
        return itemReader.read();
    }

    private List<Customer> customers() throws FileNotFoundException {
        try (XMLDecoder decoder = new XMLDecoder(new FileInputStream(fileName))){
            return (List<Customer>) decoder.readObject();
        }
    }
}
