package com.driply.payments.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JSON 문자열을 ObjectNode로 변환하는 유틸리티 함수
     *
     * @param jsonString JSON 형식의 문자열
     * @return ObjectNode로 변환된 객체
     * @throws JsonProcessingException JSON 파싱 중 오류 발생 시 예외 처리
     */
    public static ObjectNode parseStringToObjectNode(String jsonString) throws JsonProcessingException {
        return (ObjectNode) objectMapper.readTree(jsonString);
    }
}
