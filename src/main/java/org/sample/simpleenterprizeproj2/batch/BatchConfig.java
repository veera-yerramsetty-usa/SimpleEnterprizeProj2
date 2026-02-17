package org.sample.simpleenterprizeproj2.batch;

import org.sample.simpleenterprizeproj2.model.Employee;
import org.sample.simpleenterprizeproj2.repository.EmployeeRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class BatchConfig {

    @Bean
    public Job softDeletePurgeJob(JobRepository jobRepository,
                                  Step purgeStep,
                                  JobCompletionListener listener) {
        return new JobBuilder("softDeletePurgeJob", jobRepository)
                .listener(listener)
                .start(purgeStep)
                .build();
    }

    @Bean
    public Step purgeStep(JobRepository jobRepository,
                          PlatformTransactionManager transactionManager,
                          SoftDeletePurgeTasklet tasklet) {
        return new StepBuilder("purgeStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job employeeExportJob(JobRepository jobRepository,
                                 Step exportStep,
                                 JobCompletionListener listener) {
        return new JobBuilder("employeeExportJob", jobRepository)
                .listener(listener)
                .start(exportStep)
                .build();
    }

    @Bean
    public Step exportStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           RepositoryItemReader<Employee> employeeReader,
                           EmployeeCsvRowProcessor processor,
                           FlatFileItemWriter<EmployeeCsvRow> csvWriter) {
        return new StepBuilder("exportStep", jobRepository)
                .<Employee, EmployeeCsvRow>chunk(100, transactionManager)
                .reader(employeeReader)
                .processor(processor)
                .writer(csvWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Employee> employeeReader(EmployeeRepository employeeRepository) {
        return new RepositoryItemReaderBuilder<Employee>()
                .name("employeeReader")
                .repository(employeeRepository)
                .methodName("findAll")
                .sorts(Map.of("id", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<EmployeeCsvRow> csvWriter(
            @Value("#{jobParameters['outputPath']}") String outputPath) {
        return new FlatFileItemWriterBuilder<EmployeeCsvRow>()
                .name("csvWriter")
                .resource(new FileSystemResource(outputPath))
                .delimited()
                .delimiter(",")
                .names("id", "firstName", "lastName", "email", "phone", "departmentName")
                .headerCallback(writer -> writer.write("id,firstName,lastName,email,phone,departmentName"))
                .build();
    }
}
