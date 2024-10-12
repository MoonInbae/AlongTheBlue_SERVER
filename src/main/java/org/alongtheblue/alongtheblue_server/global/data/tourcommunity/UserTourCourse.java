package org.alongtheblue.alongtheblue_server.global.data.tourcommunity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.alongtheblue.alongtheblue_server.domain.userInfo.domain.UserInfo;

import java.util.Date;
import java.util.List;

// AlongCourse
@Entity
@Data
public class UserTourCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date createdate;
    private String title;
    private String writing; // content로 변경

    @OneToMany(mappedBy = "userTourCourse", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<TourPostItem> tourPostItems;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    @JsonBackReference
    private UserInfo userInfo;

//    @OneToMany(mappedBy = "tourCourseForHashTag", cascade = CascadeType.ALL)
//    @JsonManagedReference
//    private List<TourPostHashTag> tourPostHashTags;

    private String contentId; // 필요 없음
}
