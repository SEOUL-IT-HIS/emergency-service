package kr.co.seoulit.his.emergencyservice.referencedoc.repository;

import kr.co.seoulit.his.emergencyservice.referencedoc.entity.ReferenceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferenceDocumentRepository extends JpaRepository<ReferenceDocument, String> {

    /** 삭제되지 않은 문서(deletedYn = "N"), 올린 시각 최신순 */
    List<ReferenceDocument> findByDeletedYnOrderByUploadedAtDesc(String deletedYn);

    Optional<ReferenceDocument> findByIdAndDeletedYn(String id, String deletedYn);
}
