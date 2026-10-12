package kr.co.seoulit.his.emergencyservice.patient.dto;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 환자서비스 배치조회 결과를 patientId → 이름 맵으로 만든다.
 *
 * 임시환자·신원미상 환자는 이름이 비어 있다. 예전에는 Collectors.toMap 으로 만들었는데, toMap 은 값이 null 이면
 * NullPointerException 이라 이름 없는 환자 한 명이 섞이면 목록 전체의 이름이 사라졌다
 * (호출부가 예외를 잡아 "환자명 없이" 내려주기 때문에 오류로도 보이지 않았다).
 * 이름이 없는 환자는 맵에 넣지 않는다(화면에는 "-"로 보인다).
 */
public final class PatientNames {

    private PatientNames() {
    }

    public static Map<String, String> byId(List<PatientDto> patients) {
        Map<String, String> names = new HashMap<>();
        if (patients == null) {
            return names;
        }
        for (PatientDto patient : patients) {
            if (patient == null || !StringUtils.hasText(patient.getPatientId()) || !StringUtils.hasText(patient.getPatientName())) {
                continue;
            }
            names.putIfAbsent(patient.getPatientId(), patient.getPatientName());
        }
        return names;
    }
}
