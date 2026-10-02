package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordDto;
import java.util.List;

public interface ConsentService {
    ConsentRecordDto createConsent(ConsentRecordCreateRequestDto request);
    List<ConsentRecordDto> getConsents(String receptionId);
}
