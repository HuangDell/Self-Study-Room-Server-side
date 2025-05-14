package com.studyroom.service;

import com.studyroom.model.Student;
import com.studyroom.repository.StudentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StudentService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // 如果没有学生账户，创建一个默认账户
        if (studentRepository.findByUsername("student").isEmpty()) {
            Student student = new Student();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("password"));
            student.setName("Test Student");
            student.setStudentId("202500001");
            student.setEmail("student@gmail.com");
            student.setPhone("1234568910");
            student.setType(1);
            studentRepository.save(student);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Student student = studentRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Student not found"));

        return new User(
                student.getUsername(),
                student.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

    public void register(Student student) {

        if (studentRepository.findByUsername(student.getUsername()).isEmpty()) {
            student.setPassword(passwordEncoder.encode(student.getPassword()));
            studentRepository.save(student);
        }else
            throw new UsernameNotFoundException("Student already exists");
    }

    public Student findByUsername(String username) {
        return studentRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Student not found"));
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }
}