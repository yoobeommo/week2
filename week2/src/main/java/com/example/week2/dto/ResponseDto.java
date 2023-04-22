package com.example.week2.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResponseDto {          // 처리 상태값 반환을 위한 Dto
    private String msg;             // 성공 여부 (Sucess, Fail)
    private int statusCode;         // http 상태 코드

    public ResponseDto(String msg, int statusCode){
        this.msg = msg;                       // 성공 여부 (Sucess, Fail) 리턴 메세지
        this.statusCode = statusCode;         // http 상태 코드 반환
    }
}
