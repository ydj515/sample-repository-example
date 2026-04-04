package com.example.springaisample.tool.customer;

import java.util.List;

import com.example.springaisample.model.CustomerProfile;
import org.springframework.stereotype.Service;

@Service
public class CustomerDirectoryService {

    // 데이터베이스 정보 조회
    public CustomerProfile getCustomer(String id) {
        return new CustomerProfile(id, "James", 30);
    }

    // 데이터베이스 정보 조회
    public List<CustomerProfile> getCustomers() {
        return List.of(
                new CustomerProfile("id01", "James1", 10),
                new CustomerProfile("id02", "James2", 20),
                new CustomerProfile("id03", "James3", 30)
        );
    }
}
