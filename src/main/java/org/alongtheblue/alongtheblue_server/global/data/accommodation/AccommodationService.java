package org.alongtheblue.alongtheblue_server.global.data.accommodation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class AccommodationService {
    private final WebClient webClient;

    @Autowired
    private final AccommodationRepository accommodationRepository;
    @Autowired
    private final AccommodationImageRepository accommodationImageRepository;

    @Autowired
    public AccommodationService(WebClient.Builder webClientBuilder, AccommodationRepository accommodationRepository, AccommodationImageRepository accommodationImageRepository) {
        this.webClient = webClientBuilder.baseUrl("https://api.visitjeju.net").build();
        this.accommodationRepository = accommodationRepository;
        this.accommodationImageRepository = accommodationImageRepository;
    }

    public void fetchHotelData() {
        Mono<String> response = webClient.get()
                .uri("/vsjApi/contents/searchList?apiKey=ca0cc07e23f9458d8434fd02261340ad&locale=kr&category=c3")
                .retrieve()
                .bodyToMono(String.class);

        response.subscribe(this::parseAndSaveHotelData);
    }

    private void parseAndSaveHotelData(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    String contentsid = itemNode.path("contentsid").asText();
                    String title = itemNode.path("title").asText();
                    String roadaddress = itemNode.path("roadaddress").asText();
                    String introduction = itemNode.path("introduction").asText();

                    saveHotelData(contentsid, title, roadaddress, introduction);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveHotelData(String contentsid, String title, String roadaddress, String introduction) {
        Accommodation accommodation = new Accommodation();
        accommodation.setContentsid(contentsid);
        accommodation.setTitle(title);
        accommodation.setRoadaddress(roadaddress);
        accommodation.setIntroduction(introduction);

        // 데이터를 데이터베이스에 저장
        accommodationRepository.save(accommodation);
    }

    public void saveAccommodations() {
        String url = "https://api.visitjeju.net/vsjApi/contents/searchList?apiKey=ca0cc07e23f9458d8434fd02261340ad&locale=kr&category=c3";
        RestTemplate restTemplate = new RestTemplate();
        String jsonResponse = restTemplate.getForObject(url, String.class);

        try {
            // JSON 데이터를 파싱하여 필요한 필드 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.path("items");

            List<Accommodation> accommodations = new ArrayList<>();

            for (JsonNode itemNode : itemsNode) {
                String contentsid = itemNode.path("contentsid").asText();
                String title = itemNode.path("title").asText();
                String roadaddress = itemNode.path("roadaddress").asText();
                String introduction = itemNode.path("introduction").asText();

                Accommodation accommodation = new Accommodation();
                accommodation.setContentsid(contentsid);
                accommodation.setTitle(title);
                accommodation.setRoadaddress(roadaddress);
                accommodation.setIntroduction(introduction);

                accommodations.add(accommodation);

                // 100개의 데이터만 저장
                if (accommodations.size() >= 100) {
                    break;
                }
            }

            // 데이터베이스에 저장
            accommodationRepository.saveAll(accommodations);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateOverview() {
        String url = "https://apis.data.go.kr/B551011/KorService1/areaBasedList1?serviceKey=GY8BQwWZJD6QX3tfaQTpfYMRjcRnaHoPAxn/7u6ZffwScPHeO3TYZgA0zMPfnO/iSc/PunU/5rZYIa5jj98sUw==&numOfRows=100&pageNo=1&MobileOS=ETC&MobileApp=AppTest&_type=json&listYN=Y&arrange=A&contentTypeId=32&areaCode=39";

        WebClient webClient = WebClient.builder().build();

        try {
            System.out.println("Requesting URL: " + url);

            // API로부터 JSON 응답 가져오기
            String jsonResponse = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println("Error Response: " + errorBody);
                                    return Mono.error(new RuntimeException("API 요청 실패"));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();

            // JSON 응답 형식 확인
            if (!jsonResponse.startsWith("{") && !jsonResponse.startsWith("[")) {
                throw new RuntimeException("예상치 못한 응답 형식: " + jsonResponse);
            }

            // JSON 데이터를 파싱하여 필요한 필드 추출
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            if (itemsNode.isMissingNode()) {
                throw new RuntimeException("items 노드가 없습니다.");
            }

            List<Accommodation> accommodations = new ArrayList<>();

            for (JsonNode itemNode : itemsNode) {
                String contentsid = itemNode.path("contentid").asText();
                String addr1 = itemNode.path("addr1").asText();
                String title = itemNode.path("title").asText();

                Accommodation accommodation = new Accommodation();
                accommodation.setContentsid(contentsid);
                accommodation.setRoadaddress(addr1);
                accommodation.setTitle(title);

                accommodations.add(accommodation);

                // 100개의 데이터만 저장 (필요에 따라 조정 가능)
                if (accommodations.size() >= 100) {
                    break;
                }
            }

            // 데이터베이스에 저장
            accommodationRepository.saveAll(accommodations);

        } catch (JsonProcessingException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void updateAccommodationInfo() {
        String url = "https://apis.data.go.kr/B551011/KorService1/areaBasedList1?serviceKey=GY8BQwWZJD6QX3tfaQTpfYMRjcRnaHoPAxn/7u6ZffwScPHeO3TYZgA0zMPfnO/iSc/PunU/5rZYIa5jj98sUw==&numOfRows=100&pageNo=1&MobileOS=ETC&MobileApp=AppTest&_type=json&listYN=Y&arrange=A&contentTypeId=32&areaCode=39";

        webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(this::processAccommodationData); // 데이터 처리 메서드 호출

    }

    // JSON 데이터 처리 로직
    private void processAccommodationData(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            List<Accommodation> accommodations = new ArrayList<>();

            for (JsonNode itemNode : itemsNode) {
                String contentsid = itemNode.path("contentid").asText();
                String addr1 = itemNode.path("addr1").asText();
                String title = itemNode.path("title").asText();

                Accommodation accommodation = new Accommodation();
                accommodation.setContentsid(contentsid);
                accommodation.setRoadaddress(addr1);
                accommodation.setTitle(title);

                // 추가된 필드 설정


                accommodations.add(accommodation);

                if (accommodations.size() >= 100) {
                    break;
                }
            }

            // 데이터베이스에 저장
            accommodationRepository.saveAll(accommodations);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<AccommodationDTO> getAccommodationHomeInfo() {
        List<Accommodation> accommodations = accommodationRepository.findAll();
        AccommodationDTO dto = new AccommodationDTO();
        List<AccommodationDTO> dtos = new ArrayList<>();
        for (Accommodation accommodation : accommodations) {
            dto.setRoadaddress(accommodation.getRoadaddress().substring(8));
            dto.setContentsid(accommodation.getContentsid());
            dto.setTitle(accommodation.getTitle());
            dtos.add(dto);
        }
        return dtos;
    }


    // API 응답을 매핑하기 위한 클래스
    public static class ApiResponse {
        private String introduction;

        // Getters and setters
        public String getIntroduction() {
            return introduction;
        }

        public void setIntroduction(String introduction) {
            this.introduction = introduction;
        }
    }

    public void updateAccommodationsWithNullIntroduction() {
        // contentsid 값들만 가져옴
        List<String> contentsIds = accommodationRepository.findAllContentsIds();
        WebClient webClient = WebClient.create();

        for (String contentsid : contentsIds) {
            try {
                ApiResponse response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("apis.data.go.kr")
                                .path("/B551011/KorService1/areaBasedList1")
                                .queryParam("serviceKey", "GY8BQwWZJD6QX3tfaQTpfYMRjcRnaHoPAxn/7u6ZffwScPHeO3TYZgA0zMPfnO/iSc/PunU/5rZYIa5jj98sUw==")
                                .queryParam("numOfRows", 255)
                                .queryParam("pageNo", 1)
                                .queryParam("MobileOS", "ETC")
                                .queryParam("MobileApp", "AppTest")
                                .queryParam("_type", "json")
                                .queryParam("listYN", "Y")
                                .queryParam("arrange", "A")
                                .queryParam("contentTypeId", 32)
                                .queryParam("areaCode", 39)
                                .queryParam("contentsid", contentsid)  // contentsid 추가
                                .build())
                        .retrieve()
                        .bodyToMono(ApiResponse.class)
                        .block();  // 동기 호출

                // API 응답으로 introduction 필드가 있는 경우 업데이트
                if (response != null && response.getIntroduction() != null) {
                    // 데이터베이스에서 해당 contentsid를 가진 Accommodation 객체를 찾아 업데이트
                    Accommodation accommodation = accommodationRepository.findByContentsid(contentsid);
                    accommodation.setIntroduction(response.getIntroduction());
                    accommodationRepository.save(accommodation);  // 데이터베이스에 저장
                }
            } catch (Exception e) {
                e.printStackTrace();  // 예외 발생 시 출력
            }
        }
    }

    public void updateAllOverviews() {
        // DB에서 모든 Accommodation 객체 가져오기
        List<Accommodation> accommodations = accommodationRepository.findAll();

        // 각 contentsid에 대해 overview 업데이트
        accommodations.forEach(accommodation -> {
            String contentId = accommodation.getContentsid();
            updateOverviewonemoretime(contentId);
        });
    }

    public void updateFirstOverviews() {
        // DB에서 모든 Accommodation 객체 가져오기
        List<Accommodation> accommodations = accommodationRepository.findAll();

        // 각 contentsid에 대해 overview 업데이트
        String contentId = accommodations.get(0).getContentsid();
        updateOverviewonemoretime(contentId);
    }

    public void updateOverviewonemoretime(String contentId) {
        String url = "https://apis.data.go.kr/B551011/KorService1/detailCommon1?serviceKey=GY8BQwWZJD6QX3tfaQTpfYMRjcRnaHoPAxn/7u6ZffwScPHeO3TYZgA0zMPfnO/iSc/PunU/5rZYIa5jj98sUw==&MobileOS=ETC&MobileApp=AppTest&_type=json&contentId="+contentId+"&contentTypeId=32&defaultYN=Y&firstImageYN=Y&areacodeYN=Y&catcodeYN=Y&addrinfoYN=Y&mapinfoYN=Y&overviewYN=Y&numOfRows=10&pageNo=1";

        WebClient webClient = WebClient.builder().build();

        try {
            System.out.println("Requesting URL: " + url);

            // API로부터 JSON 응답 가져오기
            String jsonResponse = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println("Error Response: " + errorBody);
                                    return Mono.error(new RuntimeException("API 요청 실패"));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();
            // JSON 응답 형식 확인
            if (!jsonResponse.startsWith("{") && !jsonResponse.startsWith("[")) {
                throw new RuntimeException("예상치 못한 응답 형식: " + jsonResponse);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode overviewNode = rootNode
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item")
                    .get(0)  // 배열의 첫 번째 객체
                    .path("overview");
            String overview = overviewNode.asText();
            System.out.println("overview: "+ overview);
            // 해당 accommodation 객체를 업데이트
            Accommodation accommodation = accommodationRepository.findByContentsid(contentId);
            if (accommodation != null) {
                accommodation.setIntroduction(overview); // introduction 필드 업데이트
                accommodationRepository.save(accommodation); // 저장
            } else {
                System.out.println("Accommodation not found for contentId: " + contentId);
            }

        } catch (JsonProcessingException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateCheckInTimes() {
        // DB에서 모든 Accommodation 객체 가져오기
        List<Accommodation> accommodations = accommodationRepository.findAll();

        // 각 contentsid에 대해 checkintime 업데이트
        for (Accommodation accommodation : accommodations) {
            String contentId = accommodation.getContentsid();
//            updateCheckInTime(contentId);  // checkintime 업데이트 메서드 호출
            updateAccommodationDetails(contentId);
        }
    }

    public void updateCheckInTime(String contentId) {
        String url = "https://apis.data.go.kr/B551011/KorService1/detailIntro1?serviceKey=GY8BQwWZJD6QX3tfaQTpfYMRjcRnaHoPAxn/7u6ZffwScPHeO3TYZgA0zMPfnO/iSc/PunU/5rZYIa5jj98sUw==&MobileOS=ETC&MobileApp=AppTest&_type=json&contentId="+contentId+"&contentTypeId=32&numOfRows=100&pageNo=1";

        WebClient webClient = WebClient.builder().build();

        try {
            System.out.println("Requesting URL: " + url);

            // API로부터 JSON 응답 가져오기
            String jsonResponse = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println("Error Response: " + errorBody);
                                    return Mono.error(new RuntimeException("API 요청 실패"));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();

            // JSON 응답 형식 확인
            if (!jsonResponse.startsWith("{") && !jsonResponse.startsWith("[")) {
                throw new RuntimeException("예상치 못한 응답 형식: " + jsonResponse);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode checkinTimeNode = rootNode
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item")
                    .get(0)  // 배열의 첫 번째 객체
                    .path("checkintime");
            String checkintime = checkinTimeNode.asText();
            System.out.println("Check-in Time: " + checkintime);

            // 해당 accommodation 객체를 업데이트
            Accommodation accommodation = accommodationRepository.findByContentsid(contentId);
            if (accommodation != null) {
                accommodation.setCheckintime(checkintime); // checkintime 필드 업데이트
                accommodationRepository.save(accommodation); // 저장
            } else {
                System.out.println("Accommodation not found for contentId: " + contentId);
            }

        } catch (JsonProcessingException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateInfoCenterLodgings() {
        // DB에서 모든 Accommodation 객체 가져오기
        List<Accommodation> accommodations = accommodationRepository.findAll();

        // 각 contentsid에 대해 infocenterlodging 업데이트
        for (Accommodation accommodation : accommodations) {
            String contentId = accommodation.getContentsid();
            updateInfoCenterLodging(contentId);  // infocenterlodging 업데이트 메서드 호출
        }
    }

    public void updateInfoCenterLodging(String contentId) {
        String url = "https://apis.data.go.kr/B551011/KorService1/detailIntro1?serviceKey=GY8BQwWZJD6QX3tfaQTpfYMRjcRnaHoPAxn/7u6ZffwScPHeO3TYZgA0zMPfnO/iSc/PunU/5rZYIa5jj98sUw==&MobileOS=ETC&MobileApp=AppTest&_type=json&contentId="+contentId+"&contentTypeId=32&numOfRows=100&pageNo=1";

        WebClient webClient = WebClient.builder().build();

        try {
            System.out.println("Requesting URL: " + url);

            // API로부터 JSON 응답 가져오기
            String jsonResponse = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println("Error Response: " + errorBody);
                                    return Mono.error(new RuntimeException("API 요청 실패"));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();

            // JSON 응답 형식 확인
            if (!jsonResponse.startsWith("{") && !jsonResponse.startsWith("[")) {
                throw new RuntimeException("예상치 못한 응답 형식: " + jsonResponse);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode infocenterLodgingNode = rootNode
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item")
                    .get(0)  // 배열의 첫 번째 객체
                    .path("infocenterlodging");
            String infocenterlodging = infocenterLodgingNode.asText();
            System.out.println("Info Center Lodging: " + infocenterlodging);

            // 해당 accommodation 객체를 업데이트
            Accommodation accommodation = accommodationRepository.findByContentsid(contentId);
            if (accommodation != null) {
                accommodation.setInfocenter(infocenterlodging); // infocenterlodging 필드 업데이트
                accommodationRepository.save(accommodation); // 저장
            } else {
                System.out.println("Accommodation not found for contentId: " + contentId);
            }

        } catch (JsonProcessingException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAllOriginImageUrls() {
        // DB에서 모든 Accommodation 객체 가져오기
        List<Accommodation> accommodations = accommodationRepository.findAll();

        // 각 contentsid에 대해 originimgurls 업데이트
        for (Accommodation accommodation : accommodations) {
            String contentId = accommodation.getContentsid();
            updateOriginImageUrls(contentId);  // originimgurls 업데이트 메서드 호출
        }
//        updateOriginImageUrls(accommodations.get(0).getContentsid());
    }

    public Accommodation findByContentId(String contentId){
        return accommodationRepository.findByContentsid(contentId);
    }

    public void updateOriginImageUrls(String contentId) {
        String url = "https://apis.data.go.kr/B551011/KorService1/detailImage1?serviceKey=GY8BQwWZJD6QX3tfaQTpfYMRjcRnaHoPAxn/7u6ZffwScPHeO3TYZgA0zMPfnO/iSc/PunU/5rZYIa5jj98sUw==&MobileOS=ETC&MobileApp=AppTest&_type=json&contentId="+contentId+"&imageYN=Y&subImageYN=Y&numOfRows=10&pageNo=1";

        WebClient webClient = WebClient.builder().build();

        try {
            System.out.println("Requesting URL: " + url);

            // API로부터 JSON 응답 가져오기
            String jsonResponse = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println("Error Response: " + errorBody);
                                    return Mono.error(new RuntimeException("API 요청 실패"));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();

            // JSON 응답 형식 확인
            if (!jsonResponse.startsWith("{") && !jsonResponse.startsWith("[")) {
                throw new RuntimeException("예상치 못한 응답 형식: " + jsonResponse);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item");

            // 여러 개의 originimgurl을 가져옴
            List<String> originImgUrls = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                String originimgurl = itemNode.path("originimgurl").asText();
                if (!originimgurl.isEmpty()) {
                    originImgUrls.add(originimgurl);
                    System.out.println(originimgurl);
                    AccommodationImage accommodationImage = new AccommodationImage();
                    accommodationImage.setUrl(originimgurl);
                    accommodationImage.setAccommodation(findByContentId(contentId));
                    accommodationImageRepository.save(accommodationImage);
                }
            }


        } catch (JsonProcessingException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AccommodationDTO getAccommodationDetails(String contentsid) {
        // DB에서 Accommodation 조회
        Accommodation accommodation = accommodationRepository.findByContentsid(contentsid);
        if (accommodation != null) {
            // AccommodationDTO에 데이터를 매핑
            AccommodationDTO accommodationDTO = new AccommodationDTO();
            accommodationDTO.setContentsid(accommodation.getContentsid());
            accommodationDTO.setTitle(accommodation.getTitle());
            accommodationDTO.setRoadaddress(accommodation.getRoadaddress());
            accommodationDTO.setIntroduction(accommodation.getIntroduction());
            accommodationDTO.setCheckintime(accommodation.getCheckintime());
            accommodationDTO.setInfocenter(accommodation.getInfocenter());

            // 이미지 리스트를 DTO에 추가
            List<String> imageUrls = accommodation.getAccommodationImage().stream()
                    .map(AccommodationImage::getUrl)
                    .collect(Collectors.toList());
            accommodationDTO.setOriginimgurl(imageUrls);

            return accommodationDTO;
        } else {
            // 없을 경우 null 리턴 또는 예외 처리
            return null;
        }
    }

    public List<AccommodationDTO> getRandomAccommodationDetailsWithImages() {
        // DB에서 6개의 랜덤 이미지를 가진 Accommodation 조회
        List<Accommodation> accommodations = accommodationRepository.findRandomAccommodationsWithImages();

        // AccommodationDTO 리스트에 데이터를 매핑
        List<AccommodationDTO> accommodationDTOList = accommodations.stream().map(accommodation -> {
            AccommodationDTO accommodationDTO = new AccommodationDTO();
            accommodationDTO.setContentsid(accommodation.getContentsid());
            accommodationDTO.setTitle(accommodation.getTitle());
            accommodationDTO.setRoadaddress(accommodation.getRoadaddress());
            accommodationDTO.setIntroduction(accommodation.getIntroduction());
            accommodationDTO.setCheckintime(accommodation.getCheckintime());

            // 이미지 리스트를 DTO에 추가
            List<String> imageUrls = accommodation.getAccommodationImage().stream()
                    .map(AccommodationImage::getUrl)
                    .collect(Collectors.toList());
            accommodationDTO.setOriginimgurl(imageUrls);

            return accommodationDTO;
        }).collect(Collectors.toList());

        return accommodationDTOList;
    }

    public List<AccommodationDTO> getRandomAccommodationDetailsWithTwoImages() {
        // DB에서 6개의 랜덤 Accommodation 조회
        List<Accommodation> accommodations = accommodationRepository.findRandomAccommodationsWithImages();

        // AccommodationDTO 리스트에 데이터를 매핑
        List<AccommodationDTO> accommodationDTOList = accommodations.stream().map(accommodation -> {
            AccommodationDTO accommodationDTO = new AccommodationDTO();
            accommodationDTO.setContentsid(accommodation.getContentsid());
            accommodationDTO.setTitle(accommodation.getTitle());
            accommodationDTO.setRoadaddress(accommodation.getRoadaddress());
            accommodationDTO.setIntroduction(accommodation.getIntroduction());

            // 이미지 리스트에서 랜덤으로 두 개의 이미지 URL 가져오기
            List<String> imageUrls = accommodation.getAccommodationImage().stream()
                    .map(AccommodationImage::getUrl)
                    .distinct() // 중복된 이미지 URL 제거
                    .limit(2)   // 최대 2개만 선택
                    .collect(Collectors.toList());
            accommodationDTO.setOriginimgurl(imageUrls);

            return accommodationDTO;
        }).collect(Collectors.toList());

        return accommodationDTOList;
    }

    public void updateAccommodationDetails(String contentId) {
        String url = "https://apis.data.go.kr/B551011/KorService1/detailIntro1?serviceKey=GY8BQwWZJD6QX3tfaQTpfYMRjcRnaHoPAxn/7u6ZffwScPHeO3TYZgA0zMPfnO/iSc/PunU/5rZYIa5jj98sUw==&MobileOS=ETC&MobileApp=AppTest&_type=json&contentId="+contentId+"&contentTypeId=32&numOfRows=100&pageNo=1";

        WebClient webClient = WebClient.builder().build();

        try {
            System.out.println("Requesting URL: " + url);

            // API로부터 JSON 응답 가져오기
            String jsonResponse = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.out.println("Error Response: " + errorBody);
                                    return Mono.error(new RuntimeException("API 요청 실패"));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();

            // JSON 응답 형식 확인
            if (!jsonResponse.startsWith("{") && !jsonResponse.startsWith("[")) {
                throw new RuntimeException("예상치 못한 응답 형식: " + jsonResponse);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // checkintime과 infocenterlodging을 모두 추출
            JsonNode checkinTimeNode = rootNode
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item")
                    .get(0)
                    .path("checkintime");
            String checkintime = checkinTimeNode.asText();

            JsonNode infocenterLodgingNode = rootNode
                    .path("response")
                    .path("body")
                    .path("items")
                    .path("item")
                    .get(0)
                    .path("infocenterlodging");
            String infocenterlodging = infocenterLodgingNode.asText();

            System.out.println("Check-in Time: " + checkintime);
            System.out.println("Info Center Lodging: " + infocenterlodging);

            // 해당 accommodation 객체를 업데이트
            Accommodation accommodation = accommodationRepository.findByContentsid(contentId);
            if (accommodation != null) {
                accommodation.setCheckintime(checkintime); // checkintime 필드 업데이트
                accommodation.setInfocenter(infocenterlodging); // infocenterlodging 필드 업데이트
                accommodationRepository.save(accommodation); // 저장
            } else {
                System.out.println("Accommodation not found for contentId: " + contentId);
            }

        } catch (JsonProcessingException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}