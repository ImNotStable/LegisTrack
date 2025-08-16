package com.legistrack.persistence.repository

import com.legistrack.persistence.entity.Sponsor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SponsorRepository : JpaRepository<Sponsor, Long> {
    fun findByBioguideId(bioguideId: String): Sponsor?

    fun existsByBioguideId(bioguideId: String): Boolean
}
