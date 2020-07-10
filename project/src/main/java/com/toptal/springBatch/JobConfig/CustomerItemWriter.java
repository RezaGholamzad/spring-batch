package com.toptal.springBatch.JobConfig;

import com.toptal.springBatch.domain.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import javax.annotation.PreDestroy;
import java.io.*;
import java.util.List;

/*
    For outputting the data, Spring Batch provides the interface ItemWriter
    for serializing objects as necessary
 */
@Slf4j
public class CustomerItemWriter implements ItemWriter<Customer>, Closeable {

    private final PrintWriter writer;

    int counter = 0;

    public CustomerItemWriter() {
        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream("output.txt");
        } catch (FileNotFoundException e) {
            outputStream = System.out;
//            e.printStackTrace();
        }
        this.writer = new PrintWriter(outputStream);
    }

    /*
        The write() method is responsible for making sure that any internal buffers are flushed.
        If a transaction is active, it will also usually be necessary to discard the output on a
        subsequent rollback. The resource to which the writer is sending data should normally
        be able to handle this itself. There are standard implementations such as CompositeItemWriter,
        JdbcBatchItemWriter, JmsItemWriter, JpaItemWriter, SimpleMailMessageItemWriter, and others.
     */
    @Override
    public void write(List<? extends Customer> items) throws Exception {
        for (Customer item : items){
            log.info("CustomerItemWriter " + counter++);
            writer.println(item.toString());
        }
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        writer.close();
    }
}
