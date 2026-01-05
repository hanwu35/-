package com.example.aihealthcheck.service;

import com.example.aihealthcheck.dto.DoctorDTO;
import com.example.aihealthcheck.dto.DoctorQueryDTO;
import com.example.aihealthcheck.dto.PageResultDTO;
import com.example.aihealthcheck.entity.Department;
import com.example.aihealthcheck.entity.Doctor;
import com.example.aihealthcheck.repository.user.DepartmentRepository;
import com.example.aihealthcheck.repository.user.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public PageResultDTO<DoctorDTO> getDoctors(DoctorQueryDTO query) {
        // 查询所有匹配条件的医生（不分页），以便在分页前进行“有排班优先”排序
        Pageable unpaged = Pageable.unpaged();
        String deptCode = query.getDeptCode();
        String name = query.getDoctorName();
        Page<Doctor> doctorPageAll = doctorRepository.findByDeptCodeAndName(
                deptCode == null ? "all" : deptCode,
                name == null ? "" : name.trim(),
                unpaged
        );

        List<DoctorDTO> allDtos = doctorPageAll.getContent().stream()
                .map(d -> {
                    DoctorDTO dto = convertToDTO(d);
                    dto.setHasScheduleToday(hasScheduleToday(d.getDoctorId()));
                    return dto;
                })
                .sorted(createFinalComparator(query.getSortBy()))
                .collect(Collectors.toList());

        // 手动分页
        int page = query.getPage() != null ? query.getPage() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 6;
        int total = allDtos.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        List<DoctorDTO> pageData = fromIndex < toIndex ? allDtos.subList(fromIndex, toIndex) : List.of();

        return new PageResultDTO<>(
                pageData,
                page,
                pageSize,
                (long) total
        );
    }
    
    private Comparator<DoctorDTO> createFinalComparator(String sortBy) {
        Comparator<DoctorDTO> primary = Comparator.comparing((DoctorDTO dto) -> Boolean.TRUE.equals(dto.getHasScheduleToday()) ? 0 : 1);
        Comparator<DoctorDTO> secondary;
        if ("fee".equals(sortBy)) {
            secondary = Comparator.comparing(DoctorDTO::getNormalFee, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        } else if ("name".equals(sortBy)) {
            secondary = Comparator.comparing(DoctorDTO::getName, java.text.Collator.getInstance(java.util.Locale.CHINA));
        } else {
            secondary = Comparator.comparing(DoctorDTO::getDoctorId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        }
        return primary.thenComparing(secondary);
    }

    private Pageable createPageable(DoctorQueryDTO query) {
        Sort sort = createSort(query.getSortBy());
        // 注意：Spring Data JPA页码从0开始，前端从1开始
        return PageRequest.of(query.getPage() - 1, 6, sort);
    }

    private Sort createSort(String sortBy) {
        if (sortBy == null || "rating".equals(sortBy) || "appointments".equals(sortBy)) {
            // 评分和挂号量是随机生成的，无法直接排序，按医生ID排序
            return Sort.by(Sort.Direction.DESC, "doctorId");
        }

        switch (sortBy) {
            case "fee":
                return Sort.by(Sort.Direction.ASC, "normalFee");
            case "name":
                return Sort.by(Sort.Direction.ASC, "realName");
            default:
                return Sort.by(Sort.Direction.DESC, "doctorId");
        }
    }

    private DoctorDTO convertToDTO(Doctor doctor) {
        Department department = doctor.getDepartment();
        return new DoctorDTO(
                doctor.getDoctorId(),
                doctor.getDoctorCode(),
                doctor.getRealName(),
                department != null ? department.getDeptId() : null,
                department != null ? department.getDeptCode() : "未知科室",
                department != null ? department.getDeptName() : "未知科室",
                doctor.getLevel(),
                doctor.getNormalFee()
        );
    }

    public DoctorDTO getDoctorByCode(String doctorCode) {
        return doctorRepository.findByDoctorCode(doctorCode)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public DoctorDTO getDoctorById(Integer doctorId) {
        return doctorRepository.findById(doctorId)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    private boolean hasScheduleToday(Integer doctorId) {
        LocalDate today = LocalDate.now();
        String weekDay = getChineseWeekDay(today);
        Integer count = 0;
        try {
            count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM schedules WHERE doctor_id = ? AND work_day = ?", Integer.class, doctorId, weekDay);
        } catch (Exception ignored) {}
        return count != null && count > 0;
    }
    
    private String getChineseWeekDay(LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        switch (day) {
            case 1: return "周一";
            case 2: return "周二";
            case 3: return "周三";
            case 4: return "周四";
            case 5: return "周五";
            case 6: return "周六";
            case 7: return "周日";
            default: return "周一";
        }
    }
}
