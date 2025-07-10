package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "lkup_property_codelist_value")
public class LkupPropertyCodelistValue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private Integer propertyCodelistId;

    private String value;

}
