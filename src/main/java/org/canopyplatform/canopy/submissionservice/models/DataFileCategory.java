package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "lkup_data_file_category", schema = "public")
public class DataFileCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    @Column(name = "category_group")
    private String categoryGroup;

    public DataFileCategory(Integer id, String name, String categoryGroup) {
        this.id = id;
        this.name = name;
        this.categoryGroup = categoryGroup;
    }
}
