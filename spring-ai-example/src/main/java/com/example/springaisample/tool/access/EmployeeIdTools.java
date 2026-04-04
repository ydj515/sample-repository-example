package com.example.springaisample.tool.access;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class EmployeeIdTools {

    private final EmployeeIdService employeeIdService;

    public EmployeeIdTools() {
        this.employeeIdService = new EmployeeIdService();
    }

    // @ToolParam을 이용해 사진에서 분석한 사번 정보를 Argument로 입력
    @Tool(description = "직원의 사번과 모든 직원 사번과 비교한다.")
    boolean getCardList(@ToolParam(description = "사번") String idCardNumber) {
        return employeeIdService.checkEmployeeId(idCardNumber);
    }
}
