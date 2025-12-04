package com.usedcar.trading.domain.vehicle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usedcar.trading.domain.company.entity.Company;
import com.usedcar.trading.domain.employee.entity.Employee;
import com.usedcar.trading.domain.employee.repository.EmployeeRepository;
import com.usedcar.trading.domain.user.entity.User;
import com.usedcar.trading.domain.vehicle.dto.VehicleRegisterRequest;
import com.usedcar.trading.domain.vehicle.entity.Vehicle;
import com.usedcar.trading.domain.vehicle.entity.VehicleImage;
import com.usedcar.trading.domain.vehicle.entity.VehicleStatus;
import com.usedcar.trading.domain.vehicle.repository.VehicleImageRepository;
import com.usedcar.trading.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleImageRepository vehicleImageRepository;
    private final EmployeeRepository employeeRepository;

    // 로컬 파일 저장 경로
    private final String uploadDir = System.getProperty("user.dir") + "/uploads/";

    // 매물 등록
    public void register(User user, VehicleRegisterRequest request, List<MultipartFile> imageFiles) throws IOException {

        // 1. 등록자(Employee)와 소속 업체(Company) 조회
        Employee employee = employeeRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("딜러(직원) 정보를 찾을 수 없습니다."));
        Company company = employee.getCompany();

        // 2. 옵션 리스트 -> JSON 문자열 변환
        String optionsJson = convertOptionsToJson(request.getOptions());

        // 3. Vehicle 엔티티 생성
        Vehicle vehicle = Vehicle.builder()
                .company(company)
                .registeredBy(employee)
                .brand(request.getBrand())
                .model(request.getModel())
                .modelYear(request.getModelYear())
                .mileage(request.getMileage())
                .fuelType(request.getFuelType())
                .transmission(request.getTransmission())
                .price(request.getPrice())
                .color(request.getColor())
                .accidentHistory(request.getAccidentHistory())
                .description(request.getDescription())
                .options(optionsJson)
                .vehicleStatus(VehicleStatus.PENDING)
                .build();

        vehicleRepository.save(vehicle);

        // 4. 이미지 파일 저장
        if (imageFiles != null && !imageFiles.isEmpty()) {
            saveImages(vehicle, imageFiles);
        }
    }

    // 조회수 증가
    @Transactional
    public void increaseViewCount(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물 없음"));

        vehicle.increaseViewCount();
    }

    // 매물 갱신
    @Transactional
    public void renewVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물 없음"));

        if (vehicle.getVehicleStatus() != VehicleStatus.EXPIRED) {
            throw new IllegalStateException("갱신 가능한 상태가 아닙니다.");
        }

        vehicle.extendExpirationDate();

        vehicle.changeStatus(VehicleStatus.PENDING);
    }

    // 이미지 저장 로직
    private void saveImages(Vehicle vehicle, List<MultipartFile> files) throws IOException {
        File folder = new File(uploadDir);
        if (!folder.exists()) folder.mkdirs();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) continue;

            // 파일명 중복 방지 (UUID 적용)
            String originalFilename = file.getOriginalFilename();
            String saveFilename = UUID.randomUUID() + "_" + originalFilename;
            String filePath = uploadDir + saveFilename;

            // 실제 파일 저장
            file.transferTo(new File(filePath));

            // DB에 저장할 웹 접근 경로 (/uploads/파일명)
            String webAccessUrl = "/uploads/" + saveFilename;

            VehicleImage vehicleImage = VehicleImage.builder()
                    .vehicle(vehicle)
                    .imageUrl(webAccessUrl)
                    .displayOrder(i)
                    .build();

            vehicleImageRepository.save(vehicleImage);

            // 첫 번째 사진을 대표 이미지(Thumbnail)로 설정
            if (i == 0) {
                vehicle.setThumbnailUrl(webAccessUrl);
            }
        }
    }

    // 옵션 리스트(List<String>)를 JSON 문자열(String)로 변환
    private String convertOptionsToJson(List<String> options) {
        if (options == null || options.isEmpty()) return "[]";
        try {
            return new ObjectMapper().writeValueAsString(options);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 오류", e);
            return "[]";
        }
    }

    // 매물 정보 수정
    public void update(Long vehicleId, User user, VehicleRegisterRequest request, List<MultipartFile> imageFiles) throws IOException {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물 없음"));

        if (!vehicle.getRegisteredBy().getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        String optionsJson = convertOptionsToJson(request.getOptions());
        vehicle.updateVehicleInfo(
                request.getModel(),
                request.getModelYear(),
                request.getMileage(),
                request.getPrice(),
                request.getDescription(),
                optionsJson
        );

        if (imageFiles != null && !imageFiles.isEmpty() && !imageFiles.get(0).isEmpty()) {
            List<VehicleImage> oldImages = vehicleImageRepository.findByVehicleVehicleIdOrderByDisplayOrderAsc(vehicleId);
            vehicleImageRepository.deleteAll(oldImages);

            saveImages(vehicle, imageFiles);

            vehicle.extendExpirationDate();

            if (vehicle.getVehicleStatus() == VehicleStatus.EXPIRED) {
                vehicle.changeStatus(VehicleStatus.PENDING);
            }
        }
    }

    // 매물 삭제
    public void delete(Long vehicleId, User user) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("매물 없음"));

        // 1. 권한 체크
        if (!vehicle.getRegisteredBy().getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        // 2. 거래 중인 매물은 삭제 불가
        if (vehicle.getVehicleStatus() == VehicleStatus.RESERVED || vehicle.getVehicleStatus() == VehicleStatus.SOLD) {
            throw new IllegalStateException("거래 중이거나 판매된 매물은 삭제할 수 없습니다.");
        }

        // 3. 이미지 파일 삭제
        //vehicleImageRepository.deleteByVehicle(vehicle);

        // 4. DB에서 완전 삭제
        vehicleRepository.delete(vehicle);
    }
}
