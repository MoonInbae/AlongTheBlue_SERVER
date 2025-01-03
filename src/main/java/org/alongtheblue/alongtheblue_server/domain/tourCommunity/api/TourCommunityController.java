package org.alongtheblue.alongtheblue_server.domain.tourCommunity.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.alongtheblue.alongtheblue_server.domain.tourCommunity.domain.UserTourCourse;
import org.alongtheblue.alongtheblue_server.domain.tourCommunity.dto.UserTourCourseDTO;
import org.alongtheblue.alongtheblue_server.domain.tourCommunity.dto.request.CreateUserTourCourseRequestDto;
import org.alongtheblue.alongtheblue_server.domain.tourCommunity.dto.response.UserTourCourseDetailDto;
import org.alongtheblue.alongtheblue_server.global.common.response.ApiResponse;
import org.alongtheblue.alongtheblue_server.domain.tourCommunity.application.TourCommunityService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "여행따라 API", description = "여행따라  코스 등록 / 게시물 목록 조회 / 게시물 상세 조회")
@RestController
@RequestMapping("/api/tourpost")
@RequiredArgsConstructor
public class TourCommunityController {

    private final TourCommunityService tourCommunityService;

    @Operation(summary = "여행따라 게시물 등록 API")
    @PostMapping
    public UserTourCourse createTourCourse(
            @RequestHeader("Authorization") String uid,
            @RequestPart(value = "request", required = false) CreateUserTourCourseRequestDto dto,
            @RequestPart(value = "file", required = false) List<MultipartFile> images
//            @RequestPart("title") String title,
//            @RequestPart("writing") String writing,
//            @RequestPart("createdate") Date date,
//            @RequestPart List<TourPostItem> tourItems,
//            @RequestPart List<TourPostHashTag> hashTags,
//            ,
//            @RequestPart List<List<Integer>> imgIndexArr
    ) {
        return tourCommunityService.createPost(uid, dto.toServiceRequest(), images);
//        UserTourCourse userTourCourse= new UserTourCourse();
//        userTourCourse.setTitle(title);
//        userTourCourse.setTourPostItems(tourItems);
//        userTourCourse.setTourPostHashTags(hashTags);
//        userTourCourse.setCreatedate(date);
//        userTourCourse.setWriting(writing);

//        return tourCommunityService.createPost(userTourCourse, images, dto);
    }

    @Operation(summary = "여행따라 전체 게시물 조회 API")
    @GetMapping("/list")
    public ApiResponse<List<UserTourCourseDTO>> getAllUserTourCourses(){
        return tourCommunityService.getAllUserTourCourses();
    }

    @Operation(summary = "id로 여행따라 게시물(여행코스 포함) 상세 조회")
    @GetMapping("/{id}")
    public ApiResponse<UserTourCourseDetailDto> getUserCourseById(@PathVariable Long id){
        return tourCommunityService.getUserCourseByID(id);
    }
    @Operation(summary = "내 여행따라 전체 게시물 조회 API")
    @GetMapping("/my")
    public ApiResponse<List<UserTourCourseDTO>> retrieveMyUserTourCourses(@RequestHeader("Authorization") String uid) {
        return tourCommunityService.retrieveMyUserTourCourses(uid);
    }

}
