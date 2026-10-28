package com.utn.tp_spring_grupal.alicuotaiva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlicuotaIvaRepository extends JpaRepository<AlicuotaIva, Long> {
}
