package com.aiminion.aiservice.common.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T extends MasterEntity> extends JpaRepository<T, Long> {}

