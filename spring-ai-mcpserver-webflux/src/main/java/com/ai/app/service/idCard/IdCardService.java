package com.ai.app.service.idCard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class IdCardService {

    // 데이터베이스 또는 접근 권한 정보를 조회, 가상의 데이터 셋팅
    public List<String> getIdCardList(){
        return List.of("12345","35679","58473");
    }

    public boolean checkIdCardNumber(String idCardNumber){
        log.info("checkIdCardNumber:{}",idCardNumber);
        return this.getIdCardList().contains(idCardNumber);
    }
}
