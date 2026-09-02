package kr.co.seoulit.his.emergencyservice.resource.service;

import kr.co.seoulit.his.emergencyservice.resource.dto.*;

import java.util.List;

public interface ResourceService {
    CongestionDto getCongestion();
    List<BedDto> getBeds(String zoneCode, String status);
    BedAssignmentDto assignBed(BedAssignmentCreateRequestDto request);
    EquipmentAllocationDto assignEquipment(EquipmentAssignmentCreateRequestDto request);
}
