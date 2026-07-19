package kr.co.seoulit.his.emergencyservice.channel.service;

import kr.co.seoulit.his.emergencyservice.channel.dto.*;

public interface ChannelService {
    ConsultRequestDto createConsultation(ConsultCreateRequestDto request);
    OncallRequestDto createOnCallPage(OncallCreateRequestDto request);
}
