package com.example.springaisample.tool.access;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class EmployeeIdService {

    // 데이터베이스 또는 접근 권한 정보를 조회, 가상의 데이터 셋팅
    public List<String> getEmployeeIdList() {
        return List.of("12345", "35679", "58473");
    }

    boolean checkEmployeeId(String employeeId) {
        return getEmployeeIdList().contains(employeeId);
    }
}
