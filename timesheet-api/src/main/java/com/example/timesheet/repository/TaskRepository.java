package com.example.timesheet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.timesheet.domain.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
   @Query("""
           select t from Task t
           join fetch t.project p
           where t.active = true and p.active = true
           order by p.code, t.name
           """)
    List<Task> findAllActiveWithProject();


}
