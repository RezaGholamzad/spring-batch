package com.toptal.springBatch;

import com.toptal.springBatch.JobConfig.CustomerReportJobConfig;
import com.toptal.springBatch.domain.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.beans.XMLEncoder;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.util.*;


//enables Spring Batch features and provides a base configuration for setting up batch jobs.
@EnableBatchProcessing
@Slf4j
@EnableScheduling
@SpringBootApplication
public class SpringBatchApplication {

    public static void main(String[] args) {
        prepareTestData(100);
        SpringApplication.run(SpringBatchApplication.class, args);
    }

    public static void prepareTestData(final int amount){
        final int actualYear = new GregorianCalendar().get(Calendar.YEAR);
        final Collection<Customer> customers = new LinkedList<>();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < amount; i++) {

            final Calendar birthday = new GregorianCalendar();
            birthday.set(Calendar.YEAR, secureRandom
                    // random number between min and max : nextInt(max - min) + min
                    .nextInt(actualYear - (actualYear - 100)) + (actualYear - 100));
            birthday.set(Calendar.DAY_OF_YEAR, secureRandom
                    .nextInt(birthday.getActualMaximum(Calendar.DAY_OF_YEAR)) + 1);

            final Customer customer = new Customer();
            customer.setId(i);
            customer.setName(UUID.randomUUID().toString().replaceAll("[^a-z]", ""));
            customer.setBirthday(birthday);
            customer.setTransactions(secureRandom.nextInt(100));
            customers.add(customer);

            try (final XMLEncoder encoder =
                         new XMLEncoder(new FileOutputStream(CustomerReportJobConfig.XML_FILE))){
                encoder.writeObject(customers);

            } catch (final FileNotFoundException e) {
                log.error(e.getMessage(), e);
                System.exit(-1);
//                e.printStackTrace();
            }

        }
    }
}
