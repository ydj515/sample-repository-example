package com.example.springaisample.tool.customer;

import java.lang.reflect.Type;
import java.util.List;

import com.example.springaisample.model.CustomerProfile;
import org.springframework.ai.tool.execution.ToolCallResultConverter;

public class CustomerToolResultConverter implements ToolCallResultConverter {

    @Override
    public String convert(Object result, Type returnType) {
        if (result instanceof CustomerProfile customer) {
            return String.format("사용자 이름은 %s, 나이는 %s ", customer.name(), customer.age());
        }

        if (result instanceof List<?> customerList) {
            StringBuilder sb = new StringBuilder();

            for (Object customerObject : customerList) {
                if (customerObject instanceof CustomerProfile customer) {
                    sb.append(String.format("사용자 이름은 %s, 나이는 %s \n", customer.name(), customer.age()));
                }
            }

            return sb.toString();
        }

        return "";
    }
}
