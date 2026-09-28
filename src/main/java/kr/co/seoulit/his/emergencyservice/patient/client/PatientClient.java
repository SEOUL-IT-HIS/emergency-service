package kr.co.seoulit.his.emergencyservice.patient.client;

import kr.co.seoulit.his.emergencyservice.patient.dto.PatientApiResponse;
import kr.co.seoulit.his.emergencyservice.patient.dto.PatientBatchRequestDto;
import kr.co.seoulit.his.emergencyservice.patient.dto.PatientDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;


/**
 * patient-service 배치조회. 요청 1회에 patientId 1~100개(중복 포함 길이 기준).
 * 존재하지 않는 id는 응답에서 조용히 빠짐 — 호출 쪽에서 결과 개수가 요청보다
 * 적을 수 있다는 것을 감안해야 함.
 */
@Component
@RequiredArgsConstructor
public class PatientClient {

    private final RestTemplate restTemplate;

    @Value("${app.patient.base-url}")
    private String patientBaseUrl;

    public List<PatientDto> getPatients(List<String> patientIds){
        if(patientIds.isEmpty()){
            return List.of();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 바디 구성.  patientBatchRequestDto 안에  환자 ID 리스트를 담아서 보냄
        // HttpEntity는 헤더+바디를 함께 담는 객체
        HttpEntity<PatientBatchRequestDto> request =
                new HttpEntity<>(new PatientBatchRequestDto(patientIds), headers);

        // 외부 API 호출.
        ResponseEntity<PatientApiResponse<List<PatientDto>>> response = restTemplate.exchange(
                patientBaseUrl + "/api/patient/batch",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {
                });

        // 응답처리.   응답이 null일 수 있으므로 안전하게 처리.  body.getData()가 실제 환자리스트,  없으면 빈 리스트 반환
        PatientApiResponse<List<PatientDto>> body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }
}
