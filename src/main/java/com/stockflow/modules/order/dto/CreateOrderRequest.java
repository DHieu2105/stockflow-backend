package com.stockflow.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(

        @NotBlank(message = "Tên người nhận không được để trống")
        @Size(max = 255)
        String recipientName,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Size(min = 9, max = 15)
        @Pattern(
                regexp = "^[0-9+() .-]+$",
                message = "Số điện thoại không hợp lệ"
        )
        String recipientPhone,

        @NotNull(message = "Địa chỉ giao hàng không được để trống")
        @Valid
        ShippingAddressRequest shippingAddress,

        @NotEmpty(message = "Đơn hàng phải có ít nhất một sản phẩm")
        @Size(max = 50, message = "Đơn hàng tối đa 50 dòng sản phẩm")
        List<@Valid OrderItemRequest> items,

        @Size(max = 1000)
        String note

) {
}
