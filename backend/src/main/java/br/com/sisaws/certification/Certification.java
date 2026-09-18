package br.com.sisaws.certification;

import jakarta.persistence.*;

@Entity
@Table(name = "certifications", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Certification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    protected Certification() {}

    public Certification(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
