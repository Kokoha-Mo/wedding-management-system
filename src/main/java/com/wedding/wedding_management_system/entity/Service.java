package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "services")
@Data
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private String name;
    private Integer price;
    private String content;
    private Integer estimated_days;
    private LocalDate ceremony_date;

    @JsonIgnore
    @OneToMany(mappedBy = "service")
    private List<BookDetail> bookDetails;

    @JsonIgnore
    @OneToMany(mappedBy = "service")
    private List<ProjectTask> projectTasks;
}
