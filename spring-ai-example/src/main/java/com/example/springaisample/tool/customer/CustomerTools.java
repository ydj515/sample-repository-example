package com.example.springaisample.tool.customer;

import java.util.List;

import com.example.springaisample.model.CustomerProfile;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class CustomerTools {

    private final CustomerDirectoryService customerDirectoryService;

    public CustomerTools() {
        this.customerDirectoryService = new CustomerDirectoryService();
    }

    // returnDirect = true는 다시 LLM에 전송 없이 결과를 바로 전송
    @Tool(description = "특정 ID의 사용자의 정보를 조회. ", returnDirect = true)
    CustomerProfile getCustomer(@ToolParam(description = "사용자의 ID", required = true) String id) {
        return customerDirectoryService.getCustomer(id);
    }

    @Tool(description = "모든 또는 전체 사용자 정보를 조회. ", returnDirect = true)
    List<CustomerProfile> getAllCustomer() {
        return customerDirectoryService.getCustomers();
    }
}
