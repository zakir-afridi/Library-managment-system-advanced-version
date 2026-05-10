package com.library.employees;

import com.library.model.Employee;
import com.library.service.EmployeeService;

import java.util.List;

/**
 * EMPLOYEES BRANCH — service layer.
 */
public class EmployeesService {

    private final EmployeeService delegate = new EmployeeService();

    public boolean add(Employee e)                             { return delegate.addEmployee(e); }
    public boolean update(Employee e)                          { return delegate.updateEmployee(e); }
    public boolean delete(int empId)                           { return delegate.deleteEmployee(empId); }
    public Employee getById(int empId)                         { return delegate.getEmployeeById(empId); }
    public List<Employee> getPage(int page, int size)          { return delegate.getAllEmployees(page, size); }
    public List<Employee> search(String query)                 { return delegate.searchEmployees(query); }
    public boolean archive(int empId)                          { return delegate.archiveEmployee(empId); }
    public boolean unarchive(int empId)                        { return delegate.unarchiveEmployee(empId); }
    public int getTotalCount()                                 { return delegate.getTotalEmployees(); }
}
