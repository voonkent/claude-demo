package com.example.taskmanager.repository

import com.example.taskmanager.domain.Project
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectRepository : JpaRepository<Project, UUID>
