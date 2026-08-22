//import com.asjad.studygen.entity.User;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//import java.util.List;
//@Entity
//@Table(name = "roadmaps")
//@Getter
//@Setter
//@NoArgsConstructor
//public class Roadmap {
//
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @Column(nullable = false)
//    private String title;
//
//    @Column(name = "target_role")
//    private String targetRole;
//
//    @Column(name = "is_active", nullable = false)
//    private boolean active = true;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private OffsetDateTime createdAt = OffsetDateTime.now();
//
//    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
//    @OrderBy("sequenceOrder ASC")
//    private List<Module> modules = new ArrayList<>();
//
//    public void addModule(Module module) {
//        modules.add(module);
//        module.setRoadmap(this);
//    }
//}