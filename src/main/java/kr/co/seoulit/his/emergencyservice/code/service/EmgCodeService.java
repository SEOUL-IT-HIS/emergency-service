package kr.co.seoulit.his.emergencyservice.code.service;

import kr.co.seoulit.his.emergencyservice.code.dto.*;
import java.util.List;

public interface EmgCodeService {
    List<EmgCodeGroupDto> getCodes();
    List<EmgCodeDto> getCodesByGroup(String groupCode);
    EmgCodeDto createCode(EmgCodeCreateRequestDto request);
    EmgCodeDto updateCode(Long codeId, EmgCodeUpdateRequestDto request);
    EmgCodeDto updateUseYn(Long codeId, EmgCodeUseYnRequestDto request);
}
