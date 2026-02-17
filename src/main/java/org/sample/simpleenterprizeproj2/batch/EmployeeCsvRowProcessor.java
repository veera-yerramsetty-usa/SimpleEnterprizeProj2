package org.sample.simpleenterprizeproj2.batch;

import org.sample.simpleenterprizeproj2.model.Employee;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class EmployeeCsvRowProcessor implements ItemProcessor<Employee, EmployeeCsvRow> {

    @Override
    public EmployeeCsvRow process(Employee employee) {
        String departmentName = employee.getDepartment() != null
                ? employee.getDepartment().getName()
                : "";
        return new EmployeeCsvRow(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhone(),
                departmentName
        );
    }
}
