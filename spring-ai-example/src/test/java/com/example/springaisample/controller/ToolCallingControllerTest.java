package com.example.springaisample.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springaisample.service.tool.AccessControlToolService;
import com.example.springaisample.service.tool.CustomerInquiryToolService;
import com.example.springaisample.service.tool.ShoppingRecommendationToolService;
import com.example.springaisample.service.tool.TimeWeatherToolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ToolCallingController.class)
class ToolCallingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeWeatherToolService timeWeatherToolService;

    @MockitoBean
    private CustomerInquiryToolService customerInquiryToolService;

    @MockitoBean
    private ShoppingRecommendationToolService shoppingRecommendationToolService;

    @MockitoBean
    private AccessControlToolService accessControlToolService;

    @Test
    void dateTimeEndpointReturnsServiceResponse() throws Exception {
        when(timeWeatherToolService.chat("지금 서울 시간 알려줘")).thenReturn("현재 시간입니다.");

        mockMvc.perform(get("/test/tools/date-time").param("prompt", "지금 서울 시간 알려줘"))
                .andExpect(status().isOk())
                .andExpect(content().string("현재 시간입니다."));

        verify(timeWeatherToolService).chat("지금 서울 시간 알려줘");
    }

    @Test
    void customerInquiryJsonEndpointReturnsServiceResponse() throws Exception {
        when(customerInquiryToolService.getCustomer("id01 고객 정보 알려줘")).thenReturn("{\"name\":\"James\"}");

        mockMvc.perform(get("/test/tools/customer-inquiry-json").param("prompt", "id01 고객 정보 알려줘"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"name\":\"James\"}"));

        verify(customerInquiryToolService).getCustomer("id01 고객 정보 알려줘");
    }

    @Test
    void customerInquiryStringEndpointReturnsServiceResponse() throws Exception {
        when(customerInquiryToolService.getCustomerAsString("id01 고객 정보 알려줘")).thenReturn("사용자 이름은 James, 나이는 30");

        mockMvc.perform(get("/test/tools/customer-inquiry-string").param("prompt", "id01 고객 정보 알려줘"))
                .andExpect(status().isOk())
                .andExpect(content().string("사용자 이름은 James, 나이는 30"));

        verify(customerInquiryToolService).getCustomerAsString("id01 고객 정보 알려줘");
    }

    @Test
    void recommendationEndpointReturnsServiceResponse() throws Exception {
        when(shoppingRecommendationToolService.getRecommendations("추천해줘", "id01")).thenReturn("1. 청반바지: 50000, 바지, 빨강");

        mockMvc.perform(get("/test/tools/recommendation")
                        .param("prompt", "추천해줘")
                        .param("user_id", "id01"))
                .andExpect(status().isOk())
                .andExpect(content().string("1. 청반바지: 50000, 바지, 빨강"));

        verify(shoppingRecommendationToolService).getRecommendations("추천해줘", "id01");
    }

    @Test
    void accessSystemEndpointReturnsServiceResponse() throws Exception {
        MockMultipartFile attach = new MockMultipartFile("attach", "badge.png", "image/png", "img".getBytes());
        when(accessControlToolService.analyzeAccessImage(any(), any())).thenReturn("출입문이 열립니다.");

        mockMvc.perform(multipart("/test/tools/access-system").file(attach))
                .andExpect(status().isOk())
                .andExpect(content().string("출입문이 열립니다."));

        verify(accessControlToolService).analyzeAccessImage(any(), any());
    }

    @Test
    void accessSystemEndpointRejectsMissingImage() throws Exception {
        mockMvc.perform(multipart("/test/tools/access-system"))
                .andExpect(status().isOk())
                .andExpect(content().string("이미지를 올려주세요."));

        verifyNoInteractions(accessControlToolService);
    }
}
