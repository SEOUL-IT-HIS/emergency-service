package kr.co.seoulit.his.emergencyservice.care.mapper;

import kr.co.seoulit.his.emergencyservice.care.dto.*;
import kr.co.seoulit.his.emergencyservice.care.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CareMapstructMapper {
    ClinicalNoteDto toNoteDto(ClinicalNote entity);

    @Mapping(target = "treatmentTypeCode", source = "treatmentTypeCode")
    TreatmentRecordDto toTreatmentDto(TreatmentRecord entity);

    MarDto toMarDto(MedicationAdministration entity);
}
