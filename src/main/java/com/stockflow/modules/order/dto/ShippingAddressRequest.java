package com.stockflow.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(

        @NotBlank(message = "Địa chỉ cụ thể không được để trống")
        @Size(max = 255)
        String addressLine,

        @NotBlank(message = "Phường/Xã không được để trống")
        @Size(max = 100)
        String ward,

        @NotBlank(message = "Quận/Huyện không được để trống")
        @Size(max = 100)
        String district,

        @NotBlank(message = "Tỉnh/Thành phố không được để trống")
        @Size(max = 100)
        String province

) {
}
