package kr.co.seoulit.his.emergencyservice.resource.service;

import kr.co.seoulit.his.emergencyservice.resource.dto.*;

public interface ResourceService {
    CongestionDto getCongestion();
    BedAssignmentDto assignBed(BedAssignmentCreateRequestDto request);
    EquipmentAllocationDto assignEquipment(EquipmentAssignmentCreateRequestDto request);
}
