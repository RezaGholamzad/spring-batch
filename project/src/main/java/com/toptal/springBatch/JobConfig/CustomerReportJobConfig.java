package com.toptal.springBatch.JobConfig;

import com.toptal.springBatch.JobConfig.processor.BirthdayFilterProcessor;
import com.toptal.springBatch.JobConfig.processor.TransactionValidatingProcessor;
import com.toptal.springBatch.domain.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PreDestroy;
import java.util.Arrays;

@Slf4j
@Configuration
public class CustomerReportJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;

    public static final String XML_FILE = "database.xml";
    private static final String JOB_NAME = "customerReportJob";
    public static final String TASKLET_STEP = "taskletStep";

    public CustomerReportJobConfig(JobBuilderFactory jobBuilderFactory,
                                   StepBuilderFactory stepBuilderFactory,
                                   JobLauncher jobLauncher,
                                   JobExplorer jobExplorer) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
    }

    /*
        There are two main approaches to building a step :
        1) One approach, is tasklet-based.
        A Tasklet supports a simple interface that has only one method, execute(), which is
        called repeatedly until it either returns RepeatStatus.FINISHED or throws an exception to
        signal a failure. Each call to the Tasklet is wrapped in a transaction.
     */
    @Bean
    public Step taskletStep(){
        return stepBuilderFactory.get(TASKLET_STEP)
                .tasklet(tasklet())
                .build();
    }
    @Bean
    public Tasklet tasklet(){
        return (contribution, chunkContext) -> {
            log.info("Executing tasklet step");
            return RepeatStatus.FINISHED;
        };
    }

//    ######################################################

    /*
        Spring Batch processes items in chunks. A job reads and writes items in small chunks.
        Chunk processing allows streaming data instead of loading all the data in memory.
        By default, chunk processing is single threaded and usually performs well,
        but has an option to distribute processing on multiple threads or physical nodes as well.
        Spring Batch collects items one at a time from the ItemReader into a configurable-sized chunk.
        Spring Batch then sends the chunk to the ItemWriter and goes back to using the ItemReader
        to create another chunk, and so on, until the input is exhausted.
        Spring Batch provides an optional processing where a job can process (transform)
        items before sending them to ItemWriter. It is called ItemProcessor.
        Spring Batch also handles transactions and errors around read and write operations.
     */

    /*
        2) Another approach, chunk-oriented processing, refers to reading the data sequentially
        and creating “chunks” that will be written out within a transaction boundary.
        Each individual item is read in from an ItemReader, handed to an ItemProcessor, and aggregated.
        Once the number of items read equals the commit interval(chunkSize), the entire chunk is written out via
        the ItemWriter, and then the transaction is committed.
        The chunkStep() method builds a step that processes items in chunks with the size provided,
        with each chunk then being passed to the specified reader, processor, and writer.
        A chunk-oriented step can be configured as follows:
     */
    @Bean
    public Step chunkStep(){
        return stepBuilderFactory.get("chunkStep")
                .<Customer, Customer>chunk(20)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    /*
        A Spring bean for ItemReader implementation is created with
        the @Component and @StepScope annotations,
        letting Spring know that this class is a step-scoped Spring component and will
        be created once per step execution
     */
    @StepScope
    @Bean
    public ItemReader<Customer> reader(){
        return new CustomerItemReader(XML_FILE);
    }

    @StepScope
    @Bean
    public BirthdayFilterProcessor birthdayFilterProcessor() {
        return new BirthdayFilterProcessor();
    }

    @StepScope
    @Bean
    public TransactionValidatingProcessor transactionValidatingProcessor() {
        return new TransactionValidatingProcessor(5);
    }

    @StepScope
    @Bean
    public ItemProcessor<Customer, Customer> processor(){
        final CompositeItemProcessor<Customer, Customer> processor =
                new CompositeItemProcessor<>();
        processor.setDelegates(Arrays.asList(
                birthdayFilterProcessor(),
                transactionValidatingProcessor()));
        return processor;
    }

    @StepScope
    @Bean
    public ItemWriter<Customer> writer(){
        return new CustomerItemWriter();
    }

//    ######################################################

    @Bean
    public Job customerReportJob(){
        return jobBuilderFactory.get(JOB_NAME)
                .start(taskletStep())
                .next(chunkStep())
                .build();
    }

//    ######################################################

    @Scheduled(fixedRate = 5000)
    public void run() throws JobParametersInvalidException,
            JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException {

        /*
            the job will succeed the first time only. When it launches the second time
            (i.e. after five seconds), it will generate the following messages in the logs
            (note that in previous versions of Spring Batch a
            JobInstanceAlreadyCompleteException would have been thrown).
            This happens because only unique JobInstances may be created and executed
            and Spring Batch has no way of distinguishing between the first and second JobInstance.
            There are two ways of avoiding this problem when you schedule a batch job.
            One is to be sure to introduce one or more unique parameters :
         */
        JobExecution jobExecution = jobLauncher.run(customerReportJob(),
                new JobParametersBuilder()
//                        be sure to introduce one or more unique parameters
                        .addLong("uniqueness", System.nanoTime())
                        .toJobParameters());

        log.info("Exit status: {}", jobExecution.getStatus());
    }

/*
    Alternatively, you can launch the next job in a sequence of JobInstances determined by
    the JobParametersIncrementer attached to the specified job with
    SimpleJobOperator.startNextInstance():
*/
//    @Autowired
//    private JobOperator jobOperator;
//
//    @Scheduled(fixedRate = 5000)
//    public void run() throws Exception {
//        List<JobInstance> lastInstances = jobExplorer.getJobInstances(JOB_NAME, 0, 1);
//        if (lastInstances.isEmpty()) {
//            jobLauncher.run(customerReportJob(), new JobParameters());
//        } else {
//            jobOperator.startNextInstance(JOB_NAME);
//        }
//    }

//    ######################################################

    @PreDestroy
    public void destroy() throws NoSuchJobException {
        jobExplorer.getJobNames().forEach(name -> log.info("job name: {}", name));
        jobExplorer.getJobInstances(JOB_NAME, 0,
                jobExplorer.getJobInstanceCount(JOB_NAME)).forEach(
                jobInstance -> {
                    log.info("job instance id {}", jobInstance.getInstanceId());
                }
        );
    }




}
