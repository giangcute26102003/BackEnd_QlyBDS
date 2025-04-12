package com.example.qlyBDS.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
//    @JsonProperty("user_id")
    private Integer userId;

    private String name;

    private String email;

//    @JsonProperty("phone_number")
    private String phoneNumber;

    private String address;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @JsonProperty("role_id")
    private Integer roleId;

    @JsonProperty("is_active")
    private Boolean isActive;

    private String message;;

    public UserResponse(String message) {
        this.message = message;
    }



    // Static method để chuyển từ User Entity sang UserResponse
    public static UserResponse fromUser(com.example.qlyBDS.models.User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .dob(user.getDob())
                .roleId(user.getRole().getRoleId())
                .isActive(user.getIsActive())
                .build();
    }
}
